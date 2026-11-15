package com.mrcrayfish.vehicle.client.render.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.client.EntityRayTracer;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.client.render.AbstractPoweredRenderer;
import com.mrcrayfish.vehicle.client.render.Axis;
import com.mrcrayfish.vehicle.common.entity.PartPosition;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import com.mrcrayfish.vehicle.init.ModEntities;
import com.mrcrayfish.vehicle.util.RenderUtil;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Draws the tank: the hull, the running gear (tracks as one of 4 frames that follow the distance
 * travelled, turning road wheels, idlers and sprockets), the turret turned towards the driver's view
 * and the gun with its pitch and recoil.
 *
 * All positions are in pixels of the tank models (made by tools/concepts/tank/build.js), whose hull
 * is centred on x 8 and z 8.
 */
public class TankRenderer extends AbstractPoweredRenderer<TankEntity>
{
    private static final float[] WHEELS_Z = {23.1F, 18.4F, 13.7F, 9.0F, 4.3F, -0.4F, -5.1F};
    private static final float WHEEL_Y = 2.36F, WHEEL_RADIUS = 1.8F, WHEEL_X = 0.4F;
    private static final float IDLER_Z = 27F, IDLER_Y = 3.6F, IDLER_RADIUS = 1.6F;
    private static final float SPROCKET_Z = -9.3F, SPROCKET_Y = 4.4F, SPROCKET_RADIUS = 2.2F;
    /** Length of a track link, in model pixels; the 4 track frames are a quarter of a link apart */
    private static final float LINK = 2.0F;
    private static final SpecialModels[] TRACK_A = {SpecialModels.TANK_TRACK_LEFT_0, SpecialModels.TANK_TRACK_LEFT_1, SpecialModels.TANK_TRACK_LEFT_2, SpecialModels.TANK_TRACK_LEFT_3};
    private static final SpecialModels[] TRACK_B = {SpecialModels.TANK_TRACK_RIGHT_0, SpecialModels.TANK_TRACK_RIGHT_1, SpecialModels.TANK_TRACK_RIGHT_2, SpecialModels.TANK_TRACK_RIGHT_3};
    private static final float RECOIL_DISTANCE = 2.5F;

    public TankRenderer(VehicleProperties defaultProperties)
    {
        super(defaultProperties);
    }

