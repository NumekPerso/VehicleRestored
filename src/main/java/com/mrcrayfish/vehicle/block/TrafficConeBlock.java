package com.mrcrayfish.vehicle.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Author: MrCrayfish
 */
public class TrafficConeBlock extends ObjectBlock
{
    /* Follows the steps of the cone model of the 1.12.2 version */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 3, 14, 2, 13),
            Block.box(3, 0, 2, 13, 2, 14),
            Block.box(4, 1, 4, 12, 4, 12),
            Block.box(4.5, 4, 4.5, 11.5, 6, 11.5),
            Block.box(5, 6, 5, 11, 8, 11),
            Block.box(5.5, 8, 5.5, 10.5, 10, 10.5),
            Block.box(6, 10, 6, 10, 12, 10),
            Block.box(6.5, 12, 6.5, 9.5, 14, 9.5));

    public TrafficConeBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).sound(SoundType.GRAVEL).strength(0.5F));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }
}
