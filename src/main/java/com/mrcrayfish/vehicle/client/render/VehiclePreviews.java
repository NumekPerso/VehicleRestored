package com.mrcrayfish.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mrcrayfish.vehicle.common.VehicleRegistry;
import com.mrcrayfish.vehicle.common.entity.PartPosition;
import com.mrcrayfish.vehicle.entity.EngineTier;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.init.ModItems;
import com.mrcrayfish.vehicle.item.EngineItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * Vehicles drawn without an entity: in the tooltip of a vehicle crate and as the miniature built by
 * the advanced workshop. One renderer per vehicle (so these settings do not change the vehicles in
 * the world), and the size of each vehicle, measured once.
 */
public final class VehiclePreviews
{
    private static final Map<EntityType<?>, AbstractVehicleRenderer<?>> RENDERERS = new HashMap<>();
    private static final Map<EntityType<?>, Bounds> BOUNDS = new HashMap<>();

    private VehiclePreviews() {}

    /** Clears the cached renderers and sizes, for example when the resources are reloaded */
    public static void clearCache()
    {
        RENDERERS.clear();
        BOUNDS.clear();
    }

    @Nullable
    public static AbstractVehicleRenderer<?> getRenderer(EntityType<?> type)
    {
        return VehicleRenderRegistry.getRenderer(type) != null ? RENDERERS.computeIfAbsent(type, VehicleRenderRegistry::getRendererFunction) : null;
    }

    /** Size of the vehicle as drawn by its renderer with its display position applied */
    public static Bounds getBounds(EntityType<?> type, AbstractVehicleRenderer<?> renderer)
    {
        return BOUNDS.computeIfAbsent(type, t -> Bounds.measure(renderer, VehicleProperties.get(t)));
    }

    /** The transform of the vehicle in the workstation (scale, rotation and offset of its model) */
    public static void applyDisplayPosition(PoseStack pose, VehicleProperties properties)
    {
        PartPosition position = properties.getDisplayPosition();
        pose.scale((float) position.getScale(), (float) position.getScale(), (float) position.getScale());
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) position.getRotX()));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) position.getRotY()));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) position.getRotZ()));
        pose.translate(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Gives the renderer the color, engine and wheels to draw. An empty engine gives the iron engine of
     * the vehicle's type when `defaultEngine` is set, an empty wheel the standard wheel.
     */
    public static void prepare(AbstractVehicleRenderer<?> renderer, VehicleProperties properties, @Nullable Integer color, ItemStack engine, boolean defaultEngine, ItemStack wheel)
    {
        if(properties.isColored())
        {
            renderer.setColor(color != null ? color : VehicleEntity.DYE_TO_COLOR[0]);
        }
        if(renderer instanceof AbstractPoweredRenderer<?> poweredRenderer)
        {
            if(engine.isEmpty() && defaultEngine)
            {
                EngineItem engineItem = VehicleRegistry.getEngineItem(properties.getEngineType(), EngineTier.IRON);
                engine = engineItem != null ? new ItemStack(engineItem) : ItemStack.EMPTY;
            }
            poweredRenderer.setEngineStack(engine);
            poweredRenderer.setWheelStack(wheel.isEmpty() ? new ItemStack(ModItems.STANDARD_WHEEL.get()) : wheel);
        }
    }

    /**
     * The size of a vehicle as drawn, before it turns: the centre of its footprint, its radius around
     * that centre, its height, and its footprint (x and z extents)
     */
    public record Bounds(float centreX, float centreZ, float radius, float minY, float maxY, float minX, float maxX, float minZ, float maxZ)
    {
        /** Draws the vehicle once into a buffer that only records the positions of its vertices */
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
                return new Bounds(0, 0, 0.5F, 0, 1, -0.5F, 0.5F, -0.5F, 0.5F);
            }
            if(recorder.count == 0)
            {
                return new Bounds(0, 0, 0.5F, 0, 1, -0.5F, 0.5F, -0.5F, 0.5F);
            }
            float centreX = (recorder.minX + recorder.maxX) / 2.0F;
            float centreZ = (recorder.minZ + recorder.maxZ) / 2.0F;
            return new Bounds(centreX, centreZ, (float) Math.sqrt(recorder.maxRadiusSq(centreX, centreZ)), recorder.minY, recorder.maxY,
                    recorder.minX, recorder.maxX, recorder.minZ, recorder.maxZ);
        }
    }

    /** A vertex consumer that keeps the extent of the vertices it receives and draws nothing */
    private static class VertexRecorder implements VertexConsumer
    {
        private final List<float[]> points = new ArrayList<>();
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
