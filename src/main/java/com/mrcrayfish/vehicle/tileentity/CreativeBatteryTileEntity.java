package com.mrcrayfish.vehicle.tileentity;

import com.mrcrayfish.vehicle.init.ModTileEntities;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Endless energy: pushed every tick into the neighbours that take energy, and given to the cables
 * that pull from it.
 */
public class CreativeBatteryTileEntity extends BlockEntity
{
    private static final int PUSH_PER_TICK = 1_000_000;

    private final IEnergyStorage endless = new IEnergyStorage()
    {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return maxExtract; }
        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };
    private final LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> this.endless);

    public CreativeBatteryTileEntity(BlockPos pos, BlockState state)
    {
        super(ModTileEntities.CREATIVE_BATTERY.get(), pos, state);
    }

    public void tick()
    {
        if(this.level == null)
            return;
        for(Direction direction : Direction.values())
        {
            BlockEntity neighbour = this.level.getBlockEntity(this.worldPosition.relative(direction));
            if(neighbour != null && !(neighbour instanceof CreativeBatteryTileEntity))
            {
                neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(storage -> {
                    if(storage.canReceive())
                    {
                        storage.receiveEnergy(PUSH_PER_TICK, false);
                    }
                });
            }
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side)
    {
        return capability == ForgeCapabilities.ENERGY ? this.capability.cast() : super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        this.capability.invalidate();
    }
}
