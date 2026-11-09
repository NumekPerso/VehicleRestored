package com.mrcrayfish.vehicle.client;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.common.data.SyncedPlayerData;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import com.mrcrayfish.vehicle.init.ModDataKeys;
import com.mrcrayfish.vehicle.item.JerryCanItem;
import com.mrcrayfish.vehicle.network.PacketHandler;
import com.mrcrayfish.vehicle.network.message.MessageFuelVehicle;
import com.mrcrayfish.vehicle.tileentity.GasPumpTankTileEntity;
import com.mrcrayfish.vehicle.tileentity.GasPumpTileEntity;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

/**
 * Author: MrCrayfish
 */
public interface RayTraceFunction
{
    InteractionHand apply(EntityRayTracer rayTracer, EntityRayTracer.RayTraceResultRotated result, Player player);

    /**
     * Whether the right mouse button is physically held down. The "use" key mapping cannot be used:
     * the ray tracer cancels the right click on the fuel port before Minecraft updates the key
     * mappings, so keyUse never becomes pressed while refueling. Same check as the original mod.
     */
    static boolean isRightClicking()
    {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    /**
     * Checks if fuel can be transferred from a jerry can to a powered vehicle, and sends a packet to do so every other tick, if it can
     *
     * @return whether or not fueling can continue
     */
    RayTraceFunction FUNCTION_FUELING = (rayTracer, result, player) ->
    {
        Entity entity = result.getEntity();
        if(!(entity instanceof PoweredVehicleEntity))
            return null;

        PoweredVehicleEntity poweredVehicle = (PoweredVehicleEntity) entity;
        if(!poweredVehicle.requiresFuel() || poweredVehicle.getCurrentFuel() >= poweredVehicle.getFuelCapacity())
            return null;

        gasPump: if(SyncedPlayerData.instance().get(player, ModDataKeys.GAS_PUMP).isPresent() && isRightClicking())
        {
            BlockPos pos = SyncedPlayerData.instance().get(player, ModDataKeys.GAS_PUMP).get();
            BlockEntity tileEntity = player.level().getBlockEntity(pos);
            if(!(tileEntity instanceof GasPumpTileEntity))
                break gasPump;

            tileEntity = player.level().getBlockEntity(pos.below());
            if(!(tileEntity instanceof GasPumpTankTileEntity))
                break gasPump;

            GasPumpTankTileEntity gasPumpTank = (GasPumpTankTileEntity) tileEntity;
            FluidTank tank = gasPumpTank.getFluidTank();
            FluidStack stack = tank.getFluid();
            if(stack.isEmpty() || !Config.SERVER.validFuels.get().contains(String.valueOf(ForgeRegistries.FLUIDS.getKey(stack.getFluid()))))
                break gasPump;

            if(rayTracer.getContinuousInteractionTickCounter() % 2 == 0)
            {
                PacketHandler.instance.sendToServer(new MessageFuelVehicle(result.getEntity().getId(), InteractionHand.MAIN_HAND));
            }
            return InteractionHand.MAIN_HAND;
        }

        for(InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if(stack.isEmpty() || !(stack.getItem() instanceof JerryCanItem) || !isRightClicking())
                continue;

            Optional<IFluidHandlerItem> optional = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
            if(!optional.isPresent())
                continue;

            IFluidHandlerItem handler = optional.get();
            FluidStack fluidStack = handler.getFluidInTank(0);
            if(fluidStack.isEmpty() || !Config.SERVER.validFuels.get().contains(String.valueOf(ForgeRegistries.FLUIDS.getKey(fluidStack.getFluid()))))
                continue;

            if(rayTracer.getContinuousInteractionTickCounter() % 2 == 0)
            {
                PacketHandler.instance.sendToServer(new MessageFuelVehicle(entity.getId(), hand));
            }
            return hand;
        }
        return null;
    };
}
