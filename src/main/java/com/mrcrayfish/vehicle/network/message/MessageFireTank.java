package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import com.mrcrayfish.vehicle.entity.vehicle.TankWeapon;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sent when the driver of a tank fires the selected weapon. The server checks that the player
 * drives the tank and that the weapon is ready before firing.
 */
public class MessageFireTank implements IMessage<MessageFireTank>
{
	private TankWeapon weapon = TankWeapon.CANNON;

	public MessageFireTank() {}

	public MessageFireTank(TankWeapon weapon)
	{
		this.weapon = weapon;
	}

	@Override
	public void encode(MessageFireTank message, FriendlyByteBuf buffer)
	{
		buffer.writeByte(message.weapon.ordinal());
	}

	@Override
	public MessageFireTank decode(FriendlyByteBuf buffer)
	{
		return new MessageFireTank(TankWeapon.byId(buffer.readByte()));
	}

	@Override
	public void handle(MessageFireTank message, Supplier<NetworkEvent.Context> supplier)
	{
		supplier.get().enqueueWork(() ->
		{
			ServerPlayer player = supplier.get().getSender();
			if(player != null && player.getVehicle() instanceof TankEntity tank)
			{
				tank.fire(player, message.weapon);
			}
		});
		supplier.get().setPacketHandled(true);
	}
}
