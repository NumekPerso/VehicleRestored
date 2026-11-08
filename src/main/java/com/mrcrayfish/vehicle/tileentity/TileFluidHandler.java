package com.mrcrayfish.vehicle.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Replacement for Forge's 1.16 TileFluidHandler (removed in 1.17+). A block entity holding a single
 * fluid tank exposed through the fluid handler capability.
 */
public class TileFluidHandler extends BlockEntity
{
    protected FluidTank tank = new FluidTank(FluidType.BUCKET_VOLUME);
    private LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> this.tank);

    public TileFluidHandler(@Nonnull BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.tank.readFromNBT(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        this.tank.writeToNBT(tag);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing)
    {
        if(capability == ForgeCapabilities.FLUID_HANDLER)
            return this.holder.cast();
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        this.holder.invalidate();
    }

    @Override
    public void reviveCaps()
    {
        super.reviveCaps();
        this.holder = LazyOptional.of(() -> this.tank);
    }
}
