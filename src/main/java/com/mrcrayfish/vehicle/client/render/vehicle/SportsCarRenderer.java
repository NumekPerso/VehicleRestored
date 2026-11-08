package com.mrcrayfish.vehicle.client.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.client.EntityRayTracer;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.client.render.AbstractLandVehicleRenderer;
import com.mrcrayfish.vehicle.client.render.Axis;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.entity.vehicle.SportsCarEntity;
import com.mrcrayfish.vehicle.init.ModEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Renderer of the sports car (from the 1.16.X dev branch). The dev branch drew the doors, hood,
 * boot, dashboard, roof and seat through its cosmetics system; they are drawn here at the same
 * offsets.
 *
 * Author: MrCrayfish
 */
public class SportsCarRenderer extends AbstractLandVehicleRenderer<SportsCarEntity>
{
    private static final Part[] PARTS = {
            new Part(SpecialModels.SPORTS_CAR_HOOD, 0.0F, 13.5F, 13.5F),
            new Part(SpecialModels.SPORTS_CAR_LEFT_DOOR, 15.0F, 1.0F, 10.0F),
            new Part(SpecialModels.SPORTS_CAR_RIGHT_DOOR, -15.0F, 1.0F, 10.0F),
            new Part(SpecialModels.SPORTS_CAR_BOOT, 0.0F, 15.5F, -23.5F),
            new Part(SpecialModels.SPORTS_CAR_DASHBOARD, 0.0F, 14.0F, 12.0F),
            new Part(SpecialModels.SPORTS_CAR_ROOF, 0.0F, 14.0F, 0.0F),
            new Part(SpecialModels.SPORTS_CAR_SEAT, 0.0F, 1.0F, 0.0F)
    };

    public SportsCarRenderer(VehicleProperties defaultProperties)
    {
        super(defaultProperties);
    }

    @Override
    protected void render(@Nullable SportsCarEntity vehicle, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, float partialTicks, int light)
    {
        this.renderDamagedPart(vehicle, SpecialModels.SPORTS_CAR_BODY.getModel(), matrixStack, renderTypeBuffer, light);

        for(Part part : PARTS)
        {
            matrixStack.pushPose();
            matrixStack.translate(part.x * 0.0625, part.y * 0.0625, part.z * 0.0625);
            matrixStack.translate(0.0, -0.5, 0.0);
            this.renderDamagedPart(vehicle, part.model.getModel(), matrixStack, renderTypeBuffer, light);
            matrixStack.popPose();
        }

        //Steering wheel, turning with the steering
        matrixStack.pushPose();
        matrixStack.translate(-7.0 * 0.0625, 4.0 * 0.0625, 2.6 * 0.0625);
        matrixStack.mulPose(Axis.POSITIVE_X.rotationDegrees(-67.5F));
        matrixStack.scale(0.7F, 0.7F, 0.7F);
        if(vehicle != null)
        {
            float wheelAngle = vehicle.prevWheelAngle + (vehicle.wheelAngle - vehicle.prevWheelAngle) * partialTicks;
            matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees((wheelAngle / (float) vehicle.getMaxTurnAngle()) * 25F));
        }
        this.renderDamagedPart(vehicle, SpecialModels.SPORTS_CAR_STEERING_WHEEL.getModel(), matrixStack, renderTypeBuffer, light);
        matrixStack.popPose();
    }

    @Override
    public void applyPlayerModel(SportsCarEntity entity, Player player, PlayerModel<AbstractClientPlayer> model, float partialTicks)
    {
        model.rightLeg.xRot = (float) Math.toRadians(-85F);
        model.rightLeg.yRot = (float) Math.toRadians(10F);
        model.leftLeg.xRot = (float) Math.toRadians(-85F);
        model.leftLeg.yRot = (float) Math.toRadians(-10F);

        // Only the driver holds the steering wheel
        if(entity.getControllingPassenger() != player)
            return;

        float wheelAngle = entity.prevRenderWheelAngle + (entity.renderWheelAngle - entity.prevRenderWheelAngle) * partialTicks;
        float turnRotation = (wheelAngle / (float) entity.getMaxTurnAngle()) * 25F / 2F;
        model.rightArm.xRot = (float) Math.toRadians(-75F - turnRotation);
        model.rightArm.yRot = (float) Math.toRadians(-7F);
        model.leftArm.xRot = (float) Math.toRadians(-75F + turnRotation);
        model.leftArm.yRot = (float) Math.toRadians(7F);
    }

    @Nullable
    @Override
    public EntityRayTracer.IRayTraceTransforms getRayTraceTransforms()
    {
        return (entityRayTracer, transforms, parts) ->
        {
            EntityRayTracer.createTransformListForPart(SpecialModels.SPORTS_CAR_BODY, parts, transforms);
            for(Part part : PARTS)
            {
                if(part.model == SpecialModels.SPORTS_CAR_DASHBOARD || part.model == SpecialModels.SPORTS_CAR_SEAT)
                    continue;
                EntityRayTracer.createTransformListForPart(part.model, parts, transforms,
                        EntityRayTracer.MatrixTransformation.createTranslation(part.x * 0.0625F, part.y * 0.0625F, part.z * 0.0625F),
                        EntityRayTracer.MatrixTransformation.createTranslation(0.0F, -0.5F, 0.0F));
            }
            EntityRayTracer.createFuelPartTransforms(ModEntities.SPORTS_CAR.get(), SpecialModels.FUEL_DOOR_CLOSED, parts, transforms);
            EntityRayTracer.createKeyPortTransforms(ModEntities.SPORTS_CAR.get(), parts, transforms);
        };
    }

    private record Part(SpecialModels model, float x, float y, float z) {}
}
