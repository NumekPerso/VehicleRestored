package com.mrcrayfish.vehicle.client.tooltip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.client.render.AbstractVehicleRenderer;
import com.mrcrayfish.vehicle.client.render.VehiclePreviews;
import com.mrcrayfish.vehicle.client.render.VehicleRenderRegistry;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.item.VehicleCrateItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

/**
 * Draws a turning 3D preview of the vehicle inside a vehicle crate in its tooltip. Each vehicle is
 * measured once, then centred on its turning axis and scaled so it fits in the preview at every
 * angle, whatever its size (see VehiclePreviews).
 */
public class VehicleCrateTooltipComponent implements ClientTooltipComponent
{
    private static final int WIDTH = 120;
    private static final int HEIGHT = 80;
    private static final float MARGIN = 6.0F;
    private static final float TILT = 15.0F;
    private static final float MAX_SCALE = 40.0F;

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
        VehiclePreviews.clearCache();
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

        AbstractVehicleRenderer<?> renderer = VehiclePreviews.getRenderer(type);
        if(renderer == null)
            return;

        VehicleProperties properties = VehicleProperties.get(type);
        // the color, engine and wheels the vehicle will have when the crate opens (rules of the crate block entity)
        VehiclePreviews.prepare(renderer, properties, this.tooltip.color(), this.tooltip.engine(), this.tooltip.creative(), this.tooltip.wheel());

        VehiclePreviews.Bounds bounds = VehiclePreviews.getBounds(type, renderer);
        float cos = Mth.cos(TILT * Mth.DEG_TO_RAD);
        float sin = Mth.sin(TILT * Mth.DEG_TO_RAD);
        // While it turns, the vehicle stays within its radius around the turning axis; the tilt of
        // the view adds part of that radius to its height
        float width = bounds.radius() * 2.0F;
        float height = (bounds.maxY() - bounds.minY()) * cos + bounds.radius() * 2.0F * sin;
        float scale = Math.min(MAX_SCALE, Math.min((WIDTH - MARGIN * 2) / Math.max(width, 0.01F), (HEIGHT - MARGIN * 2) / Math.max(height, 0.01F)));
        float centreY = (bounds.maxY() + bounds.minY()) / 2.0F * cos;

        float partialTicks = Minecraft.getInstance().getFrameTime();
        float rotation = (Util.getMillis() % 7200L) / 20.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        // The GUI y axis points down, so the centre of the vehicle goes up by centreY
        pose.translate(x + WIDTH / 2.0F, y + HEIGHT / 2.0F + centreY * scale, 100.0F);
        pose.mulPoseMatrix(new Matrix4f().scaling(-scale, -scale, -scale));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-TILT));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rotation));
        pose.translate(-bounds.centreX(), 0, -bounds.centreZ());
        VehiclePreviews.applyDisplayPosition(pose, properties);

        Lighting.setupForEntityInInventory();
        renderer.setupTransformsAndRender(null, pose, graphics.bufferSource(), partialTicks, 15728880);
        graphics.flush();
        Lighting.setupFor3DItems();

        pose.popPose();
    }
}
