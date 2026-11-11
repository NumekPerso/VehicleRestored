package com.mrcrayfish.vehicle.client.tooltip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrcrayfish.vehicle.client.render.AbstractPoweredRenderer;
import com.mrcrayfish.vehicle.client.render.AbstractVehicleRenderer;
import com.mrcrayfish.vehicle.client.render.VehicleRenderRegistry;
import com.mrcrayfish.vehicle.common.VehicleRegistry;
import com.mrcrayfish.vehicle.common.entity.PartPosition;
import com.mrcrayfish.vehicle.entity.EngineTier;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.init.ModItems;
import com.mrcrayfish.vehicle.item.EngineItem;
import com.mrcrayfish.vehicle.item.VehicleCrateItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a turning 3D preview of the vehicle inside a vehicle crate in its tooltip. Each vehicle is
 * measured once, then centred on its turning axis and scaled so it fits in the preview at every
 * angle, whatever its size.
 */
public class VehicleCrateTooltipComponent implements ClientTooltipComponent
{
    private static final int WIDTH = 120;
    private static final int HEIGHT = 80;
    private static final float MARGIN = 6.0F;
    private static final float TILT = 15.0F;
    private static final float MAX_SCALE = 40.0F;

    /* One renderer per vehicle, so the settings of a crate do not change the vehicles in the world */
    private static final Map<EntityType<?>, AbstractVehicleRenderer<?>> RENDERERS = new HashMap<>();
    private static final Map<EntityType<?>, Bounds> BOUNDS = new HashMap<>();

    private final VehicleCrateItem.Tooltip tooltip;

    public VehicleCrateTooltipComponent(VehicleCrateItem.Tooltip tooltip)
    {
        this.tooltip = tooltip;
    }

    /**
     * Clears the cached renderers and sizes, for example when the resources are reloaded
     */
    public static void clearCache()
    {
        RENDERERS.clear();
        BOUNDS.clear();
    }

    @Override
    public int getHeight()
    {
        return this.getType() != null ? HEIGHT : 0;
    }

    @Override
    public int getWidth(Font font)
    {
        return this.getType() != null ? WIDTH : 0;
    }

