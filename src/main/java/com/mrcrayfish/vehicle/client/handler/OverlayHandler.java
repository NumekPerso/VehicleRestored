package com.mrcrayfish.vehicle.client.handler;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.awt.*;
import java.text.DecimalFormat;

/**
 * Renders the speedometer and fuel level while driving a powered vehicle. In 1.16.5 this was drawn
 * at the end of the render tick; it is now a GUI overlay drawn above all the others.
 *
 * Author: MrCrayfish
 */
public class OverlayHandler
{
    public static final IGuiOverlay SPEEDOMETER = OverlayHandler::render;

    public static void register(RegisterGuiOverlaysEvent event)
    {
        event.registerAboveAll("speedometer", SPEEDOMETER);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight)
    {
        if(!Config.CLIENT.enabledSpeedometer.get())
            return;

        Minecraft mc = Minecraft.getInstance();
        if(!mc.isWindowActive() || mc.options.hideGui)
            return;

        Player player = mc.player;
        if(player == null)
            return;

        Entity entity = player.getVehicle();
        if(!(entity instanceof PoweredVehicleEntity))
            return;

        PoweredVehicleEntity vehicle = (PoweredVehicleEntity) entity;
        String speed = new DecimalFormat("0.0").format(vehicle.getKilometersPreHour());
        graphics.drawString(mc.font, ChatFormatting.BOLD + "BPS: " + ChatFormatting.YELLOW + speed, 10, 10, Color.WHITE.getRGB(), true);

        if(vehicle.requiresFuel())
        {
            DecimalFormat format = new DecimalFormat("0.0");
            String fuel = format.format(vehicle.getCurrentFuel()) + "/" + format.format(vehicle.getFuelCapacity());
            graphics.drawString(mc.font, ChatFormatting.BOLD + "Fuel: " + ChatFormatting.YELLOW + fuel, 10, 25, Color.WHITE.getRGB(), true);
        }
    }
}