    @Override
    public void setupTransformsAndRender(@Nullable TankEntity vehicle, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, float partialTicks, int light)
    {
        matrixStack.pushPose();
        // Rocking after a shot of the 120 mm gun: around the axis across the gun, the gun side going up
        float rock = vehicle != null ? vehicle.getRock(partialTicks) : 0F;
        if(rock != 0F)
        {
            matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees(-vehicle.rockYaw));
            matrixStack.mulPose(Axis.POSITIVE_X.rotationDegrees(rock));
            matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees(vehicle.rockYaw));
        }
        VehicleProperties properties = this.vehiclePropertiesProperty.get(vehicle);
        PartPosition bodyPosition = properties.getBodyPosition();
        matrixStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) bodyPosition.getRotX()));
        matrixStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) bodyPosition.getRotY()));
        matrixStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) bodyPosition.getRotZ()));
        matrixStack.translate(bodyPosition.getX(), bodyPosition.getY(), bodyPosition.getZ());
        matrixStack.scale((float) bodyPosition.getScale(), (float) bodyPosition.getScale(), (float) bodyPosition.getScale());
        matrixStack.translate(0, 0.5, 0);
        matrixStack.translate(0, properties.getAxleOffset() * 0.0625F, 0);
        matrixStack.translate(0, properties.getWheelOffset() * 0.0625F, 0);

        this.render(vehicle, matrixStack, renderTypeBuffer, partialTicks, light);

        this.renderFuelPort(vehicle, matrixStack, renderTypeBuffer, light);
        this.renderKeyPort(vehicle, matrixStack, renderTypeBuffer, light);
        matrixStack.popPose();
    }

    @Override
    protected void render(@Nullable TankEntity vehicle, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, float partialTicks, int light)
    {
        this.renderDamagedPart(vehicle, SpecialModels.TANK_HULL.getModel(), matrixStack, renderTypeBuffer, light);

        /* Running gear: side A has the low model x, side B is its mirror */
        float distanceA = vehicle != null ? Mth.lerp(partialTicks, vehicle.prevTrackDistanceA, vehicle.trackDistanceA) : 0F;
        float distanceB = vehicle != null ? Mth.lerp(partialTicks, vehicle.prevTrackDistanceB, vehicle.trackDistanceB) : 0F;
        this.renderRunningGear(matrixStack, renderTypeBuffer, light, false, distanceA);
        this.renderRunningGear(matrixStack, renderTypeBuffer, light, true, distanceB);

        /* Turret, turned around its ring */
        float turretYaw = vehicle != null ? vehicle.getTurretYaw(partialTicks) : 0F;
        float gunPitch = vehicle != null ? vehicle.getGunPitch(partialTicks) : 0F;
        float recoil = vehicle != null ? Mth.lerp(partialTicks, vehicle.prevRecoil, vehicle.recoil) : 0F;
        matrixStack.pushPose();
        translatePixels(matrixStack, (float) TankEntity.TURRET_PIVOT.x, (float) TankEntity.TURRET_PIVOT.y - 8F, (float) TankEntity.TURRET_PIVOT.z);
        matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees(-turretYaw));
        translatePixels(matrixStack, 0F, 8F, 0F); // turret model pivot (8, 0, 8)
        this.renderDamagedPart(vehicle, SpecialModels.TANK_TURRET.getModel(), matrixStack, renderTypeBuffer, light);

        /* Gun, raised around its trunnions, pushed back by the recoil */
        translatePixels(matrixStack, (float) TankEntity.GUN_PIVOT.x, (float) TankEntity.GUN_PIVOT.y - 8F, (float) TankEntity.GUN_PIVOT.z);
        matrixStack.mulPose(Axis.POSITIVE_X.rotationDegrees(-gunPitch));
        translatePixels(matrixStack, 0F, 0F, -recoil * RECOIL_DISTANCE);
        translatePixels(matrixStack, 0F, 5F, 18F); // gun model pivot (8, 3, -10)
        this.renderDamagedPart(vehicle, SpecialModels.TANK_BARREL.getModel(), matrixStack, renderTypeBuffer, light);
        matrixStack.popPose();
    }

    private void renderRunningGear(PoseStack matrixStack, MultiBufferSource buffer, int light, boolean sideB, float distance)
    {
        int frame = Math.floorMod((int) Math.floor(distance / (LINK / 4F)), 4);
        RenderUtil.renderColoredModel((sideB ? TRACK_B : TRACK_A)[frame].getModel(), ItemDisplayContext.NONE, false, matrixStack, buffer, -1, light, OverlayTexture.NO_OVERLAY);

        float x = sideB ? 16F - WHEEL_X : WHEEL_X;
        for(float z : WHEELS_Z)
        {
            this.renderWheel(matrixStack, buffer, light, SpecialModels.TANK_ROAD_WHEEL, x, WHEEL_Y, z, distance / WHEEL_RADIUS, 1F, sideB);
        }
        this.renderWheel(matrixStack, buffer, light, SpecialModels.TANK_ROAD_WHEEL, x, IDLER_Y, IDLER_Z, distance / IDLER_RADIUS, IDLER_RADIUS / WHEEL_RADIUS, sideB);
        this.renderWheel(matrixStack, buffer, light, SpecialModels.TANK_SPROCKET, x, SPROCKET_Y, SPROCKET_Z, distance / SPROCKET_RADIUS, 1F, sideB);
    }

    /**
     * Wheel models are centred on (8, 8, 8) with their outer face towards -x; turned half a turn for side B
     */
    private void renderWheel(PoseStack matrixStack, MultiBufferSource buffer, int light, SpecialModels model, float x, float y, float z, float angle, float scale, boolean sideB)
    {
        matrixStack.pushPose();
        translatePixels(matrixStack, x - 8F, y - 8F, z - 8F);
        matrixStack.mulPose(Axis.POSITIVE_X.rotation(angle));
        if(sideB)
        {
            matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees(180F));
        }
        matrixStack.scale(scale, scale, scale);
        RenderUtil.renderColoredModel(model.getModel(), ItemDisplayContext.NONE, false, matrixStack, buffer, -1, light, OverlayTexture.NO_OVERLAY);
        matrixStack.popPose();
    }

    /**
     * The fuel filler belongs to the hull (its models are in the frame of the hull, tank.json puts the
     * fuel port at zero): drawn closed also when the tank takes no fuel and on the previews (no vehicle)
     */
    @Override
    protected void renderFuelPort(@Nullable TankEntity vehicle, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light)
    {
        if(vehicle != null && vehicle.shouldRenderFuelPort() && vehicle.requiresFuel())
        {
            super.renderFuelPort(vehicle, matrixStack, renderTypeBuffer, light);
            return;
        }
        RenderUtil.renderColoredModel(SpecialModels.TANK_FUEL_CAP.getModel(), ItemDisplayContext.NONE, false, matrixStack, renderTypeBuffer, -1, light, OverlayTexture.NO_OVERLAY);
    }

    private static void translatePixels(PoseStack matrixStack, float x, float y, float z)
    {
        matrixStack.translate(x * 0.0625F, y * 0.0625F, z * 0.0625F);
    }

    @Nullable
    @Override
    public EntityRayTracer.IRayTraceTransforms getRayTraceTransforms()
    {
        return (tracer, transforms, parts) ->
        {
            EntityRayTracer.createTransformListForPart(SpecialModels.TANK_HULL, parts, transforms);
            EntityRayTracer.createFuelPartTransforms(ModEntities.TANK.get(), SpecialModels.TANK_FUEL_CAP, parts, transforms);
            EntityRayTracer.createKeyPortTransforms(ModEntities.TANK.get(), parts, transforms);
        };
    }
}
