package com.mrcrayfish.vehicle.client.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrcrayfish.vehicle.client.EntityRayTracer;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.client.render.AbstractHelicopterRenderer;
import com.mrcrayfish.vehicle.client.render.Axis;
import com.mrcrayfish.vehicle.common.Seat;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.entity.vehicle.CompactHelicopterEntity;
import com.mrcrayfish.vehicle.init.ModEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Renderer of the compact helicopter (from the 1.16.X dev branch). The child transforms of the
 * dev "base.complex" model (joystick, main rotor and tail rotor) are applied here.
 *
 * Author: MrCrayfish
 */
public class CompactHelicopterRenderer extends AbstractHelicopterRenderer<CompactHelicopterEntity>
{
    public CompactHelicopterRenderer(VehicleProperties defaultProperties)
    {
        super(defaultProperties);
    }

    @Override
    protected void render(@Nullable CompactHelicopterEntity vehicle, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, float partialTicks, int light)
    {
        this.renderDamagedPart(vehicle, SpecialModels.COMPACT_HELICOPTER_BODY.getModel(), matrixStack, renderTypeBuffer, light);

        //Seat
        matrixStack.pushPose();
        matrixStack.translate(0.0, 11.0 * 0.0625, 0.0);
        matrixStack.translate(0.0, -0.5, 0.0);
        this.renderDamagedPart(vehicle, SpecialModels.COMPACT_HELICOPTER_SEAT.getModel(), matrixStack, renderTypeBuffer, light);
        matrixStack.popPose();

        float bladeRotation = vehicle != null ? Mth.lerp(partialTicks, vehicle.prevBladeRotation, vehicle.bladeRotation) : 0F;

        //Joystick, tilting towards the travel direction
        matrixStack.pushPose();
        matrixStack.translate(-7.5 * 0.0625, 4.0 * 0.0625, 13.5 * 0.0625);
        if(vehicle != null && vehicle.getControllingPassenger() != null)
        {
            float relativeDirection = (vehicle.getTravelDirection() - vehicle.getYRot()) * Mth.DEG_TO_RAD;
            float speed = vehicle.getTravelSpeed();
            float forwards = Mth.cos(relativeDirection) * speed;
            float sidewards = Mth.sin(relativeDirection) * speed;
            matrixStack.mulPose(Axis.POSITIVE_X.rotationDegrees(forwards * 10F));
            matrixStack.mulPose(Axis.POSITIVE_Z.rotationDegrees(-sidewards * 10F));
        }
        this.renderDamagedPart(vehicle, SpecialModels.COMPACT_HELICOPTER_JOYSTICK.getModel(), matrixStack, renderTypeBuffer, light);
        matrixStack.popPose();

        //Main rotor
        matrixStack.pushPose();
        matrixStack.translate(0.0, 37.0 * 0.0625, -9.0 * 0.0625);
        matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees(bladeRotation));
        this.renderDamagedPart(vehicle, SpecialModels.COMPACT_HELICOPTER_BLADES.getModel(), matrixStack, renderTypeBuffer, light);
        matrixStack.popPose();

        //Tail rotor
        matrixStack.pushPose();
        matrixStack.translate(3.0 * 0.0625, 24.5 * 0.0625, -81.5 * 0.0625);
        matrixStack.mulPose(Axis.POSITIVE_X.rotationDegrees(bladeRotation));
        this.renderDamagedPart(vehicle, SpecialModels.COMPACT_HELICOPTER_TAIL_ROTOR.getModel(), matrixStack, renderTypeBuffer, light);
        matrixStack.popPose();
    }

    @Override
    public void applyPlayerModel(CompactHelicopterEntity entity, Player player, PlayerModel<AbstractClientPlayer> model, float partialTicks)
    {
        model.rightArm.xRot = (float) Math.toRadians(-55F);
        model.rightArm.yRot = (float) Math.toRadians(10F);
        model.leftArm.xRot = (float) Math.toRadians(-30F);
        model.leftArm.yRot = (float) Math.toRadians(-10F);
        model.rightLeg.xRot = (float) Math.toRadians(-90F);
        model.rightLeg.yRot = (float) Math.toRadians(15F);
        model.leftLeg.xRot = (float) Math.toRadians(-90F);
        model.leftLeg.yRot = (float) Math.toRadians(-15F);
    }

    @Override
    public void applyPlayerRender(CompactHelicopterEntity entity, Player player, float partialTicks, PoseStack matrixStack, VertexConsumer builder)
    {
        int index = entity.getSeatTracker().getSeatIndex(player.getUUID());
        if(index != -1)
        {
            VehicleProperties properties = entity.getProperties();
            Seat seat = properties.getSeats().get(index);
            Vec3 seatVec = seat.getPosition().add(0, properties.getAxleOffset() + properties.getWheelOffset(), 0).scale(properties.getBodyPosition().getScale()).multiply(-1, 1, 1).scale(0.0625);
            double scale = 32.0 / 30.0;
            double offsetX = -seatVec.x * scale;
            double offsetY = (seatVec.y + player.getMyRidingOffset() + 0.3) * scale + 24 * 0.0625; //Player is 2 blocks high tall but renders at 1.8 blocks tall
            double offsetZ = seatVec.z * scale;
            float entityYaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;

            matrixStack.translate(offsetX, offsetY, offsetZ);
            matrixStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));
            matrixStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-(entity.prevBodyRotationX + (entity.bodyRotationX - entity.prevBodyRotationX) * partialTicks)));
            matrixStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(entity.prevBodyRotationZ + (entity.bodyRotationZ - entity.prevBodyRotationZ) * partialTicks));
            matrixStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(entityYaw));
            matrixStack.translate(-offsetX, -offsetY, -offsetZ);
        }
    }

    @Nullable
    @Override
    public EntityRayTracer.IRayTraceTransforms getRayTraceTransforms()
    {
        return (tracer, transforms, parts) ->
        {
            EntityRayTracer.createTransformListForPart(SpecialModels.COMPACT_HELICOPTER_BODY, parts, transforms);
            EntityRayTracer.createTransformListForPart(SpecialModels.COMPACT_HELICOPTER_SEAT, parts, transforms,
                    EntityRayTracer.MatrixTransformation.createTranslation(0.0F, 11.0F * 0.0625F, 0.0F),
                    EntityRayTracer.MatrixTransformation.createTranslation(0.0F, -0.5F, 0.0F));
            EntityRayTracer.createFuelPartTransforms(ModEntities.COMPACT_HELICOPTER.get(), SpecialModels.FUEL_DOOR_CLOSED, parts, transforms);
            EntityRayTracer.createKeyPortTransforms(ModEntities.COMPACT_HELICOPTER.get(), parts, transforms);
        };
    }
}