    private EntityType<?> getType()
    {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(this.tooltip.vehicle());
        return type != null && VehicleRenderRegistry.getRenderer(type) != null ? type : null;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics)
    {
        EntityType<?> type = this.getType();
        if(type == null)
            return;

        AbstractVehicleRenderer<?> renderer = RENDERERS.computeIfAbsent(type, VehicleRenderRegistry::getRendererFunction);
        if(renderer == null)
            return;

        VehicleProperties properties = VehicleProperties.get(type);
        this.applyCrateSettings(renderer, properties);

        Bounds bounds = BOUNDS.computeIfAbsent(type, t -> Bounds.measure(renderer, properties));
        float cos = Mth.cos(TILT * Mth.DEG_TO_RAD);
        float sin = Mth.sin(TILT * Mth.DEG_TO_RAD);
        // While it turns, the vehicle stays within its radius around the turning axis; the tilt of
        // the view adds part of that radius to its height
        float width = bounds.radius * 2.0F;
        float height = (bounds.maxY - bounds.minY) * cos + bounds.radius * 2.0F * sin;
        float scale = Math.min(MAX_SCALE, Math.min((WIDTH - MARGIN * 2) / Math.max(width, 0.01F), (HEIGHT - MARGIN * 2) / Math.max(height, 0.01F)));
        float centreY = (bounds.maxY + bounds.minY) / 2.0F * cos;

        float partialTicks = Minecraft.getInstance().getFrameTime();
        float rotation = (Util.getMillis() % 7200L) / 20.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        // The GUI y axis points down, so the centre of the vehicle goes up by centreY
        pose.translate(x + WIDTH / 2.0F, y + HEIGHT / 2.0F + centreY * scale, 100.0F);
        pose.mulPoseMatrix(new Matrix4f().scaling(-scale, -scale, -scale));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-TILT));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rotation));
        pose.translate(-bounds.centreX, 0, -bounds.centreZ);
        applyDisplayPosition(pose, properties);

        Lighting.setupForEntityInInventory();
        renderer.setupTransformsAndRender(null, pose, graphics.bufferSource(), partialTicks, 15728880);
        graphics.flush();
        Lighting.setupFor3DItems();

        pose.popPose();
    }

    /**
     * The transform of the vehicle in the workstation (scale, rotation and offset of its model)
     */
    private static void applyDisplayPosition(PoseStack pose, VehicleProperties properties)
    {
        PartPosition position = properties.getDisplayPosition();
        pose.scale((float) position.getScale(), (float) position.getScale(), (float) position.getScale());
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) position.getRotX()));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) position.getRotY()));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) position.getRotZ()));
        pose.translate(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Gives the renderer the color, engine and wheels the vehicle will have when the crate opens.
     * Follows the rules of the vehicle crate block entity.
     */
    private void applyCrateSettings(AbstractVehicleRenderer<?> renderer, VehicleProperties properties)
    {
        if(properties.isColored())
        {
            renderer.setColor(this.tooltip.color() != null ? this.tooltip.color() : com.mrcrayfish.vehicle.entity.VehicleEntity.DYE_TO_COLOR[0]);
        }

        if(renderer instanceof AbstractPoweredRenderer<?> poweredRenderer)
        {
            ItemStack engine = this.tooltip.engine();
            if(engine.isEmpty() && this.tooltip.creative())
            {
                EngineItem engineItem = VehicleRegistry.getEngineItem(properties.getEngineType(), EngineTier.IRON);
                engine = engineItem != null ? new ItemStack(engineItem) : ItemStack.EMPTY;
            }
            poweredRenderer.setEngineStack(engine);

            ItemStack wheel = this.tooltip.wheel();
            if(wheel.isEmpty())
            {
                wheel = new ItemStack(ModItems.STANDARD_WHEEL.get());
            }
            poweredRenderer.setWheelStack(wheel);
        }
    }

    /**
     * The size of a vehicle as drawn in the preview, before it turns: the centre of its footprint,
     * its radius around that centre and its height.
     */
    private record Bounds(float centreX, float centreZ, float radius, float minY, float maxY)
    {
        /**
         * Draws the vehicle once into a buffer that only records the positions of its vertices
         */
        static Bounds measure(AbstractVehicleRenderer<?> renderer, VehicleProperties properties)
        {
            VertexRecorder recorder = new VertexRecorder();
            PoseStack pose = new PoseStack();
            applyDisplayPosition(pose, properties);
            try
            {
                renderer.setupTransformsAndRender(null, pose, renderType -> recorder, 0.0F, 15728880);
            }
            catch(Exception e)
            {
                // A renderer that cannot be measured keeps the size of a block
                return new Bounds(0, 0, 0.5F, 0, 1);
            }
            if(recorder.count == 0)
            {
                return new Bounds(0, 0, 0.5F, 0, 1);
            }
            float centreX = (recorder.minX + recorder.maxX) / 2.0F;
            float centreZ = (recorder.minZ + recorder.maxZ) / 2.0F;
            return new Bounds(centreX, centreZ, (float) Math.sqrt(recorder.maxRadiusSq(centreX, centreZ)), recorder.minY, recorder.maxY);
        }
    }

    /**
     * A vertex consumer that keeps the extent of the vertices it receives and draws nothing
     */
    private static class VertexRecorder implements VertexConsumer
    {
        private final java.util.List<float[]> points = new java.util.ArrayList<>();
        private int count;
        private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        private float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        @Override
        public VertexConsumer vertex(double x, double y, double z)
        {
            float fx = (float) x, fy = (float) y, fz = (float) z;
            this.points.add(new float[]{fx, fz});
            this.count++;
            this.minX = Math.min(this.minX, fx);
            this.minY = Math.min(this.minY, fy);
            this.minZ = Math.min(this.minZ, fz);
            this.maxX = Math.max(this.maxX, fx);
            this.maxY = Math.max(this.maxY, fy);
            this.maxZ = Math.max(this.maxZ, fz);
            return this;
        }

        float maxRadiusSq(float centreX, float centreZ)
        {
            float max = 0;
            for(float[] point : this.points)
            {
                float dx = point[0] - centreX;
                float dz = point[1] - centreZ;
                max = Math.max(max, dx * dx + dz * dz);
            }
            return max;
        }

        @Override public VertexConsumer color(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() {}
        @Override public void defaultColor(int red, int green, int blue, int alpha) {}
        @Override public void unsetDefaultColor() {}
    }
}
