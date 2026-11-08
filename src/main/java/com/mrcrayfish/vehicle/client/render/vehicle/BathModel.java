package com.mrcrayfish.vehicle.client.render.vehicle;

import net.minecraft.client.renderer.BiomeColors;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrcrayfish.vehicle.client.EntityRayTracer;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.client.render.AbstractPlaneRenderer;
import com.mrcrayfish.vehicle.client.render.Axis;
import com.mrcrayfish.vehicle.common.Seat;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.entity.vehicle.BathEntity;
import javax.annotation.Nullable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Author: MrCrayfish
 */
public class BathModel extends AbstractPlaneRenderer<BathEntity>
{
    private static final int DEFAULT_WATER_COLOR = 0x3F76E4;

    public BathModel(VehicleProperties defaultProperties)
    {
        super(defaultProperties);
    }

    @Override
    protected void render(@Nullable BathEntity vehicle, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, float partialTicks, int light)
    {
        matrixStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90F));
        // The water of the Furniture Restored bath is tinted (tint index 0) with the water color of the biome
        int waterColor = vehicle != null ? BiomeColors.getAverageWaterColor(vehicle.level(), vehicle.blockPosition()) : DEFAULT_WATER_COLOR;
        this.renderDamagedPart(vehicle, SpecialModels.BATH.getModel(), matrixStack, renderTypeBuffer, light, waterColor);
    }

    @Override
    public void applyPlayerRender(BathEntity entity, Player player, float partialTicks, PoseStack matrixStack, VertexConsumer builder)
    {
        int index = entity.getSeatTracker().getSeatIndex(player.getUUID());
        if(index != -1)
        {
            VehicleProperties properties = entity.getProperties();
            Seat seat = properties.getSeats().get(index);
            Vec3 seatVec = seat.getPosition().add(0, properties.getAxleOffset() + properties.getWheelOffset(), 0).scale(properties.getBodyPosition().getScale()).scale(0.0625);
            double scale = 32.0 / 30.0;
            double offsetX = seatVec.x * scale;
            double offsetY = (seatVec.y + player.getMyRidingOffset() - 0.5) * scale + 24 * 0.0625; //Player is 2 blocks high tall but renders at 1.8 blocks tall
            double offsetZ = seatVec.z * scale;
            matrixStack.translate(offsetX, offsetY, offsetZ);
            float bodyPitch = entity.prevBodyRotationX + (entity.bodyRotationX - entity.prevBodyRotationX) * partialTicks;
            float bodyRoll = entity.prevBodyRotationZ + (entity.bodyRotationZ - entity.prevBodyRotationZ) * partialTicks;
            matrixStack.mulPose(Axis.POSITIVE_Z.rotationDegrees(bodyRoll));
            matrixStack.mulPose(Axis.POSITIVE_X.rotationDegrees(-bodyPitch));
            matrixStack.translate(-offsetX, -offsetY, -offsetX);
        }
    }

    @Override
    public void applyPlayerModel(BathEntity entity, Player player, PlayerModel model, float partialTicks)
    {
        model.rightLeg.xRot = (float) Math.toRadians(-85F);
        model.rightLeg.yRot = (float) Math.toRadians(10F);
        model.leftLeg.xRot = (float) Math.toRadians(-85F);
        model.leftLeg.yRot = (float) Math.toRadians(-10F);
        model.rightArm.xRot = (float) Math.toRadians(-80F);
        model.rightArm.yRot = (float) Math.toRadians(5F);
        model.rightArm.zRot = (float) Math.toRadians(0F);
        model.leftArm.xRot = (float) Math.toRadians(-80F);
        model.leftArm.yRot = (float) Math.toRadians(-5F);
        model.leftArm.zRot = (float) Math.toRadians(0F);
    }

    @Nullable
    @Override
    public EntityRayTracer.IRayTraceTransforms getRayTraceTransforms()
    {
        return (tracer, transforms, parts) ->
        {
            EntityRayTracer.createTransformListForPart(SpecialModels.BATH, parts, transforms,
                    EntityRayTracer.MatrixTransformation.createRotation(Axis.POSITIVE_Y, 90F));
        };
    }
}
