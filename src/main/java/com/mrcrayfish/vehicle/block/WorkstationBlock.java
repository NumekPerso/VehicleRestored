package com.mrcrayfish.vehicle.block;

import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.mrcrayfish.vehicle.init.ModTileEntities;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import com.mrcrayfish.vehicle.tileentity.WorkstationTileEntity;
import com.mrcrayfish.vehicle.util.VoxelShapeHelper;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import java.util.Map;
import net.minecraftforge.network.NetworkHooks;

/**
 * Author: MrCrayfish
 */
public class WorkstationBlock extends RotatedObjectBlock implements EntityBlock
{
    /* Body and legs of the workstation model of the 1.12.2 version */
    private static final VoxelShape BODY = Shapes.or(
            Block.box(0, 1, 0, 16, 16, 16),
            Block.box(0.5, 0, 0.5, 2.5, 1, 2.5),
            Block.box(0.5, 0, 13.5, 2.5, 1, 15.5),
            Block.box(13.5, 0, 13.5, 15.5, 1, 15.5),
            Block.box(13.5, 0, 0.5, 15.5, 1, 2.5));

    /* The back board sticks out above the top, on the side the block faces */
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Shapes.or(BODY, Block.box(0, 16, 0, 16, 17.5, 2)),
            Direction.EAST, Shapes.or(BODY, Block.box(14, 16, 0, 16, 17.5, 16)),
            Direction.SOUTH, Shapes.or(BODY, Block.box(0, 16, 14, 16, 17.5, 16)),
            Direction.WEST, Shapes.or(BODY, Block.box(0, 16, 0, 2, 17.5, 16)));

    public WorkstationBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.0F));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context)
    {
        return SHAPES.getOrDefault(state.getValue(DIRECTION), BODY);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player playerEntity, InteractionHand hand, BlockHitResult result)
    {
        if(!world.isClientSide)
        {
            BlockEntity tileEntity = world.getBlockEntity(pos);
            if(tileEntity instanceof MenuProvider)
            {
                NetworkHooks.openScreen((ServerPlayer) playerEntity, (MenuProvider) tileEntity, pos);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The dye, engine and wheel left in the workstation were lost when it was broken. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if(!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WorkstationTileEntity workstation)
        {
            Containers.dropContents(level, pos, workstation);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new WorkstationTileEntity(pos, state);
    }
}
