package com.mrcrayfish.vehicle.entity;

import com.mrcrayfish.vehicle.client.particle.TankEffects;
import com.mrcrayfish.vehicle.init.ModEntities;
import com.mrcrayfish.vehicle.init.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;

/**
 * Machine gun bullet of the tank (coaxial 7.62 mm or .50 cal). Fast, a little drop, hurts what it
 * hits and leaves a tracer. It has no model: the tracer is drawn with particles.
 */
public class TankBulletEntity extends Projectile implements IEntityAdditionalSpawnData
{
	private static final EntityDataAccessor<Boolean> HEAVY = SynchedEntityData.defineId(TankBulletEntity.class, EntityDataSerializers.BOOLEAN);
	public static final double GRAVITY = 0.01;
	private static final int MAX_LIFE = 40;

	private int tankId = -1;
	private float damage = 4F;
	private int life;

	public TankBulletEntity(EntityType<? extends TankBulletEntity> type, Level level)
	{
		super(type, level);
	}

	public TankBulletEntity(Level level, Entity shooter, Entity tank, Vec3 position, Vec3 motion, float damage, boolean heavy)
	{
		this(ModEntities.TANK_BULLET.get(), level);
		this.setOwner(shooter);
		this.tankId = tank.getId();
		this.damage = damage;
		this.entityData.set(HEAVY, heavy);
		this.setPos(position);
		this.setDeltaMovement(motion);
		this.updateRotation();
	}

	@Override
	protected void defineSynchedData()
	{
		this.entityData.define(HEAVY, false);
	}

	/*
	 * The vanilla spawn and motion packets clamp the speed to 3.9 blocks per tick on each axis: the
	 * tracers of these fast bullets went the wrong way on the clients. The exact velocity is sent once
	 * with the spawn (and the clients fly the bullet themselves, see ModEntities: no velocity updates).
	 */
	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket()
	{
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void writeSpawnData(FriendlyByteBuf buffer)
	{
		Vec3 motion = this.getDeltaMovement();
		buffer.writeDouble(motion.x);
		buffer.writeDouble(motion.y);
		buffer.writeDouble(motion.z);
		buffer.writeInt(this.tankId);
	}

	@Override
	public void readSpawnData(FriendlyByteBuf buffer)
	{
		this.setDeltaMovement(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		this.tankId = buffer.readInt();
		this.updateRotation();
	}

	@Override
	public void tick()
	{
		super.tick();
		if(++this.life > MAX_LIFE)
		{
			this.discard();
			return;
		}
		HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		Vec3 from = this.position();
		Vec3 motion = this.getDeltaMovement();
		Vec3 to = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : from.add(motion);
		if(this.level().isClientSide())
		{
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TankEffects.bulletTracer(this.level(), from, to, this.entityData.get(HEAVY)));
		}
		if(hit.getType() != HitResult.Type.MISS)
		{
			this.onHit(hit);
			return;
		}
		this.setPos(to);
		this.setDeltaMovement(motion.add(0, -GRAVITY, 0));
		this.updateRotation();
	}

	@Override
	protected boolean canHitEntity(Entity target)
	{
		if(target.getId() == this.tankId)
			return false;
		Entity tank = this.level().getEntity(this.tankId);
		if(tank != null && target.getRootVehicle() == tank)
			return false;
		return super.canHitEntity(target) || target instanceof VehicleEntity;
	}

	@Override
	protected void onHitEntity(EntityHitResult result)
	{
		if(!this.level().isClientSide())
		{
			Entity owner = this.getOwner();
			result.getEntity().hurt(this.damageSources().thrown(this, owner), this.damage);
		}
	}

	@Override
	protected void onHit(HitResult result)
	{
		super.onHit(result);
		if(this.level() instanceof ServerLevel level)
		{
			// Sparks and dust where the bullet hits (the side that was hit for a block)
			Vec3 at = result.getLocation();
			Vec3 normal = result instanceof BlockHitResult blockHit ? Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()) : this.getDeltaMovement().normalize().scale(-1);
			for(ServerPlayer player : level.players())
			{
				level.sendParticles(player, ModParticles.TANK_BULLET_IMPACT.get(), true, at.x, at.y, at.z, 0, normal.x, normal.y, normal.z, 1.0);
			}
		}
		this.discard();
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance)
	{
		return distance < 256 * 256;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putInt("Life", this.life);
		tag.putFloat("Damage", this.damage);
		tag.putBoolean("Heavy", this.entityData.get(HEAVY));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		this.life = tag.getInt("Life");
		this.damage = tag.getFloat("Damage");
		this.entityData.set(HEAVY, tag.getBoolean("Heavy"));
	}
}
