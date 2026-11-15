package com.mrcrayfish.vehicle.client.render.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mrcrayfish.vehicle.block.MilitaryCrateBlock;
import com.mrcrayfish.vehicle.client.EntityRayTracer;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.tileentity.MilitaryCrateTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Draws the military crate while it opens (closed, it is the block model of its main cell, baked in the chunk):
 * the lid jumps off its latches and breaks apart (MilitaryCrateTileEntity.LID_GONE), the four walls turn outwards
 * about their bottom edge one after the other (front, left, back, right: they land on the ground with the sounds of
 * the vehicle crate, at 90, 110, 130 and 150), then the vehicle grows out of the crate as from the vehicle crate.
 */
public class MilitaryCrateRenderer implements BlockEntityRenderer<MilitaryCrateTileEntity>
{
    /** Height of the bottom edge of the walls (on the skids), of the floor, and how far a wall goes down to lie on the ground (blocks) */
    private static final float HINGE = 3F / 16F, FLOOR = 5F / 16F, DROP = 2.5F / 16F;
    /** How high the lid flies above the line from the crate to where it lands (blocks) */
    private static final float LID_ARC = 1.6F;

    public MilitaryCrateRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MilitaryCrateTileEntity crate, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
    {
        if(!crate.isOpened() || crate.getLevel() == null)
            return;
        Direction facing = crate.getBlockState().getValue(MilitaryCrateBlock.FACING);
        pose.pushPose();
        // from the main cell (front, left, bottom) to the frame of the models, turned to face `facing`
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5, 0, -1.5);

        light = LevelRenderer.getLightColor(crate.getLevel(), crate.getBlockPos().above());
        float time = crate.getTimer() + 5F * partialTicks;
        renderCrate(pose, buffers, light, time, MilitaryCrateTileEntity.lidLanding(crate.getBlockPos()));

        Entity entity = crate.getEntity();
        if(entity != null)
        {
            // the vehicle, small on the floor, then growing out of the crate (as from the vehicle crate)
            float progress = Mth.clamp((time - 150F) / 100F, 0F, 1F);
            @SuppressWarnings("unchecked")
            Pair<Float, Float> scaleAndOffset = EntityRayTracer.instance().getCrateScaleAndOffset((EntityType<? extends VehicleEntity>) entity.getType());
            // the crate is twice as wide as the vehicle crate inside
            float start = Math.min(1F, scaleAndOffset.getLeft() * 2F);
            float scale = start + (1F - start) * progress;
            pose.pushPose();
            pose.translate(1, FLOOR * (1F - progress), 1);
            pose.translate(0, 0, scaleAndOffset.getRight() * (1F - progress) * scale);
            if(time >= 150F)
            {
                pose.translate(0, Math.sin(Math.PI * progress) * 5, 0);
                pose.mulPose(Axis.YP.rotationDegrees(720F * progress));
            }
            pose.scale(scale, scale, scale);
            EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            renderer.render(entity, 0F, partialTicks, pose, buffers, light);
            pose.popPose();
        }
        pose.popPose();
    }

    /**
     * The crate opening, `time` on the timer of the crate (0 opened, 250 gone), in the frame of its models (blocks,
     * front at +z, origin at the left back bottom corner); `landing`: where the lid lands
     * (MilitaryCrateTileEntity.lidLanding). Also used by the image exporter of the development client.
     */
    public static void renderCrate(PoseStack pose, MultiBufferSource buffers, int light, float time, float[] landing)
    {
        VertexConsumer consumer = buffers.getBuffer(Sheets.solidBlockSheet());
        renderModel(SpecialModels.MILITARY_CRATE_BASE.getModel(), pose, consumer, light);

        // the lid: thrown off over one side of the crate, it flies in an arc turning over once and lands flat on the
        // ground 3 to 4 blocks away (LID_LANDED), where it lies until it breaks apart (LID_GONE)
        if(time < MilitaryCrateTileEntity.LID_GONE)
        {
            float t = Math.min(1F, time / MilitaryCrateTileEntity.LID_LANDED);
            float x = Mth.lerp(t, 1F, landing[0]), z = Mth.lerp(t, 1F, landing[1]);
            float y = Mth.lerp(t, MilitaryCrateTileEntity.LID_HEIGHT, MilitaryCrateTileEntity.LID_GROUND) + LID_ARC * 4F * t * (1F - t);
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(landing[2] * t));
            pose.mulPose(Axis.ZP.rotationDegrees(-landing[3] * 360F * t));
            pose.translate(-1, -MilitaryCrateTileEntity.LID_HEIGHT, -1);
            renderModel(SpecialModels.MILITARY_CRATE_LID.getModel(), pose, consumer, light);
            pose.popPose();
        }

        // the walls: 0 front, 1 left, 2 back, 3 right (the back and right ones are the front and left ones turned
        // half a turn about the middle); each falls faster and faster, as the panels of the vehicle crate, and goes
        // down onto the ground as it lies flat (it rests on its steel caps)
        for(int i = 0; i < 4; i++)
        {
            float progress = Mth.clamp((time - i * 20F) / 90F, 0F, 1F);
            float angle = (float) (1.0 - Math.cos(Math.toRadians(progress * progress * 90F))) * 90F;
            float drop = Mth.sin(angle * Mth.DEG_TO_RAD) * DROP;
            pose.pushPose();
            if(i >= 2)
            {
                pose.translate(1, 0, 1);
                pose.mulPose(Axis.YP.rotationDegrees(180F));
                pose.translate(-1, 0, -1);
            }
            if(i % 2 == 0)
            {
                // about the bottom front edge (x axis): the top goes forwards
                pose.translate(0, HINGE - drop, 2);
                pose.mulPose(Axis.XP.rotationDegrees(angle));
                pose.translate(0, -HINGE, -2);
                renderModel(SpecialModels.MILITARY_CRATE_WALL_END.getModel(), pose, consumer, light);
            }
            else
            {
                // about the bottom left edge (z axis): the top goes to the left
                pose.translate(0, HINGE - drop, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(angle));
                pose.translate(0, -HINGE, 0);
                renderModel(SpecialModels.MILITARY_CRATE_WALL_SIDE.getModel(), pose, consumer, light);
            }
            pose.popPose();
        }
    }

    /** All the quads of a baked model (positions in blocks) */
    private static void renderModel(BakedModel model, PoseStack pose, VertexConsumer consumer, int light)
    {
        RandomSource random = RandomSource.create(42L);
        PoseStack.Pose last = pose.last();
        for(Direction direction : Direction.values())
        {
            random.setSeed(42L);
            for(BakedQuad quad : model.getQuads(null, direction, random))
            {
                consumer.putBulkData(last, quad, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
            }
        }
        random.setSeed(42L);
        for(BakedQuad quad : model.getQuads(null, null, random))
        {
            consumer.putBulkData(last, quad, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(MilitaryCrateTileEntity crate)
    {
        // the vehicle grows far out of the crate
        return true;
    }

    @Override
    public int getViewDistance()
    {
        return 256;
    }
}
