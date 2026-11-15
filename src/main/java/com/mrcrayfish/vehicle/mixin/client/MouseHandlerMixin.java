package com.mrcrayfish.vehicle.mixin.client;

import com.mrcrayfish.vehicle.client.handler.TankHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Turns the view slower while the driver of a tank zooms in, in proportion to the zoom, so aiming
 * stays precise at x4 or x10 (Forge 1.20.1 has no event to change the mouse sensitivity).
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin
{
    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 0)
    private double vehicleRestored$zoomTurnX(double yaw)
    {
        return yaw * TankHandler.getSensitivityScale();
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 1)
    private double vehicleRestored$zoomTurnY(double pitch)
    {
        return pitch * TankHandler.getSensitivityScale();
    }
}
