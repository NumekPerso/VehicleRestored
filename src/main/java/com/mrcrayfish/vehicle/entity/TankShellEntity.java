package com.mrcrayfish.vehicle.entity;

import com.mrcrayfish.vehicle.client.audio.MovingSoundShellFlight;
import com.mrcrayfish.vehicle.client.particle.TankEffects;
import com.mrcrayfish.vehicle.init.ModEntities;
import com.mrcrayfish.vehicle.init.ModSounds;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fml.DistExecutor;

/**
 * Shell fired by the tank: the 120 mm shell of the main gun, or a light shell (rapid fire). It
 * flies with a drop, leaves a trail and explodes where it hits. The explosion hurts entities but
 * does not break blocks.
 */
public class TankShellEntity extends Projectile implements IEntityAdditionalSpawnData
{
    private static final EntityDataAccessor<Boolean> LIGHT = SynchedEntityData.defineId(TankShellEntity.class, EntityDataSerializers.BOOLEAN);
    /** Explosion power (TNT is 4) and size of the explosion effect, for the 120 mm and the light shells */
    private static final float EXPLOSION_POWER = 7.0F, LIGHT_EXPLOSION_POWER = 3.5F;
    private static final float EFFECT_SIZE = 2.2F, LIGHT_EFFECT_SIZE = 1.0F;
    public static final double GRAVITY = 0.02;
    private static final int MAX_LIFE = 200;

    /** The tank that fired the shell: it is never hit by its own shell */
    private int tankId = -1;
    private int life;
    /** Client: the flight sound of the 120 mm shell has been started */
    private boolean flightSound;

    public TankShellEntity(EntityType<? extends TankShellEntity> type, Level level)
    {
        super(type, level);
    }

    public TankShellEntity(Level level, Entity shooter, Entity tank, Vec3 position, Vec3 motion, boolean light)
    {
        this(ModEntities.TANK_SHELL.get(), level);
        this.entityData.set(LIGHT, light);
        this.setOwner(shooter);
        this.tankId = tank.getId();
        this.setPos(position);
        this.setDeltaMovement(motion);
        this.updateRotation();
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(LIGHT, false);
    }

    /** A light shell (rapid fire) rather than the 120 mm shell */
    public boolean isLight()
    {
        return this.entityData.get(LIGHT);
    }

    /*
     * The vanilla spawn and motion packets clamp the speed to 3.9 blocks per tick on each axis, which
     * bends the path of a fast projectile on the clients. The exact velocity is sent once with the
     * spawn (and the clients fly the shell themselves, see ModEntities: no velocity updates).
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
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
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

        // The 120 mm shell whistles while it flies (the sound follows it)
        if(this.level().isClientSide() && !this.flightSound && !this.isLight())
        {
            this.flightSound = true;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MovingSoundShellFlight.play(this));
        }

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if(hit.getType() != HitResult.Type.MISS)
        {
            this.onHit(hit);
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        Vec3 next = this.position().add(motion);
        if(this.level().isClientSide())
        {
            // Smoke trail and tracer along the path of the tick
            Vec3 from = this.position();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TankEffects.shellTrail(this.level(), from, next, this.isLight()));
        }
        this.setPos(next);
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
    protected void onHit(HitResult result)
    {
        super.onHit(result);
        Vec3 at = result.getLocation();
        Vec3 normal = new Vec3(0, 1, 0);
        if(result instanceof EntityHitResult entityHit)
        {
            at = entityHit.getEntity().position().add(0, entityHit.getEntity().getBbHeight() / 2, 0);
        }
        else if(result instanceof BlockHitResult blockHit)
        {
            normal = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
        }
        if(this.level() instanceof ServerLevel level)
        {
            // No damage to the terrain for now: entities only. The explosion is made by hand so the
            // clients do not show the vanilla explosion particles but the tank effect.
            boolean light = this.isLight();
            Explosion explosion = new Explosion(level, this, null, null, at.x, at.y, at.z, light ? LIGHT_EXPLOSION_POWER : EXPLOSION_POWER, false, Explosion.BlockInteraction.KEEP);
            if(!ForgeEventFactory.onExplosionStart(level, explosion))
            {
                explosion.explode();
                explosion.finalizeExplosion(false);
            }
            if(light)
            {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 5.0F, 0.85F + this.random.nextFloat() * 0.15F);
            }
            else
            {
                // Big explosion, heard far away
                level.playSound(null, at.x, at.y, at.z, ModSounds.ENTITY_TANK_SHELL_EXPLODE.get(), SoundSource.BLOCKS, 12.0F, 0.95F + this.random.nextFloat() * 0.1F);
            }
            // The length of the normal is the size of the effect
            Vec3 effect = normal.scale(light ? LIGHT_EFFECT_SIZE : EFFECT_SIZE);
            for(ServerPlayer player : level.players())
            {
                level.sendParticles(player, ModParticles.TANK_EXPLOSION.get(), true, at.x, at.y, at.z, 0, effect.x, effect.y, effect.z, 1.0);
            }
            this.discard();
        }
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
        tag.putBoolean("Light", this.isLight());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        this.life = tag.getInt("Life");
        this.entityData.set(LIGHT, tag.getBoolean("Light"));
    }
}
