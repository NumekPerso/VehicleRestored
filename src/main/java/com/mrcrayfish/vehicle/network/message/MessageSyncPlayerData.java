package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.client.network.ClientPlayHandler;
import com.mrcrayfish.vehicle.common.data.SyncedPlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Syncs the values of {@link SyncedPlayerData} to the client
 */
public class MessageSyncPlayerData implements IMessage<MessageSyncPlayerData>
{
    private int entityId;
    private List<SyncedPlayerData.Entry<?>> entries;

    public MessageSyncPlayerData() {}

    public MessageSyncPlayerData(int entityId, List<SyncedPlayerData.Entry<?>> entries)
    {
        this.entityId = entityId;
        this.entries = entries;
    }

    @Override
    public void encode(MessageSyncPlayerData message, FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.entries.size());
        message.entries.forEach(entry -> entry.write(buffer));
    }

    @Override
    public MessageSyncPlayerData decode(FriendlyByteBuf buffer)
    {
        int entityId = buffer.readVarInt();
        int size = buffer.readVarInt();
        List<SyncedPlayerData.Entry<?>> entries = new ArrayList<>();
        for(int i = 0; i < size; i++)
        {
            entries.add(SyncedPlayerData.Entry.read(buffer));
        }
        return new MessageSyncPlayerData(entityId, entries);
    }

    @Override
    public void handle(MessageSyncPlayerData message, Supplier<NetworkEvent.Context> supplier)
    {
        IMessage.enqueueTask(supplier, () -> ClientPlayHandler.handleSyncPlayerData(message));
    }

    public int getEntityId()
    {
        return this.entityId;
    }

    public List<SyncedPlayerData.Entry<?>> getEntries()
    {
        return this.entries;
    }
}
