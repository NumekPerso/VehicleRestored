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
import com.mrcrayfish.vehicle.tileentity.FluidMixerTileEntity;
import com.mrcrayfish.vehicle.util.TileEntityUtil;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.network.NetworkHooks;

/**
 * Author: MrCrayfish
 */
public class FluidMixerBlock extends RotatedObjectBlock implements EntityBlock
{
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public FluidMixerBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.0F));
        this.registerDefaultState(this.getStateDefinition().any().setValue(DIRECTION, Direction.NORTH).setValue(ENABLED, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player playerEntity, InteractionHand hand, BlockHitResult result)
    {
        if(!world.isClientSide)
        {
            if(!FluidUtil.interactWithFluidHandler(playerEntity, hand, world, pos, result.getDirection()))
            {
                BlockEntity tileEntity = world.getBlockEntity(pos);
                if(tileEntity instanceof MenuProvider)
                {
                    TileEntityUtil.sendUpdatePacket(tileEntity, (ServerPlayer) playerEntity);
                    NetworkHooks.openScreen((ServerPlayer) playerEntity, (MenuProvider) tileEntity, pos);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if(state.getBlock() != newState.getBlock())
        {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if(tileentity instanceof Container)
            {
                Containers.dropContents(worldIn, pos, (Container) tileentity);
                worldIn.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FluidMixerTileEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide() ? null : createTicker(type, ModTileEntities.FLUID_MIXER.get(), (level1, pos, state1, blockEntity) -> blockEntity.tick());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(ENABLED);
    }
}