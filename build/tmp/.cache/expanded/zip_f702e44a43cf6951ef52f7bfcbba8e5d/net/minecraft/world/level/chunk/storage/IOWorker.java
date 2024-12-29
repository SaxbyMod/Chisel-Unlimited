package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class IOWorker implements ChunkScanAccess, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    private final PriorityConsecutiveExecutor consecutiveExecutor;
    private final RegionFileStorage storage;
    private final SequencedMap<ChunkPos, IOWorker.PendingStore> pendingWrites = new LinkedHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<CompletableFuture<BitSet>> regionCacheForBlender = new Long2ObjectLinkedOpenHashMap<>();
    private static final int REGION_CACHE_SIZE = 1024;

    protected IOWorker(RegionStorageInfo pInfo, Path pFolder, boolean pSync) {
        this.storage = new RegionFileStorage(pInfo, pFolder, pSync);
        this.consecutiveExecutor = new PriorityConsecutiveExecutor(IOWorker.Priority.values().length, Util.ioPool(), "IOWorker-" + pInfo.type());
    }

    public boolean isOldChunkAround(ChunkPos pChunkPos, int pRadius) {
        ChunkPos chunkpos = new ChunkPos(pChunkPos.x - pRadius, pChunkPos.z - pRadius);
        ChunkPos chunkpos1 = new ChunkPos(pChunkPos.x + pRadius, pChunkPos.z + pRadius);

        for (int i = chunkpos.getRegionX(); i <= chunkpos1.getRegionX(); i++) {
            for (int j = chunkpos.getRegionZ(); j <= chunkpos1.getRegionZ(); j++) {
                BitSet bitset = this.getOrCreateOldDataForRegion(i, j).join();
                if (!bitset.isEmpty()) {
                    ChunkPos chunkpos2 = ChunkPos.minFromRegion(i, j);
                    int k = Math.max(chunkpos.x - chunkpos2.x, 0);
                    int l = Math.max(chunkpos.z - chunkpos2.z, 0);
                    int i1 = Math.min(chunkpos1.x - chunkpos2.x, 31);
                    int j1 = Math.min(chunkpos1.z - chunkpos2.z, 31);

                    for (int k1 = k; k1 <= i1; k1++) {
                        for (int l1 = l; l1 <= j1; l1++) {
                            int i2 = l1 * 32 + k1;
                            if (bitset.get(i2)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private CompletableFuture<BitSet> getOrCreateOldDataForRegion(int pChunkX, int pChunkZ) {
        long i = ChunkPos.asLong(pChunkX, pChunkZ);
        synchronized (this.regionCacheForBlender) {
            CompletableFuture<BitSet> completablefuture = this.regionCacheForBlender.getAndMoveToFirst(i);
            if (completablefuture == null) {
                completablefuture = this.createOldDataForRegion(pChunkX, pChunkZ);
                this.regionCacheForBlender.putAndMoveToFirst(i, completablefuture);
                if (this.regionCacheForBlender.size() > 1024) {
                    this.regionCacheForBlender.removeLast();
                }
            }

            return completablefuture;
        }
    }

    private CompletableFuture<BitSet> createOldDataForRegion(int pChunkX, int pChunkZ) {
        return CompletableFuture.supplyAsync(
            () -> {
                ChunkPos chunkpos = ChunkPos.minFromRegion(pChunkX, pChunkZ);
                ChunkPos chunkpos1 = ChunkPos.maxFromRegion(pChunkX, pChunkZ);
                BitSet bitset = new BitSet();
                ChunkPos.rangeClosed(chunkpos, chunkpos1)
                    .forEach(
                        p_223480_ -> {
                            CollectFields collectfields = new CollectFields(
                                new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data")
                            );

                            try {
                                this.scanChunk(p_223480_, collectfields).join();
                            } catch (Exception exception) {
                                LOGGER.warn("Failed to scan chunk {}", p_223480_, exception);
                                return;
                            }

                            if (collectfields.getResult() instanceof CompoundTag compoundtag && this.isOldChunk(compoundtag)) {
                                int i = p_223480_.getRegionLocalZ() * 32 + p_223480_.getRegionLocalX();
                                bitset.set(i);
                            }
                        }
                    );
                return bitset;
            },
            Util.backgroundExecutor()
        );
    }

    private boolean isOldChunk(CompoundTag pChunkData) {
        return pChunkData.contains("DataVersion", 99) && pChunkData.getInt("DataVersion") >= 4185 ? pChunkData.contains("blending_data", 10) : true;
    }

    public CompletableFuture<Void> store(ChunkPos pChunkPos, @Nullable CompoundTag pChunkData) {
        return this.store(pChunkPos, () -> pChunkData);
    }

    public CompletableFuture<Void> store(ChunkPos pChunkPos, Supplier<CompoundTag> pDataSupplier) {
        return this.<CompletableFuture<Void>>submitTask(() -> {
            CompoundTag compoundtag = pDataSupplier.get();
            IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.computeIfAbsent(pChunkPos, p_223488_ -> new IOWorker.PendingStore(compoundtag));
            ioworker$pendingstore.data = compoundtag;
            return ioworker$pendingstore.result;
        }).thenCompose(Function.identity());
    }

    public CompletableFuture<Optional<CompoundTag>> loadAsync(ChunkPos pChunkPos) {
        return this.submitThrowingTask(() -> {
            IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(pChunkPos);
            if (ioworker$pendingstore != null) {
                return Optional.ofNullable(ioworker$pendingstore.copyData());
            } else {
                try {
                    CompoundTag compoundtag = this.storage.read(pChunkPos);
                    return Optional.ofNullable(compoundtag);
                } catch (Exception exception) {
                    LOGGER.warn("Failed to read chunk {}", pChunkPos, exception);
                    throw exception;
                }
            }
        });
    }

    public CompletableFuture<Void> synchronize(boolean pFlushStorage) {
        CompletableFuture<Void> completablefuture = this.<CompletableFuture<Void>>submitTask(
                () -> CompletableFuture.allOf(this.pendingWrites.values().stream().map(p_223475_ -> p_223475_.result).toArray(CompletableFuture[]::new))
            )
            .thenCompose(Function.identity());
        return pFlushStorage ? completablefuture.thenCompose(p_360563_ -> this.submitThrowingTask(() -> {
                try {
                    this.storage.flush();
                    return null;
                } catch (Exception exception) {
                    LOGGER.warn("Failed to synchronize chunks", (Throwable)exception);
                    throw exception;
                }
            })) : completablefuture.thenCompose(p_223477_ -> this.submitTask(() -> null));
    }

    @Override
    public CompletableFuture<Void> scanChunk(ChunkPos p_196939_, StreamTagVisitor p_196940_) {
        return this.submitThrowingTask(() -> {
            try {
                IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(p_196939_);
                if (ioworker$pendingstore != null) {
                    if (ioworker$pendingstore.data != null) {
                        ioworker$pendingstore.data.acceptAsRoot(p_196940_);
                    }
                } else {
                    this.storage.scanChunk(p_196939_, p_196940_);
                }

                return null;
            } catch (Exception exception) {
                LOGGER.warn("Failed to bulk scan chunk {}", p_196939_, exception);
                throw exception;
            }
        });
    }

    private <T> CompletableFuture<T> submitThrowingTask(IOWorker.ThrowingSupplier<T> pTask) {
        return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), p_360568_ -> {
            if (!this.shutdownRequested.get()) {
                try {
                    p_360568_.complete(pTask.get());
                } catch (Exception exception) {
                    p_360568_.completeExceptionally(exception);
                }
            }

            this.tellStorePending();
        });
    }

    private <T> CompletableFuture<T> submitTask(Supplier<T> pTask) {
        return this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.FOREGROUND.ordinal(), p_360561_ -> {
            if (!this.shutdownRequested.get()) {
                p_360561_.complete(pTask.get());
            }

            this.tellStorePending();
        });
    }

    private void storePendingChunk() {
        Entry<ChunkPos, IOWorker.PendingStore> entry = this.pendingWrites.pollFirstEntry();
        if (entry != null) {
            this.runStore(entry.getKey(), entry.getValue());
            this.tellStorePending();
        }
    }

    private void tellStorePending() {
        this.consecutiveExecutor.schedule(new StrictQueue.RunnableWithPriority(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
    }

    private void runStore(ChunkPos pChunkPos, IOWorker.PendingStore pPendingStore) {
        try {
            this.storage.write(pChunkPos, pPendingStore.data);
            pPendingStore.result.complete(null);
        } catch (Exception exception) {
            LOGGER.error("Failed to store chunk {}", pChunkPos, exception);
            pPendingStore.result.completeExceptionally(exception);
        }
    }

    @Override
    public void close() throws IOException {
        if (this.shutdownRequested.compareAndSet(false, true)) {
            this.waitForShutdown();
            this.consecutiveExecutor.close();

            try {
                this.storage.close();
            } catch (Exception exception) {
                LOGGER.error("Failed to close storage", (Throwable)exception);
            }
        }
    }

    private void waitForShutdown() {
        this.consecutiveExecutor.scheduleWithResult(IOWorker.Priority.SHUTDOWN.ordinal(), p_360562_ -> p_360562_.complete(Unit.INSTANCE)).join();
    }

    public RegionStorageInfo storageInfo() {
        return this.storage.info();
    }

    static class PendingStore {
        @Nullable
        CompoundTag data;
        final CompletableFuture<Void> result = new CompletableFuture<>();

        public PendingStore(@Nullable CompoundTag pData) {
            this.data = pData;
        }

        @Nullable
        CompoundTag copyData() {
            CompoundTag compoundtag = this.data;
            return compoundtag == null ? null : compoundtag.copy();
        }
    }

    static enum Priority {
        FOREGROUND,
        BACKGROUND,
        SHUTDOWN;
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        @Nullable
        T get() throws Exception;
    }
}