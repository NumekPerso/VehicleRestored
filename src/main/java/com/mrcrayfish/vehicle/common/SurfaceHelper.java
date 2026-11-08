package com.mrcrayfish.vehicle.common;

import com.mrcrayfish.vehicle.entity.IWheelType;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.entity.Wheel;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Categories blocks into a surface type to determine the effect on the speed of wheels.
 * The original grouped blocks by Material, which no longer exists since 1.20, so the blocks are
 * now grouped with block tags and sound types into the same categories.
 * Author: MrCrayfish
 */
public class SurfaceHelper
{
    public static SurfaceType getSurfaceType(BlockState state)
    {
        if(state.isAir() || !state.getFluidState().isEmpty() && !state.isSolid())
            return SurfaceType.NONE;

        // Materials that were snow-like: snow, leaves, cactus, cake, coral, TNT
        if(state.is(BlockTags.SNOW) || state.is(Blocks.POWDER_SNOW) || state.is(BlockTags.LEAVES) || state.is(Blocks.CACTUS)
                || state.is(BlockTags.CANDLE_CAKES) || state.is(Blocks.CAKE) || state.is(BlockTags.CORALS) || state.is(BlockTags.CORAL_BLOCKS)
                || state.is(BlockTags.WALL_CORALS) || state.is(Blocks.TNT) || state.getSoundType() == SoundType.SNOW || state.getSoundType() == SoundType.POWDER_SNOW)
            return SurfaceType.SNOW;

        // Materials that were dirt-like: dirt, grass, sand, clay, wool, carpets, sponge, plants
        if(state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SUSPICIOUS_GRAVEL) || state.is(Blocks.CLAY)
                || state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS) || state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE)
                || state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)
                || state.getBlock() instanceof BushBlock)
            return SurfaceType.DIRT;

        if(state.isSolid())
            return SurfaceType.SOLID;

        return SurfaceType.NONE;
    }

    public static float getSurfaceModifier(PoweredVehicleEntity vehicle)
    {
        VehicleProperties properties = vehicle.getProperties();
        List<Wheel> wheels = properties.getWheels();
        if(!vehicle.hasWheelStack() || wheels.isEmpty())
            return 1.0F;

        Optional<IWheelType> optional = vehicle.getWheelType();
        if(!optional.isPresent())
            return 1.0F;

        int wheelCount = 0;
        float surfaceModifier = 0F;
        for(int i = 0; i < wheels.size(); i++)
        {
            double wheelX = vehicle.getWheelPositions()[i * 3];
            double wheelY = vehicle.getWheelPositions()[i * 3 + 1];
            double wheelZ = vehicle.getWheelPositions()[i * 3 + 2];
            int x = Mth.floor(vehicle.getX() + wheelX);
            int y = Mth.floor(vehicle.getY() + wheelY - 0.2D);
            int z = Mth.floor(vehicle.getZ() + wheelZ);
            BlockState state = vehicle.level().getBlockState(new BlockPos(x, y, z));
            SurfaceType surfaceType = getSurfaceType(state);
            if(surfaceType == SurfaceType.NONE)
                continue;
            IWheelType wheelType = optional.get();
            surfaceModifier += (1.0F - surfaceType.wheelFunction.apply(wheelType));
            wheelCount++;
        }
        return 1.0F - (surfaceModifier / Math.max(1F, wheelCount));
    }

    public enum SurfaceType
    {
        SOLID(IWheelType::getRoadMultiplier),
        DIRT(IWheelType::getDirtMultiplier),
        SNOW(IWheelType::getSnowMultiplier),
        NONE(type -> 0F);

        private Function<IWheelType, Float> wheelFunction;

        SurfaceType(Function<IWheelType, Float> wheelFunction)
        {
            this.wheelFunction = wheelFunction;
        }
    }
}
