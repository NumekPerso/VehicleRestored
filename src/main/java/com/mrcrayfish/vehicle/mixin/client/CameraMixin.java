package com.mrcrayfish.vehicle.mixin.client;

import com.mrcrayfish.vehicle.entity.VehicleEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the third person camera further back (and higher) for large vehicles, such as the
 * helicopters and the plane, so the vehicle does not fill the screen. The distances come from the
 * unreleased development version of the Vehicle Mod.
 */
@Mixin(Camera.class)
public abstract class CameraMixin
{
    @Shadow
    private Entity entity;

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private void vehicleRestored$raiseCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci)
    {
        if(entity.getVehicle() instanceof VehicleEntity vehicle && vehicle.getCameraHeightOffset() != 0.0)
        {
            this.setPosition(this.getPosition().add(0, vehicle.getCameraHeightOffset(), 0));
        }
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private double vehicleRestored$cameraDistance(double distance)
    {
        if(this.entity != null && this.entity.getVehicle() instanceof VehicleEntity vehicle)
        {
            return vehicle.getCameraDistance();
        }
        return distance;
    }
}
