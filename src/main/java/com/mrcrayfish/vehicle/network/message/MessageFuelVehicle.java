package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

public class MessageFuelVehicle implements IMessage<MessageFuelVehicle>
{
    protected int entityId;
    private InteractionHand hand;

    public MessageFuelVehicle()
    {
    }

    public MessageFuelVehicle(int entityId, InteractionHand hand)
    {
        this.entityId = entityId;
        this.hand = hand;
    }

    @Override
    public void encode(MessageFuelVehicle message, FriendlyByteBuf buffer)
    {
        buffer.writeInt(message.entityId);
        buffer.writeEnum(message.hand);
    }

    @Override
    public MessageFuelVehicle decode(FriendlyByteBuf buffer)
    {
        return new MessageFuelVehicle(buffer.readInt(), buffer.readEnum(InteractionHand.class));
    }

    @Override
    public void handle(MessageFuelVehicle message, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() -> {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                Entity targetEntity = player.level().getEntity(message.entityId);
                if(targetEntity instanceof PoweredVehicleEntity)
                {
                    ((PoweredVehicleEntity) targetEntity).fuelVehicle(player, message.hand);
                }
            }
        });
        supplier.get().setPacketHandled(true);
    }
}