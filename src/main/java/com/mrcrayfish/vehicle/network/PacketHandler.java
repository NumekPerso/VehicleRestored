package com.mrcrayfish.vehicle.network;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.network.message.*;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler
{
    public static final String PROTOCOL_VERSION = "1";

    public static SimpleChannel instance;
    private static int nextId = 0;

    public static void register()
    {
        instance = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Reference.MOD_ID, "play"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .simpleChannel();

        // The order gives the ids: do not reorder, add new messages at the end
        register(MessageTurnDirection.class, new MessageTurnDirection(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageTurnAngle.class, new MessageTurnAngle(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageAccelerating.class, new MessageAccelerating(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageDrift.class, new MessageDrift(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageHorn.class, new MessageHorn(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageThrowVehicle.class, new MessageThrowVehicle(), NetworkDirection.PLAY_TO_SERVER);
        register(MessagePickupVehicle.class, new MessagePickupVehicle(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageFlaps.class, new MessageFlaps(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageAttachChest.class, new MessageAttachChest(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageAttachTrailer.class, new MessageAttachTrailer(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageFuelVehicle.class, new MessageFuelVehicle(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageInteractKey.class, new MessageInteractKey(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageAltitude.class, new MessageAltitude(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageCraftVehicle.class, new MessageCraftVehicle(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageHitchTrailer.class, new MessageHitchTrailer(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageSyncInventory.class, new MessageSyncInventory(), NetworkDirection.PLAY_TO_CLIENT);
        register(MessageOpenStorage.class, new MessageOpenStorage(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageTravelProperties.class, new MessageTravelProperties(), NetworkDirection.PLAY_TO_SERVER);
        register(MessagePower.class, new MessagePower(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageEntityFluid.class, new MessageEntityFluid(), NetworkDirection.PLAY_TO_CLIENT);
        register(MessageSyncPlayerSeat.class, new MessageSyncPlayerSeat(), NetworkDirection.PLAY_TO_CLIENT);
        register(MessageCycleSeats.class, new MessageCycleSeats(), NetworkDirection.PLAY_TO_SERVER);
        register(MessageSyncHeldVehicle.class, new MessageSyncHeldVehicle(), NetworkDirection.PLAY_TO_CLIENT);
        register(MessageSyncPlayerData.class, new MessageSyncPlayerData(), NetworkDirection.PLAY_TO_CLIENT);
    }

    /**
     * The direction is mandatory: without it a client could send to the server a message meant
     * for the clients, and the server would run its client side handler.
     */
    private static <T> void register(Class<T> clazz, IMessage<T> message, NetworkDirection direction)
    {
        instance.registerMessage(nextId++, clazz, message::encode, message::decode, message::handle, Optional.of(direction));
    }
}
