package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Sent by the driver's client: where the turret and the gun must point (turret yaw relative to the
 * hull and gun elevation, in degrees) and the point under the crosshair. The client works them out
 * from what is under its crosshair, with the drop of the shell, which only the client can see
 * (camera position). The light shells are aimed at the point from their launch tubes.
 */
public class MessageTankAim implements IMessage<MessageTankAim>
{
	private float turretYaw;
	private float gunPitch;
	private Vec3 target = Vec3.ZERO;

	public MessageTankAim() {}

	public MessageTankAim(float turretYaw, float gunPitch, Vec3 target)
	{
		this.turretYaw = turretYaw;
		this.gunPitch = gunPitch;
		this.target = target;
	}

	@Override
	public void encode(MessageTankAim message, FriendlyByteBuf buffer)
	{
		buffer.writeFloat(message.turretYaw);
		buffer.writeFloat(message.gunPitch);
		buffer.writeDouble(message.target.x);
		buffer.writeDouble(message.target.y);
		buffer.writeDouble(message.target.z);
	}

	@Override
	public MessageTankAim decode(FriendlyByteBuf buffer)
	{
		return new MessageTankAim(buffer.readFloat(), buffer.readFloat(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
	}

	@Override
	public void handle(MessageTankAim message, Supplier<NetworkEvent.Context> supplier)
	{
		supplier.get().enqueueWork(() ->
		{
			ServerPlayer player = supplier.get().getSender();
			if(player != null && player.getVehicle() instanceof TankEntity tank && tank.getControllingPassenger() == player)
			{
				tank.setAim(message.turretYaw, message.gunPitch, message.target);
			}
		});
		supplier.get().setPacketHandled(true);
	}
}
