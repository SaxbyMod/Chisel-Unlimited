package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2551 extends NamespacedSchema {
    public V2551(int p_17944_, Schema p_17945_) {
        super(p_17944_, p_17945_);
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        super.registerTypes(pSchema, pEntityTypes, pBlockEntityTypes);
        pSchema.registerType(
            false,
            References.WORLD_GEN_SETTINGS,
            () -> DSL.fields(
                    "dimensions",
                    DSL.compoundList(
                        DSL.constType(namespacedString()),
                        DSL.fields(
                            "generator",
                            DSL.taggedChoiceLazy(
                                "type",
                                DSL.string(),
                                ImmutableMap.of(
                                    "minecraft:debug",
                                    DSL::remainder,
                                    "minecraft:flat",
                                    () -> DSL.optionalFields(
                                            "settings",
                                            DSL.optionalFields(
                                                "biome",
                                                References.BIOME.in(pSchema),
                                                "layers",
                                                DSL.list(DSL.optionalFields("block", References.BLOCK_NAME.in(pSchema)))
                                            )
                                        ),
                                    "minecraft:noise",
                                    () -> DSL.optionalFields(
                                            "biome_source",
                                            DSL.taggedChoiceLazy(
                                                "type",
                                                DSL.string(),
                                                ImmutableMap.of(
                                                    "minecraft:fixed",
                                                    () -> DSL.fields("biome", References.BIOME.in(pSchema)),
                                                    "minecraft:multi_noise",
                                                    () -> DSL.list(DSL.fields("biome", References.BIOME.in(pSchema))),
                                                    "minecraft:checkerboard",
                                                    () -> DSL.fields("biomes", DSL.list(References.BIOME.in(pSchema))),
                                                    "minecraft:vanilla_layered",
                                                    DSL::remainder,
                                                    "minecraft:the_end",
                                                    DSL::remainder
                                                )
                                            ),
                                            "settings",
                                            DSL.or(
                                                DSL.constType(DSL.string()),
                                                DSL.optionalFields(
                                                    "default_block", References.BLOCK_NAME.in(pSchema), "default_fluid", References.BLOCK_NAME.in(pSchema)
                                                )
                                            )
                                        )
                                )
                            )
                        )
                    )
                )
        );
    }
}