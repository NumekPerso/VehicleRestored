package com.mrcrayfish.vehicle.mixin.client;

import com.mrcrayfish.vehicle.client.event.PlayerModelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerModelEvent.SetupAngles.Post} so the mod can pose the player when riding a
 * vehicle, holding a vehicle, spraying or fueling.
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin
{
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void vehicleRestored$afterSetupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci)
    {
        if(!(entity instanceof Player player))
            return;

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        MinecraftForge.EVENT_BUS.post(new PlayerModelEvent.SetupAngles.Post(player, model, Minecraft.getInstance().getFrameTime()));

        // Copy the changes to the outer layers of the model
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }
}
