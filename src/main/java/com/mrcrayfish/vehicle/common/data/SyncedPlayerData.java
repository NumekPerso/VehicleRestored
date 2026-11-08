package com.mrcrayfish.vehicle.common.data;

import com.mrcrayfish.vehicle.network.PacketHandler;
import com.mrcrayfish.vehicle.network.message.MessageSyncPlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stores values on players and syncs them to the clients that can see these players. The values
 * are not saved: they only last while the player is loaded.
 *
 * Replaces the synced player data of the Obfuscate library, which the original mod used.
 */
public class SyncedPlayerData
{
    private static final SyncedPlayerData INSTANCE = new SyncedPlayerData();

    private final Map<ResourceLocation, SyncedDataKey<?>> keys = new LinkedHashMap<>();
    private final Map<Player, Map<SyncedDataKey<?>, Object>> data = new WeakHashMap<>();

    public static SyncedPlayerData instance()
    {
        return INSTANCE;
    }

    public void registerKey(SyncedDataKey<?> key)
    {
        this.keys.put(key.getId(), key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Player player, SyncedDataKey<T> key)
    {
        synchronized(this.data)
        {
            Map<SyncedDataKey<?>, Object> values = this.data.get(player);
            if(values != null && values.containsKey(key))
            {
                return (T) values.get(key);
            }
        }
        return key.getDefaultValue();
    }

    public <T> void set(Player player, SyncedDataKey<T> key, T value)
    {
        synchronized(this.data)
        {
            this.data.computeIfAbsent(player, p -> new HashMap<>()).put(key, value);
        }
        if(player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null)
        {
            MessageSyncPlayerData message = new MessageSyncPlayerData(player.getId(), List.of(new Entry<>(key, value)));
            PacketHandler.instance.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), message);
        }
    }

    private List<Entry<?>> getEntries(Player player)
    {
        List<Entry<?>> entries = new ArrayList<>();
        synchronized(this.data)
        {
            Map<SyncedDataKey<?>, Object> values = this.data.get(player);
            if(values != null)
            {
                values.forEach((key, value) -> entries.add(Entry.unchecked(key, value)));
            }
        }
        return entries;
    }

    private void sendAll(Player target, ServerPlayer receiver)
    {
        List<Entry<?>> entries = this.getEntries(target);
        if(!entries.isEmpty())
        {
            PacketHandler.instance.send(PacketDistributor.PLAYER.with(() -> receiver), new MessageSyncPlayerData(target.getId(), entries));
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event)
    {
        Entity target = event.getTarget();
        if(target instanceof Player player && event.getEntity() instanceof ServerPlayer receiver)
        {
            this.sendAll(player, receiver);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if(event.getEntity() instanceof ServerPlayer player)
        {
            this.sendAll(player, player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if(event.getEntity() instanceof ServerPlayer player)
        {
            this.sendAll(player, player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event)
    {
        Player original = event.getOriginal();
        Player player = event.getEntity();
        List<Entry<?>> entries = this.getEntries(original);
        synchronized(this.data)
        {
            Map<SyncedDataKey<?>, Object> values = this.data.computeIfAbsent(player, p -> new HashMap<>());
            for(Entry<?> entry : entries)
            {
                if(!event.isWasDeath() || !entry.key().isResetOnDeath())
                {
                    values.put(entry.key(), entry.value());
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        synchronized(this.data)
        {
            this.data.remove(event.getEntity());
        }
    }

    /**
     * Called on the client when a sync message arrives
     */
    public void receive(Player player, List<Entry<?>> entries)
    {
        synchronized(this.data)
        {
            Map<SyncedDataKey<?>, Object> values = this.data.computeIfAbsent(player, p -> new HashMap<>());
            entries.forEach(entry -> values.put(entry.key(), entry.value()));
        }
    }

    public record Entry<T>(SyncedDataKey<T> key, T value)
    {
        @SuppressWarnings("unchecked")
        static <T> Entry<T> unchecked(SyncedDataKey<T> key, Object value)
        {
            return new Entry<>(key, (T) value);
        }

        public void write(FriendlyByteBuf buffer)
        {
            buffer.writeResourceLocation(this.key.getId());
            this.key.getSerializer().write(buffer, this.value);
        }

        public static Entry<?> read(FriendlyByteBuf buffer)
        {
            ResourceLocation id = buffer.readResourceLocation();
            SyncedDataKey<?> key = INSTANCE.keys.get(id);
            if(key == null)
            {
                throw new IllegalStateException("Unknown synced data key: " + id);
            }
            return readValue(key, buffer);
        }

        private static <T> Entry<T> readValue(SyncedDataKey<T> key, FriendlyByteBuf buffer)
        {
            return new Entry<>(key, key.getSerializer().read(buffer));
        }
    }
}
