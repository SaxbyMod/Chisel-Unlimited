package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RegistryDataCollector {
    @Nullable
    private RegistryDataCollector.ContentsCollector contentsCollector;
    @Nullable
    private RegistryDataCollector.TagCollector tagCollector;

    public void appendContents(ResourceKey<? extends Registry<?>> pRegistryKey, List<RegistrySynchronization.PackedRegistryEntry> pRegistryEntries) {
        if (this.contentsCollector == null) {
            this.contentsCollector = new RegistryDataCollector.ContentsCollector();
        }

        this.contentsCollector.append(pRegistryKey, pRegistryEntries);
    }

    public void appendTags(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> pTags) {
        if (this.tagCollector == null) {
            this.tagCollector = new RegistryDataCollector.TagCollector();
        }

        pTags.forEach(this.tagCollector::append);
    }

    private static <T> Registry.PendingTags<T> resolveRegistryTags(
        RegistryAccess.Frozen pRegistryAccess, ResourceKey<? extends Registry<? extends T>> pRegistryKey, TagNetworkSerialization.NetworkPayload pPayload
    ) {
        Registry<T> registry = pRegistryAccess.lookupOrThrow(pRegistryKey);
        return registry.prepareTagReload(pPayload.resolve(registry));
    }

    private RegistryAccess loadNewElementsAndTags(ResourceProvider pResourceProvider, RegistryDataCollector.ContentsCollector pContentCollector, boolean pIsMemoryConnection) {
        LayeredRegistryAccess<ClientRegistryLayer> layeredregistryaccess = ClientRegistryLayer.createRegistryAccess();
        RegistryAccess.Frozen registryaccess$frozen = layeredregistryaccess.getAccessForLoading(ClientRegistryLayer.REMOTE);
        Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> map = new HashMap<>();
        pContentCollector.elements
            .forEach(
                (p_365837_, p_368362_) -> map.put(
                        (ResourceKey<? extends Registry<?>>)p_365837_,
                        new RegistryDataLoader.NetworkedRegistryData(
                            (List<RegistrySynchronization.PackedRegistryEntry>)p_368362_, TagNetworkSerialization.NetworkPayload.EMPTY
                        )
                    )
            );
        List<Registry.PendingTags<?>> list = new ArrayList<>();
        if (this.tagCollector != null) {
            this.tagCollector.forEach((p_369903_, p_361286_) -> {
                if (!p_361286_.isEmpty()) {
                    if (RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_369903_)) {
                        map.compute((ResourceKey<? extends Registry<?>>)p_369903_, (p_364673_, p_362709_) -> {
                            List<RegistrySynchronization.PackedRegistryEntry> list2 = p_362709_ != null ? p_362709_.elements() : List.of();
                            return new RegistryDataLoader.NetworkedRegistryData(list2, p_361286_);
                        });
                    } else if (!pIsMemoryConnection) {
                        list.add(resolveRegistryTags(registryaccess$frozen, (ResourceKey<? extends Registry<?>>)p_369903_, p_361286_));
                    }
                }
            });
        }

        List<HolderLookup.RegistryLookup<?>> list1 = TagLoader.buildUpdatedLookups(registryaccess$frozen, list);

        RegistryAccess.Frozen registryaccess$frozen1;
        try {
            registryaccess$frozen1 = RegistryDataLoader.load(map, pResourceProvider, list1, RegistryDataLoader.SYNCHRONIZED_REGISTRIES).freeze();
        } catch (Exception exception) {
            CrashReport crashreport = CrashReport.forThrowable(exception, "Network Registry Load");
            addCrashDetails(crashreport, map, list);
            throw new ReportedException(crashreport);
        }

        RegistryAccess registryaccess = layeredregistryaccess.replaceFrom(ClientRegistryLayer.REMOTE, registryaccess$frozen1).compositeAccess();
        list.forEach(Registry.PendingTags::apply);
        return registryaccess;
    }

    private static void addCrashDetails(
        CrashReport pCrashReport,
        Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> pDynamicRegistries,
        List<Registry.PendingTags<?>> pStaticRegistries
    ) {
        CrashReportCategory crashreportcategory = pCrashReport.addCategory("Received Elements and Tags");
        crashreportcategory.setDetail(
            "Dynamic Registries",
            () -> pDynamicRegistries.entrySet()
                    .stream()
                    .sorted(Comparator.comparing(p_369666_ -> p_369666_.getKey().location()))
                    .map(
                        p_369114_ -> String.format(
                                Locale.ROOT,
                                "\n\t\t%s: elements=%d tags=%d",
                                p_369114_.getKey().location(),
                                p_369114_.getValue().elements().size(),
                                p_369114_.getValue().tags().size()
                            )
                    )
                    .collect(Collectors.joining())
        );
        crashreportcategory.setDetail(
            "Static Registries",
            () -> pStaticRegistries.stream()
                    .sorted(Comparator.comparing(p_365860_ -> p_365860_.key().location()))
                    .map(p_368235_ -> String.format(Locale.ROOT, "\n\t\t%s: tags=%d", p_368235_.key().location(), p_368235_.size()))
                    .collect(Collectors.joining())
        );
    }

    private void loadOnlyTags(RegistryDataCollector.TagCollector pTagCollector, RegistryAccess.Frozen pRegistryAccess, boolean pIsMemoryConnection) {
        pTagCollector.forEach((p_370187_, p_363143_) -> {
            if (pIsMemoryConnection || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_370187_)) {
                resolveRegistryTags(pRegistryAccess, (ResourceKey<? extends Registry<?>>)p_370187_, p_363143_).apply();
            }
        });
    }

    public RegistryAccess.Frozen collectGameRegistries(ResourceProvider pResourceProvider, RegistryAccess.Frozen pRegistryAccess, boolean pIsMemoryConnection) {
        RegistryAccess registryaccess;
        if (this.contentsCollector != null) {
            registryaccess = this.loadNewElementsAndTags(pResourceProvider, this.contentsCollector, pIsMemoryConnection);
        } else {
            if (this.tagCollector != null) {
                this.loadOnlyTags(this.tagCollector, pRegistryAccess, !pIsMemoryConnection);
            }

            registryaccess = pRegistryAccess;
        }

        return registryaccess.freeze();
    }

    @OnlyIn(Dist.CLIENT)
    static class ContentsCollector {
        final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> elements = new HashMap<>();

        public void append(ResourceKey<? extends Registry<?>> pRegistryKey, List<RegistrySynchronization.PackedRegistryEntry> pEntries) {
            this.elements.computeIfAbsent(pRegistryKey, p_332834_ -> new ArrayList<>()).addAll(pEntries);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class TagCollector {
        private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap<>();

        public void append(ResourceKey<? extends Registry<?>> pRegistryKey, TagNetworkSerialization.NetworkPayload pPayload) {
            this.tags.put(pRegistryKey, pPayload);
        }

        public void forEach(BiConsumer<? super ResourceKey<? extends Registry<?>>, ? super TagNetworkSerialization.NetworkPayload> pAction) {
            this.tags.forEach(pAction);
        }
    }
}