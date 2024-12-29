package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackLocationInfo location;
    private final BuiltInMetadata metadata;
    private final Set<String> namespaces;
    private final List<Path> rootPaths;
    private final Map<PackType, List<Path>> pathsForType;

    VanillaPackResources(
        PackLocationInfo pLocation, BuiltInMetadata pMetadata, Set<String> pNamespaces, List<Path> pRootPaths, Map<PackType, List<Path>> pPathsForType
    ) {
        this.location = pLocation;
        this.metadata = pMetadata;
        this.namespaces = pNamespaces;
        this.rootPaths = pRootPaths;
        this.pathsForType = pPathsForType;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... p_250530_) {
        FileUtil.validatePath(p_250530_);
        List<String> list = List.of(p_250530_);

        for (Path path : this.rootPaths) {
            Path path1 = FileUtil.resolvePath(path, list);
            if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
                return IoSupplier.create(path1);
            }
        }

        return null;
    }

    public void listRawPaths(PackType pPackType, ResourceLocation pPackLocation, Consumer<Path> pOutput) {
        FileUtil.decomposePath(pPackLocation.getPath()).ifSuccess(p_248238_ -> {
            String s = pPackLocation.getNamespace();

            for (Path path : this.pathsForType.get(pPackType)) {
                Path path1 = path.resolve(s);
                pOutput.accept(FileUtil.resolvePath(path1, (List<String>)p_248238_));
            }
        }).ifError(p_326467_ -> LOGGER.error("Invalid path {}: {}", pPackLocation, p_326467_.message()));
    }

    @Override
    public void listResources(PackType p_248974_, String p_248703_, String p_250848_, PackResources.ResourceOutput p_249668_) {
        FileUtil.decomposePath(p_250848_).ifSuccess(p_248228_ -> {
            List<Path> list = this.pathsForType.get(p_248974_);
            int i = list.size();
            if (i == 1) {
                getResources(p_249668_, p_248703_, list.get(0), (List<String>)p_248228_);
            } else if (i > 1) {
                Map<ResourceLocation, IoSupplier<InputStream>> map = new HashMap<>();

                for (int j = 0; j < i - 1; j++) {
                    getResources(map::putIfAbsent, p_248703_, list.get(j), (List<String>)p_248228_);
                }

                Path path = list.get(i - 1);
                if (map.isEmpty()) {
                    getResources(p_249668_, p_248703_, path, (List<String>)p_248228_);
                } else {
                    getResources(map::putIfAbsent, p_248703_, path, (List<String>)p_248228_);
                    map.forEach(p_249668_);
                }
            }
        }).ifError(p_326469_ -> LOGGER.error("Invalid path {}: {}", p_250848_, p_326469_.message()));
    }

    private static void getResources(PackResources.ResourceOutput pResourceOutput, String pNamespace, Path pRoot, List<String> pPaths) {
        Path path = pRoot.resolve(pNamespace);
        PathPackResources.listPath(pNamespace, path, pPaths, pResourceOutput);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType p_250512_, ResourceLocation p_251554_) {
        return FileUtil.decomposePath(p_251554_.getPath()).mapOrElse(p_248224_ -> {
            String s = p_251554_.getNamespace();

            for (Path path : this.pathsForType.get(p_250512_)) {
                Path path1 = FileUtil.resolvePath(path.resolve(s), (List<String>)p_248224_);
                if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
                    return IoSupplier.create(path1);
                }
            }

            return null;
        }, p_326471_ -> {
            LOGGER.error("Invalid path {}: {}", p_251554_, p_326471_.message());
            return null;
        });
    }

    @Override
    public Set<String> getNamespaces(PackType pType) {
        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> p_378082_) {
        IoSupplier<InputStream> iosupplier = this.getRootResource("pack.mcmeta");
        if (iosupplier != null) {
            try (InputStream inputstream = iosupplier.get()) {
                T t = AbstractPackResources.getMetadataFromStream(p_378082_, inputstream);
                if (t != null) {
                    return t;
                }

                return this.metadata.get(p_378082_);
            } catch (IOException ioexception) {
            }
        }

        return this.metadata.get(p_378082_);
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public void close() {
    }

    public ResourceProvider asProvider() {
        return p_248239_ -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, p_248239_))
                .map(p_248221_ -> new Resource(this, (IoSupplier<InputStream>)p_248221_));
    }
}