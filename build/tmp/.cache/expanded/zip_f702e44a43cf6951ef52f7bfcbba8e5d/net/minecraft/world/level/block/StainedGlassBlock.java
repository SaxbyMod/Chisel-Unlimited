package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StainedGlassBlock extends TransparentBlock implements BeaconBeamBlock {
    public static final MapCodec<StainedGlassBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360452_ -> p_360452_.group(DyeColor.CODEC.fieldOf("color").forGetter(StainedGlassBlock::getColor), propertiesCodec())
                .apply(p_360452_, StainedGlassBlock::new)
    );
    private final DyeColor color;

    @Override
    public MapCodec<StainedGlassBlock> codec() {
        return CODEC;
    }

    public StainedGlassBlock(DyeColor pColor, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.color = pColor;
    }

    @Override
    public DyeColor getColor() {
        return this.color;
    }
}