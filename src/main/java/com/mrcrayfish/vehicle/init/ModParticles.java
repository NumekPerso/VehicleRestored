package com.mrcrayfish.vehicle.init;

import com.mrcrayfish.vehicle.Reference;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Particles of the tank effects. Their textures are the mod's own (textures/particle/tank_*), drawn
 * with a soft particle shader (see TankEffects on the client).
 */
public class ModParticles
{
    public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Reference.MOD_ID);

    public static final RegistryObject<SimpleParticleType> TANK_FIRE = REGISTER.register("tank_fire", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> TANK_SMOKE = REGISTER.register("tank_smoke", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> TANK_FLASH = REGISTER.register("tank_flash", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> TANK_SPARK = REGISTER.register("tank_spark", () -> new SimpleParticleType(true));
    /** Sent by the server where a machine gun bullet hits: sparks and dust */
    public static final RegistryObject<SimpleParticleType> TANK_BULLET_IMPACT = REGISTER.register("tank_bullet_impact", () -> new SimpleParticleType(true));
    /** Sent by the server where a shell explodes; the client spawns the whole explosion effect */
    public static final RegistryObject<SimpleParticleType> TANK_EXPLOSION = REGISTER.register("tank_explosion", () -> new SimpleParticleType(true));
}
