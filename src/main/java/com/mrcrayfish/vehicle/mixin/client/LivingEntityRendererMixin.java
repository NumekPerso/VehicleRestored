package com.mrcrayfish.vehicle.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrcrayfish.vehicle.client.event.PlayerModelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerModelEvent.Render} around the drawing of the player model. The current
 * player is remembered at the start of the render method because the model draw call does not
 * receive the entity.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>>
{
    @Shadow
    protected M model;

    @Unique
    private T vehicleRestored$entity;

    @Unique
    private int vehicleRestored$light;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void vehicleRestored$onRenderStart(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci)
    {
        this.vehicleRestored$entity = entity;
        this.vehicleRestored$light = light;
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void vehicleRestored$onRenderEnd(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci)
    {
        this.vehicleRestored$entity = null;
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void vehicleRestored$beforeModelRender(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci)
    {
        if(entity instanceof Player player && this.model instanceof PlayerModel<?> playerModel)
        {
            MinecraftForge.EVENT_BUS.post(new PlayerModelEvent.Render.Pre(player, playerModel, poseStack, null, light, 0, partialTicks));
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", shift = At.Shift.AFTER))
    private void vehicleRestored$afterModelRender(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci)
    {
        if(entity instanceof Player player && this.model instanceof PlayerModel<?> playerModel)
        {
            MinecraftForge.EVENT_BUS.post(new PlayerModelEvent.Render.Post(player, playerModel, poseStack, null, light, 0, partialTicks));
        }
    }
}
