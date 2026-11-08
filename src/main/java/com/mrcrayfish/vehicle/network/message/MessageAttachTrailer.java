package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.common.data.SyncedPlayerData;
import com.mrcrayfish.vehicle.entity.TrailerEntity;
import com.mrcrayfish.vehicle.init.ModDataKeys;
import com.mrcrayfish.vehicle.util.CommonUtils;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

/**
 * Author: MrCrayfish
 */
public class MessageAttachTrailer implements IMessage<MessageAttachTrailer>
{
    private int trailerId;
    private int entityId;

    public MessageAttachTrailer() {}

    public MessageAttachTrailer(int trailerId, int entityId)
    {
        this.trailerId = trailerId;
        this.entityId = entityId;
    }

    @Override
    public void encode(MessageAttachTrailer message, FriendlyByteBuf buffer)
    {
        buffer.writeInt(message.trailerId);
        buffer.writeInt(message.entityId);
    }

    @Override
    public MessageAttachTrailer decode(FriendlyByteBuf buffer)
    {
        return new MessageAttachTrailer(buffer.readInt(), buffer.readInt());
    }

    @Override
    public void handle(MessageAttachTrailer message, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
        {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                Level world = player.level();
                Entity trailerEntity = world.getEntity(message.trailerId);
                // Both ids come from the client: a player only grabs a trailer within reach, and
                // only for himself. The entity id is kept in the message for compatibility.
                if(trailerEntity instanceof TrailerEntity trailer && message.entityId == player.getId() && player.getVehicle() == null && CommonUtils.isWithinInteractionReach(player, trailer))
                {
                    trailer.setPullingEntity(player);
                    SyncedPlayerData.instance().set(player, ModDataKeys.TRAILER, trailer.getId());
                }
            }
        });
        supplier.get().setPacketHandled(true);
    }
}
