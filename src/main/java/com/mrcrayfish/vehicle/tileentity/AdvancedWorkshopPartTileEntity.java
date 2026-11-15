package com.mrcrayfish.vehicle.tileentity;

import com.mrcrayfish.vehicle.block.AdvancedWorkshopBlock;
import com.mrcrayfish.vehicle.init.ModTileEntities;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * A cell of the advanced workshop other than the main one: gives the capabilities of the machine
 * (energy, items), so a cable or a hopper works on any face of the machine.
 */
public class AdvancedWorkshopPartTileEntity extends BlockEntity
{
    public AdvancedWorkshopPartTileEntity(BlockPos pos, BlockState state)
    {
        super(ModTileEntities.ADVANCED_WORKSHOP_PART.get(), pos, state);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side)
    {
        if(this.level != null && this.getBlockState().getBlock() instanceof AdvancedWorkshopBlock)
        {
            BlockPos main = AdvancedWorkshopBlock.getMainPos(this.worldPosition, this.getBlockState());
            if(this.level.isLoaded(main) && this.level.getBlockEntity(main) instanceof AdvancedWorkshopTileEntity workshop)
            {
                return workshop.getCapability(capability, side);
            }
        }
        return super.getCapability(capability, side);
    }
}
