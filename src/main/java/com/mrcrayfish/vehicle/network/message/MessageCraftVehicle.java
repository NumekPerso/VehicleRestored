package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.util.CommonUtils;
import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.block.VehicleCrateBlock;
import com.mrcrayfish.vehicle.common.VehicleRegistry;
import com.mrcrayfish.vehicle.crafting.WorkstationRecipe;
import com.mrcrayfish.vehicle.crafting.WorkstationRecipes;
import com.mrcrayfish.vehicle.entity.EngineType;
import com.mrcrayfish.vehicle.entity.IEngineType;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.inventory.container.WorkstationContainer;
import com.mrcrayfish.vehicle.item.EngineItem;
import com.mrcrayfish.vehicle.item.WheelItem;
import com.mrcrayfish.vehicle.tileentity.WorkstationTileEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Author: MrCrayfish
 */
public class MessageCraftVehicle implements IMessage<MessageCraftVehicle>
{
    private String vehicleId;
    private BlockPos pos;

    public MessageCraftVehicle() {}

    public MessageCraftVehicle(String vehicleId, BlockPos pos)
    {
        this.vehicleId = vehicleId;
        this.pos = pos;
    }

    @Override
    public void encode(MessageCraftVehicle message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.vehicleId, 128);
        buffer.writeBlockPos(message.pos);
    }

    @Override
    public MessageCraftVehicle decode(FriendlyByteBuf buffer)
    {
        return new MessageCraftVehicle(buffer.readUtf(128), buffer.readBlockPos());
    }

    @Override
    public void handle(MessageCraftVehicle message, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
        {
            ServerPlayer player = supplier.get().getSender();
            if(player == null)
                return;

            Level world = player.level();
            if(!(player.containerMenu instanceof WorkstationContainer))
                return;

            WorkstationContainer workstation = (WorkstationContainer) player.containerMenu;
            if(!workstation.getPos().equals(message.pos) || !workstation.stillValid(player))
                return;

            ResourceLocation entityId = new ResourceLocation(message.vehicleId);
            if(Config.SERVER.disabledVehicles.get().contains(entityId.toString()))
                return;

            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
            if(entityType == null)
                return;

            if(!VehicleRegistry.getRegisteredVehicleTypes().contains(entityType))
                return;

            WorkstationRecipe recipe = WorkstationRecipes.getRecipe(entityType, world);
            if(recipe == null || !recipe.hasMaterials(player))
                return;

            Entity entity = entityType.create(world);
            if(!(entity instanceof VehicleEntity))
                return;

            IEngineType engineType = EngineType.NONE;
            VehicleEntity vehicle = (VehicleEntity) entity;
            if(vehicle instanceof PoweredVehicleEntity)
            {
                PoweredVehicleEntity entityPoweredVehicle = (PoweredVehicleEntity) entity;
                engineType = entityPoweredVehicle.getProperties().getEngineType();

                WorkstationTileEntity workstationTileEntity = workstation.getTileEntity();
                // A vehicle without engine type needs no engine (it was required but never used)
                if(engineType != EngineType.NONE)
                {
                    ItemStack workstationEngine = workstationTileEntity.getItem(1);
                    if(workstationEngine.isEmpty() || !(workstationEngine.getItem() instanceof EngineItem))
                        return;

                    IEngineType engineType2 = ((EngineItem) workstationEngine.getItem()).getEngineType();
                    if(engineType != engineType2)
                        return;
                }

                if(entityPoweredVehicle.canChangeWheels())
                {
                    ItemStack wheel = workstationTileEntity.getInventory().get(2);
                    if(!(wheel.getItem() instanceof WheelItem))
                        return;
                }
            }

            /* At this point we have verified the crafting and can perform irreversible actions */

            recipe.consumeMaterials(player);

            WorkstationTileEntity workstationTileEntity = workstation.getTileEntity();

            /* Gets the color based on the dye */
            int color = VehicleEntity.DYE_TO_COLOR[0];
            if(vehicle.canBeColored())
            {
                ItemStack workstationDyeStack = workstationTileEntity.getInventory().get(0);
                if(workstationDyeStack.getItem() instanceof DyeItem)
                {
                    DyeItem dyeItem = (DyeItem) workstationDyeStack.getItem();
                    color = CommonUtils.getDyeColor(dyeItem.getDyeColor());
                    // One of each part per vehicle: a stack (put by a hopper before the slots were
                    // limited) used to vanish whole into the crate
                    workstationDyeStack.shrink(1);
                }
            }

            ItemStack engineStack = ItemStack.EMPTY;
            if(engineType != EngineType.NONE)
            {
                ItemStack workstationEngineStack = workstationTileEntity.getInventory().get(1);
                if(workstationEngineStack.getItem() instanceof EngineItem)
                {
                    engineStack = workstationEngineStack.split(1);
                }
            }

            ItemStack wheelStack = ItemStack.EMPTY;
            if(vehicle instanceof PoweredVehicleEntity && ((PoweredVehicleEntity) vehicle).canChangeWheels())
            {
                ItemStack workstationWheelStack = workstationTileEntity.getInventory().get(2);
                if(workstationWheelStack.getItem() instanceof WheelItem)
                {
                    wheelStack = workstationWheelStack.split(1);
                }
            }
            workstationTileEntity.setChanged();

            ItemStack stack = VehicleCrateBlock.create(entityId, color, engineStack, wheelStack);
            world.addFreshEntity(new ItemEntity(world, message.pos.getX() + 0.5, message.pos.getY() + 1.125, message.pos.getZ() + 0.5, stack));
        });
        supplier.get().setPacketHandled(true);
    }
}
