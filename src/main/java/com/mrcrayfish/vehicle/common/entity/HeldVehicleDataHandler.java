package com.mrcrayfish.vehicle.common.entity;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.network.PacketHandler;
import com.mrcrayfish.vehicle.network.message.MessageSyncHeldVehicle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Author: MrCrayfish
 */
public class HeldVehicleDataHandler
{
    public static final Capability<IHeldVehicle> CAPABILITY_HELD_VEHICLE = CapabilityManager.get(new CapabilityToken<>(){});

    public static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        event.register(IHeldVehicle.class);
        MinecraftForge.EVENT_BUS.register(new HeldVehicleDataHandler());
    }

    public static boolean isHoldingVehicle(Player player)
    {
        IHeldVehicle handler = getHandler(player);
        if(handler != null)
        {
            return !handler.getVehicleTag().isEmpty();
        }
        return false;
    }

    public static CompoundTag getHeldVehicle(Player player)
    {
        IHeldVehicle handler = getHandler(player);
        if(handler != null)
        {
            return handler.getVehicleTag();
        }
        return new CompoundTag();
    }

    public static void setHeldVehicle(Player player, CompoundTag vehicleTag)
    {
        IHeldVehicle handler = getHandler(player);
        if(handler != null)
        {
            handler.setVehicleTag(vehicleTag);
        }
        if(!player.level().isClientSide)
        {
            PacketHandler.instance.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new MessageSyncHeldVehicle(player.getId(), vehicleTag));
        }
    }

    @Nullable
    public static IHeldVehicle getHandler(Player player)
    {
        return player.getCapability(CAPABILITY_HELD_VEHICLE, Direction.DOWN).orElse(null);
    }

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            event.addCapability(new ResourceLocation(Reference.MOD_ID, "held_vehicle"), new Provider());
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event)
    {
        if(event.isWasDeath())
            return;

        event.getOriginal().reviveCaps();
        CompoundTag vehicleTag = getHeldVehicle(event.getOriginal());
        event.getOriginal().invalidateCaps();
        if(!vehicleTag.isEmpty())
        {
            setHeldVehicle(event.getEntity(), vehicleTag);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event)
    {
        if(event.getTarget() instanceof Player)
        {
            Player player = (Player) event.getTarget();
            CompoundTag vehicleTag = getHeldVehicle(player);
            PacketHandler.instance.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new MessageSyncHeldVehicle(player.getId(), vehicleTag));
        }
    }

    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinLevelEvent event)
    {
        Entity entity = event.getEntity();
        if(entity instanceof Player && !event.getLevel().isClientSide)
        {
            Player player = (Player) entity;
            CompoundTag vehicleTag = getHeldVehicle(player);
            PacketHandler.instance.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageSyncHeldVehicle(player.getId(), vehicleTag));
        }
    }

    public interface IHeldVehicle
    {
        void setVehicleTag(CompoundTag tagCompound);
        CompoundTag getVehicleTag();
    }

    public static class HeldVehicle implements IHeldVehicle
    {
        private CompoundTag compound = new CompoundTag();

        @Override
        public void setVehicleTag(CompoundTag tagCompound)
        {
            this.compound = tagCompound;
        }

        @Override
        public CompoundTag getVehicleTag()
        {
            return compound;
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag>
    {
        final IHeldVehicle INSTANCE = new HeldVehicle();

        @Override
        public CompoundTag serializeNBT()
        {
            return INSTANCE.getVehicleTag();
        }

        @Override
        public void deserializeNBT(CompoundTag compound)
        {
            INSTANCE.setVehicleTag(compound);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
        {
            return CAPABILITY_HELD_VEHICLE.orEmpty(cap, LazyOptional.of(() -> INSTANCE));
        }
    }
}
