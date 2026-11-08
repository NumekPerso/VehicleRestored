package com.mrcrayfish.vehicle.client.tooltip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a turning 3D preview of the vehicle inside a vehicle crate in its tooltip. The vehicle is
 * drawn the same way as in the workstation.
 */
public class VehicleCrateTooltipComponent implements ClientTooltipComponent
{
    private static final int WIDTH = 120;
    private static final int HEIGHT = 80;
    private static final float SCALE = 22.0F;

    /* One renderer per vehicle, so the settings of a crate do not change the vehicles in the world */
    private static final Map<EntityType<?>, AbstractVehicleRenderer<?>> RENDERERS = new HashMap<>();

    private final VehicleCrateItem.Tooltip tooltip;

    public VehicleCrateTooltipComponent(VehicleCrateItem.Tooltip tooltip)
    {
        this.tooltip = tooltip;
    }

    /**
     * Clears the cached renderers, for example when the resources are reloaded
     */
    public static void clearCache()
    {
        RENDERERS.clear();
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

        float partialTicks = Minecraft.getInstance().getFrameTime();
        float rotation = (Util.getMillis() % 7200L) / 20.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + WIDTH / 2.0F, y + HEIGHT * 0.7F, 100.0F);
        // Same orientation as the workstation preview: the GUI y axis points down
        pose.mulPoseMatrix(new Matrix4f().scaling(-SCALE, -SCALE, -SCALE));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-15F));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rotation));

        PartPosition position = properties.getDisplayPosition();
        pose.scale((float) position.getScale(), (float) position.getScale(), (float) position.getScale());
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) position.getRotX()));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) position.getRotY()));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) position.getRotZ()));
        pose.translate(position.getX(), position.getY(), position.getZ());

        Lighting.setupForEntityInInventory();
        renderer.setupTransformsAndRender(null, pose, graphics.bufferSource(), partialTicks, 15728880);
        graphics.flush();
        Lighting.setupFor3DItems();

        pose.popPose();
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
}
