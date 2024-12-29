package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record Tool(List<Tool.Rule> rules, float defaultMiningSpeed, int damagePerBlock) {
    public static final Codec<Tool> CODEC = RecordCodecBuilder.create(
        p_335351_ -> p_335351_.group(
                    Tool.Rule.CODEC.listOf().fieldOf("rules").forGetter(Tool::rules),
                    Codec.FLOAT.optionalFieldOf("default_mining_speed", Float.valueOf(1.0F)).forGetter(Tool::defaultMiningSpeed),
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("damage_per_block", 1).forGetter(Tool::damagePerBlock)
                )
                .apply(p_335351_, Tool::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Tool> STREAM_CODEC = StreamCodec.composite(
        Tool.Rule.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Tool::rules,
        ByteBufCodecs.FLOAT,
        Tool::defaultMiningSpeed,
        ByteBufCodecs.VAR_INT,
        Tool::damagePerBlock,
        Tool::new
    );

    public float getMiningSpeed(BlockState pState) {
        for (Tool.Rule tool$rule : this.rules) {
            if (tool$rule.speed.isPresent() && pState.is(tool$rule.blocks)) {
                return tool$rule.speed.get();
            }
        }

        return this.defaultMiningSpeed;
    }

    public boolean isCorrectForDrops(BlockState pState) {
        for (Tool.Rule tool$rule : this.rules) {
            if (tool$rule.correctForDrops.isPresent() && pState.is(tool$rule.blocks)) {
                return tool$rule.correctForDrops.get();
            }
        }

        return false;
    }

    public static record Rule(HolderSet<Block> blocks, Optional<Float> speed, Optional<Boolean> correctForDrops) {
        public static final Codec<Tool.Rule> CODEC = RecordCodecBuilder.create(
            p_329479_ -> p_329479_.group(
                        RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(Tool.Rule::blocks),
                        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("speed").forGetter(Tool.Rule::speed),
                        Codec.BOOL.optionalFieldOf("correct_for_drops").forGetter(Tool.Rule::correctForDrops)
                    )
                    .apply(p_329479_, Tool.Rule::new)
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, Tool.Rule> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.BLOCK),
            Tool.Rule::blocks,
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs::optional),
            Tool.Rule::speed,
            ByteBufCodecs.BOOL.apply(ByteBufCodecs::optional),
            Tool.Rule::correctForDrops,
            Tool.Rule::new
        );

        public static Tool.Rule minesAndDrops(HolderSet<Block> pBlocks, float pSpeed) {
            return new Tool.Rule(pBlocks, Optional.of(pSpeed), Optional.of(true));
        }

        public static Tool.Rule deniesDrops(HolderSet<Block> pBlocks) {
            return new Tool.Rule(pBlocks, Optional.empty(), Optional.of(false));
        }

        public static Tool.Rule overrideSpeed(HolderSet<Block> pBlocks, float pSpeed) {
            return new Tool.Rule(pBlocks, Optional.of(pSpeed), Optional.empty());
        }
    }
}