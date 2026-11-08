package com.mrcrayfish.vehicle.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.client.event.RenderItemEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link RenderItemEvent.Held.Pre} before an item held by an entity is rendered in third
 * person, so it can be hidden (for example while holding a gas pump nozzle).
 */
@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin
{
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void vehicleRestored$onRenderArmWithItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci)
    {
        if(MinecraftForge.EVENT_BUS.post(new RenderItemEvent.Held.Pre(entity, stack, context, poseStack, buffer, light)))
        {
            ci.cancel();
        }
    }
}
