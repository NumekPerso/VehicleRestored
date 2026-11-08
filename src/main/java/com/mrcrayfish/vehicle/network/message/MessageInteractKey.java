package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import com.mrcrayfish.vehicle.init.ModItems;
import com.mrcrayfish.vehicle.util.CommonUtils;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Author: MrCrayfish
 */
public class MessageInteractKey implements IMessage<MessageInteractKey>
{
    private int entityId;

    public MessageInteractKey()
    {
    }

    public MessageInteractKey(Entity targetEntity)
    {
        this.entityId = targetEntity.getId();
    }

    private MessageInteractKey(int entityId)
    {
        this.entityId = entityId;
    }

    @Override
    public void encode(MessageInteractKey message, FriendlyByteBuf buffer)
    {
        buffer.writeInt(message.entityId);
    }

    @Override
    public MessageInteractKey decode(FriendlyByteBuf buffer)
    {
        return new MessageInteractKey(buffer.readInt());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void handle(MessageInteractKey message, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
        {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                Entity targetEntity = player.level().getEntity(message.entityId);
                // The id comes from the client: only a vehicle the player could have clicked
                if(targetEntity instanceof PoweredVehicleEntity && CommonUtils.isWithinInteractionReach(player, targetEntity))
                {
                    PoweredVehicleEntity poweredVehicle = (PoweredVehicleEntity) targetEntity;
                    if(poweredVehicle.isKeyNeeded())
                    {
                        ItemStack stack = player.getMainHandItem();
                        if(!stack.isEmpty() && stack.getItem() == ModItems.WRENCH.get())
                        {
                            if(poweredVehicle.isOwner(player))
                            {
                                poweredVehicle.ejectKey();
                                poweredVehicle.setKeyNeeded(false);
                                CommonUtils.sendInfoMessage(player, "vehicle_restored.status.key_removed");
                            }
                            else
                            {
                                CommonUtils.sendInfoMessage(player, "vehicle_restored.status.invalid_owner");
                            }
                            return;
                        }
                        if(poweredVehicle.getKeyStack().isEmpty())
                        {
                            if(!stack.isEmpty() && stack.getItem() == ModItems.KEY.get())
                            {
                                // A blank key has no id: getUUID would throw
                                CompoundTag keyTag = stack.getTag();
                                UUID keyUuid = keyTag != null && keyTag.hasUUID("VehicleId") ? keyTag.getUUID("VehicleId") : null;
                                if(poweredVehicle.getUUID().equals(keyUuid))
                                {
                                    poweredVehicle.setKeyStack(stack.copy());
                                    player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                                }
                                else
                                {
                                    CommonUtils.sendInfoMessage(player, "vehicle_restored.status.key_invalid");
                                }
                            }
                        }
                        else if(poweredVehicle.isOwner(player) || poweredVehicle.hasPassenger(player))
                        {
                            // Anyone seated can already drive it, so taking the key out gives them
                            // nothing more. Someone outside who is not the owner must not stop the vehicle.
                            poweredVehicle.ejectKey();
                        }
                        else
                        {
                            CommonUtils.sendInfoMessage(player, "vehicle_restored.status.invalid_owner");
                        }
                    }
                }
            }
        });
        supplier.get().setPacketHandled(true);
    }
}
