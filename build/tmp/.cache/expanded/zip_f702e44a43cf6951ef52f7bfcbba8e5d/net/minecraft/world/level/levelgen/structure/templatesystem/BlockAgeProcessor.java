package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

public class BlockAgeProcessor extends StructureProcessor {
    public static final MapCodec<BlockAgeProcessor> CODEC = Codec.FLOAT.fieldOf("mossiness").xmap(BlockAgeProcessor::new, p_74023_ -> p_74023_.mossiness);
    private static final float PROBABILITY_OF_REPLACING_FULL_BLOCK = 0.5F;
    private static final float PROBABILITY_OF_REPLACING_STAIRS = 0.5F;
    private static final float PROBABILITY_OF_REPLACING_OBSIDIAN = 0.15F;
    private static final BlockState[] NON_MOSSY_REPLACEMENTS = new BlockState[]{Blocks.STONE_SLAB.defaultBlockState(), Blocks.STONE_BRICK_SLAB.defaultBlockState()};
    private final float mossiness;

    public BlockAgeProcessor(float pMossiness) {
        this.mossiness = pMossiness;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
        LevelReader p_74016_,
        BlockPos p_74017_,
        BlockPos p_74018_,
        StructureTemplate.StructureBlockInfo p_74019_,
        StructureTemplate.StructureBlockInfo p_74020_,
        StructurePlaceSettings p_74021_
    ) {
        RandomSource randomsource = p_74021_.getRandom(p_74020_.pos());
        BlockState blockstate = p_74020_.state();
        BlockPos blockpos = p_74020_.pos();
        BlockState blockstate1 = null;
        if (blockstate.is(Blocks.STONE_BRICKS) || blockstate.is(Blocks.STONE) || blockstate.is(Blocks.CHISELED_STONE_BRICKS)) {
            blockstate1 = this.maybeReplaceFullStoneBlock(randomsource);
        } else if (blockstate.is(BlockTags.STAIRS)) {
            blockstate1 = this.maybeReplaceStairs(randomsource, p_74020_.state());
        } else if (blockstate.is(BlockTags.SLABS)) {
            blockstate1 = this.maybeReplaceSlab(randomsource);
        } else if (blockstate.is(BlockTags.WALLS)) {
            blockstate1 = this.maybeReplaceWall(randomsource);
        } else if (blockstate.is(Blocks.OBSIDIAN)) {
            blockstate1 = this.maybeReplaceObsidian(randomsource);
        }

        return blockstate1 != null ? new StructureTemplate.StructureBlockInfo(blockpos, blockstate1, p_74020_.nbt()) : p_74020_;
    }

    @Nullable
    private BlockState maybeReplaceFullStoneBlock(RandomSource pRandom) {
        if (pRandom.nextFloat() >= 0.5F) {
            return null;
        } else {
            BlockState[] ablockstate = new BlockState[]{Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(pRandom, Blocks.STONE_BRICK_STAIRS)};
            BlockState[] ablockstate1 = new BlockState[]{Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(pRandom, Blocks.MOSSY_STONE_BRICK_STAIRS)};
            return this.getRandomBlock(pRandom, ablockstate, ablockstate1);
        }
    }

    @Nullable
    private BlockState maybeReplaceStairs(RandomSource pRandom, BlockState pState) {
        Direction direction = pState.getValue(StairBlock.FACING);
        Half half = pState.getValue(StairBlock.HALF);
        if (pRandom.nextFloat() >= 0.5F) {
            return null;
        } else {
            BlockState[] ablockstate = new BlockState[]{
                Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, direction).setValue(StairBlock.HALF, half), Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState()
            };
            return this.getRandomBlock(pRandom, NON_MOSSY_REPLACEMENTS, ablockstate);
        }
    }

    @Nullable
    private BlockState maybeReplaceSlab(RandomSource pRandom) {
        return pRandom.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState() : null;
    }

    @Nullable
    private BlockState maybeReplaceWall(RandomSource pRandom) {
        return pRandom.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState() : null;
    }

    @Nullable
    private BlockState maybeReplaceObsidian(RandomSource pRandom) {
        return pRandom.nextFloat() < 0.15F ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : null;
    }

    private static BlockState getRandomFacingStairs(RandomSource pRandom, Block pStairsBlock) {
        return pStairsBlock.defaultBlockState()
            .setValue(StairBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(pRandom))
            .setValue(StairBlock.HALF, Util.getRandom(Half.values(), pRandom));
    }

    private BlockState getRandomBlock(RandomSource pRandom, BlockState[] pNormalStates, BlockState[] pMossyStates) {
        return pRandom.nextFloat() < this.mossiness ? getRandomBlock(pRandom, pMossyStates) : getRandomBlock(pRandom, pNormalStates);
    }

    private static BlockState getRandomBlock(RandomSource pRandom, BlockState[] pStates) {
        return pStates[pRandom.nextInt(pStates.length)];
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.BLOCK_AGE;
    }
}