package com.mrcrayfish.vehicle.block;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.tileentity.IndustrialFuelDrumTileEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Author: MrCrayfish
 */
public class IndustrialFuelDrumBlock extends FuelDrumBlock
{
    @Override
    public int getCapacity()
    {
        return Config.SERVER.industrialFuelDrumCapacity.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new IndustrialFuelDrumTileEntity(pos, state);
    }
}
