package com.mrcrayfish.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.entity.TankShellEntity;
import com.mrcrayfish.vehicle.util.RenderUtil;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Draws the tank shell along its flight direction, fully lit (its tracer glows)
 */
public class TankShellRenderer extends EntityRenderer<TankShellEntity>
{
    public TankShellRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(TankShellEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light)
    {
        matrixStack.pushPose();
        matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())));
        matrixStack.mulPose(Axis.POSITIVE_X.rotationDegrees(-Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        if(entity.isLight())
        {
            matrixStack.scale(0.5F, 0.5F, 0.5F);
        }
        RenderUtil.renderColoredModel(SpecialModels.TANK_PROJECTILE.getModel(), ItemDisplayContext.NONE, false, matrixStack, buffer, -1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TankShellEntity entity)
    {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
