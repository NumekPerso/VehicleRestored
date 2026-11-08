package com.mrcrayfish.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.client.EntityRayTracer;
import com.mrcrayfish.vehicle.entity.EntityJack;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Author: MrCrayfish
 */
public class EntityVehicleRenderer<T extends VehicleEntity & EntityRayTracer.IEntityRayTraceable> extends EntityRenderer<T>
{
    private final AbstractVehicleRenderer<T> wrapper;

    public EntityVehicleRenderer(EntityRendererProvider.Context context, AbstractVehicleRenderer<T> wrapper)
    {
        super(context);
        this.wrapper = wrapper;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity)
    {
        return null;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light)
    {
        if(!entity.isAlive())
            return;

        if(entity.getVehicle() instanceof EntityJack)
            return;

        matrixStack.pushPose();
        wrapper.applyPreRotations(entity, matrixStack, partialTicks);
        matrixStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));
        this.setupBreakAnimation(entity, matrixStack, partialTicks);
        wrapper.setupTransformsAndRender(entity, matrixStack, renderTypeBuffer, partialTicks, light);
        matrixStack.popPose();

        EntityRayTracer.instance().renderRayTraceElements(entity, matrixStack, renderTypeBuffer, entityYaw);
    }

    private void setupBreakAnimation(VehicleEntity vehicle, PoseStack matrixStack, float partialTicks)
    {
        float timeSinceHit = (float) vehicle.getTimeSinceHit() - partialTicks;
        if(timeSinceHit > 0.0F)
        {
            matrixStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Mth.sin(timeSinceHit) * timeSinceHit));
        }
    }
}
