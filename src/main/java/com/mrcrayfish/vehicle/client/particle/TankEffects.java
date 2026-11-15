package com.mrcrayfish.vehicle.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import com.mrcrayfish.vehicle.init.ModParticles;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/**
 * Visual effects of the tank, made of the mod's own soft particles (no vanilla particle texture):
 * the muzzle blast when the gun fires, the smoke trail and tracer of the shell, and the explosion
 * where it hits (fire ball, flash, sparks, thrown debris of the ground, dust and a rising column of
 * dark smoke). Client only.
 */
public final class TankEffects
{
    private static SpriteSet fireSprites;
    private static SpriteSet smokeSprites;
    private static SpriteSet flashSprites;
    private static SpriteSet sparkSprites;
    @Nullable
    private static ShaderInstance shader;

    /**
     * Opacity from which an effect hides what is drawn after it (clouds, rain): the depth pass uses
     * the vanilla particle shader, which drops pixels under 0.1, with the alpha scaled by this.
     */
    private static final float DEPTH_ALPHA_SCALE = 0.1F / 0.3F;
    @Nullable
    private static VertexBuffer vertexBuffer;

    /**
     * Soft particles: premultiplied blend and linear filtering of the particle atlas, drawn with the
     * tank_particle shader (no alpha cut-off, glowing particles add their light), without writing
     * the depth so they blend with each other in any order. Once they are all drawn, their thick
     * parts write the depth (see {@link #draw}).
     */
    public static final ParticleRenderType RENDER_TYPE = new ParticleRenderType()
    {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures)
        {
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            ShaderInstance instance = shader;
            if(instance != null)
            {
                RenderSystem.setShader(() -> instance);
            }
            textures.getTexture(TextureAtlas.LOCATION_PARTICLES).setFilter(true, false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator)
        {
            draw(tesselator.getBuilder().end());
            Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES).setFilter(false, false);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString()
        {
            return Reference.MOD_ID + ":tank_effects";
        }
    };

    private static final Logger LOGGER = LogUtils.getLogger();

    private TankEffects() {}

    /**
     * Draws the effects, then writes the depth of their thick parts (colour off). The clouds and the
     * rain are drawn after the particles: without that depth they showed through the smoke.
     */
    private static void draw(BufferBuilder.RenderedBuffer buffer)
    {
        ShaderInstance colorShader = RenderSystem.getShader();
        if(buffer.isEmpty() || colorShader == null)
        {
            buffer.release();
            return;
        }
        if(vertexBuffer == null)
        {
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        vertexBuffer.bind();
        vertexBuffer.upload(buffer);
        Matrix4f modelView = RenderSystem.getModelViewMatrix();
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        vertexBuffer.drawWithShader(modelView, projection, colorShader);
        ShaderInstance depthShader = GameRenderer.getParticleShader();
        if(depthShader != null)
        {
            float[] color = RenderSystem.getShaderColor().clone();
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3] * DEPTH_ALPHA_SCALE);
            vertexBuffer.drawWithShader(modelView, projection, depthShader);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(false);
        }
        VertexBuffer.unbind();
    }

    /* ------------------------------------------------------------------ registration (mod bus) */

    public static void onRegisterShaders(RegisterShadersEvent event)
    {
        try
        {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(Reference.MOD_ID, "tank_particle"), DefaultVertexFormat.PARTICLE), instance -> shader = instance);
        }
        catch(IOException e)
        {
            // The effects still show with the vanilla particle shader, with harder edges
            shader = null;
            LOGGER.error("Could not load the tank particle shader", e);
        }
    }

    public static void onRegisterParticles(RegisterParticleProvidersEvent event)
    {
        event.registerSpriteSet(ModParticles.TANK_FIRE.get(), sprites -> {
            fireSprites = sprites;
            return (type, level, x, y, z, dx, dy, dz) -> fire(level, x, y, z, dx, dy, dz, 1F, 12);
        });
        event.registerSpriteSet(ModParticles.TANK_SMOKE.get(), sprites -> {
            smokeSprites = sprites;
            return (type, level, x, y, z, dx, dy, dz) -> smoke(level, x, y, z, dx, dy, dz, 0.5F, 2F, 60, 0.5F, 0.85F);
        });
        event.registerSpriteSet(ModParticles.TANK_FLASH.get(), sprites -> {
            flashSprites = sprites;
            return (type, level, x, y, z, dx, dy, dz) -> flash(level, x, y, z, 2F);
        });
        event.registerSpriteSet(ModParticles.TANK_SPARK.get(), sprites -> {
            sparkSprites = sprites;
            return (type, level, x, y, z, dx, dy, dz) -> spark(level, x, y, z, dx, dy, dz);
        });
        event.registerSpecial(ModParticles.TANK_EXPLOSION.get(), (type, level, x, y, z, dx, dy, dz) -> new Explosion(level, x, y, z, new Vec3(dx, dy, dz)));
        event.registerSpecial(ModParticles.TANK_BULLET_IMPACT.get(), (type, level, x, y, z, dx, dy, dz) -> new BulletImpact(level, x, y, z, new Vec3(dx, dy, dz)));    }

    /* ------------------------------------------------------------------ particles */

    private static TankParticle fire(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, float size, int life)
    {
        return new TankParticle(level, x, y, z, dx, dy, dz, fireSprites, true)
                .glow().size(size * 0.6F, size).life(life).color(1F, 1F, 1F, 1F).fade(0F, 3F)
                .motion(0.8F, -0.15F, false).spin((level.random.nextFloat() - 0.5F) * 0.12F);
    }

    private static TankParticle smoke(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, float startSize, float endSize, int life, float grey, float alpha)
    {
        float tint = grey * (0.9F + level.random.nextFloat() * 0.2F);
        return new TankParticle(level, x, y, z, dx, dy, dz, smokeSprites, false)
                .size(startSize, endSize).life(life).color(tint, tint * 0.97F, tint * 0.93F, alpha).fade(0.06F, 1.4F)
                .motion(0.9F, -0.05F, true).spin((level.random.nextFloat() - 0.5F) * 0.04F);
    }

    private static TankParticle flash(ClientLevel level, double x, double y, double z, float size)
    {
        return new TankParticle(level, x, y, z, 0, 0, 0, flashSprites, false)
                .glow().size(size, size * 1.3F).life(3).color(1F, 1F, 1F, 1F).fade(0F, 0.6F);
    }

    private static TankParticle spark(ClientLevel level, double x, double y, double z, double dx, double dy, double dz)
    {
        return new TankParticle(level, x, y, z, dx, dy, dz, sparkSprites, false)
                .glow().size(0.09F + level.random.nextFloat() * 0.06F, 0.03F).life(10 + level.random.nextInt(18))
                .color(1F, 0.78F, 0.45F, 1F).fade(0F, 4F).motion(0.96F, 1F, true);
    }

    private static void add(Particle particle)
    {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    /** Scales the number of particles with the particle setting of the player */
    private static int count(int all)
    {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        return status == ParticleStatus.ALL ? all : status == ParticleStatus.DECREASED ? Math.max(1, all / 2) : Math.max(1, all / 5);
    }

    private static Vec3 randomDirection(RandomSource random)
    {
        Vec3 v = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        return v.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : v.normalize();
    }

    /** Two directions perpendicular to the given one (and to each other) */
    private static Vec3[] perpendicular(Vec3 dir)
    {
        Vec3 side = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 a = dir.cross(side).normalize();
        return new Vec3[]{a, dir.cross(a).normalize()};
    }

    private static boolean ready()
    {
        return fireSprites != null && smokeSprites != null && flashSprites != null && sparkSprites != null;
    }

    /* ------------------------------------------------------------------ effects */

    /**
     * Muzzle blast of the tank gun: flash, a tongue of fire, a cone of smoke pushed forward, a ring
     * of smoke around the muzzle and dust raised from the ground under the gun.
     */
    public static void muzzleBlast(TankEntity tank)
    {
        if(!ready() || !(tank.level() instanceof ClientLevel level))
            return;
        RandomSource random = level.random;
        Vec3 muzzle = tank.getMuzzlePosition(1F);
        Vec3 dir = tank.getGunDirection(1F);
        Vec3[] ring = perpendicular(dir);

        Vec3 flashAt = muzzle.add(dir.scale(0.7));
        add(flash(level, flashAt.x, flashAt.y, flashAt.z, 2.4F));
        Vec3 flashFar = muzzle.add(dir.scale(2.0));
        add(flash(level, flashFar.x, flashFar.y, flashFar.z, 1.6F));

        for(int i = 0; i < count(16); i++)
        {
            double speed = 0.3 + random.nextDouble() * 0.9;
            Vec3 v = dir.scale(speed).add(randomDirection(random).scale(0.07));
            Vec3 p = muzzle.add(dir.scale(0.3));
            add(fire(level, p.x, p.y, p.z, v.x, v.y, v.z, 0.9F + random.nextFloat() * 0.8F, 5 + random.nextInt(5)).motion(0.72F, 0F, false));
        }

        // Light smoke pushed out of the barrel
        for(int i = 0; i < count(9); i++)
        {
            double speed = 0.2 + random.nextDouble() * 0.9;
            Vec3 v = dir.scale(speed).add(randomDirection(random).scale(0.05 + speed * 0.06));
            Vec3 p = muzzle.add(dir.scale(0.6 + random.nextDouble() * 0.8));
            add(smoke(level, p.x, p.y, p.z, v.x, v.y, v.z, 0.4F, 1.6F + random.nextFloat() * 0.8F, 30 + random.nextInt(25), 0.68F, 0.45F)
                    .motion(0.86F, -0.05F, true));
        }

        // Thin ring of smoke around the muzzle
        int ringCount = count(8);
        for(int i = 0; i < ringCount; i++)
        {
            double a = Mth.TWO_PI * i / ringCount + random.nextDouble() * 0.4;
            Vec3 out = ring[0].scale(Math.cos(a)).add(ring[1].scale(Math.sin(a)));
            Vec3 v = out.scale(0.18 + random.nextDouble() * 0.1).add(dir.scale(0.1));
            Vec3 p = muzzle.add(out.scale(0.2)).add(dir.scale(0.3));
            add(smoke(level, p.x, p.y, p.z, v.x, v.y, v.z, 0.3F, 1.0F + random.nextFloat() * 0.5F, 25 + random.nextInt(15), 0.7F, 0.4F)
                    .motion(0.84F, -0.04F, true));
        }

        // A little dust blown up from the ground in front of the gun
        double groundY = tank.getY() + 0.15;
        for(int i = 0; i < count(6); i++)
        {
            double along = 3.0 + random.nextDouble() * 3.0;
            double px = muzzle.x - dir.x * 1.5 + dir.x * along + random.nextGaussian() * 0.8;
            double pz = muzzle.z - dir.z * 1.5 + dir.z * along + random.nextGaussian() * 0.8;
            Vec3 out = new Vec3(px - tank.getX(), 0, pz - tank.getZ());
            out = out.lengthSqr() > 1.0E-4 ? out.normalize() : new Vec3(dir.x, 0, dir.z);
            Vec3 v = out.scale(0.12 + random.nextDouble() * 0.2).add(0, 0.03 + random.nextDouble() * 0.05, 0);
            float dust = 0.62F + random.nextFloat() * 0.08F;
            add(smoke(level, px, groundY, pz, v.x, v.y, v.z, 0.6F, 1.8F + random.nextFloat() * 0.6F, 30 + random.nextInt(25), dust, 0.3F)
                    .color(dust, dust * 0.9F, dust * 0.76F, 0.3F).motion(0.88F, -0.02F, true));
        }
    }

    /**
     * Trail of the shell between two positions of a tick: a light smoke trail that widens and fades,
     * and a hot tracer (smaller for the light shells).
     */
    public static void shellTrail(Level world, Vec3 from, Vec3 to, boolean light)
    {
        if(!ready() || !(world instanceof ClientLevel level))
            return;
        RandomSource random = level.random;
        int puffs = light ? 1 : count(3);
        for(int i = 0; i < puffs; i++)
        {
            Vec3 p = from.lerp(to, (i + random.nextDouble()) / puffs);
            Vec3 v = randomDirection(random).scale(0.01);
            add(smoke(level, p.x, p.y, p.z, v.x, v.y + 0.005, v.z, light ? 0.12F : 0.18F, (light ? 0.45F : 0.65F) + random.nextFloat() * 0.25F, 18 + random.nextInt(12), 0.82F, light ? 0.2F : 0.28F)
                    .motion(0.92F, -0.01F, false));
        }
        int sparks = light ? 3 : 6;
        for(int i = 0; i < sparks; i++)
        {
            Vec3 p = from.lerp(to, i / (double) sparks);
            add(new TankParticle(level, p.x, p.y, p.z, 0, 0, 0, sparkSprites, false)
                    .glow().size(light ? 0.28F : 0.45F, light ? 0.1F : 0.15F).life(3).color(1F, 0.72F, 0.38F, 1F).fade(0F, 1F));
        }
    }

    /**
     * Flash and a little smoke at the muzzle of a machine gun
     */
    public static void machineGunFlash(Level world, Vec3 muzzle, Vec3 dir, boolean heavy)
    {
        if(!ready() || !(world instanceof ClientLevel level))
            return;
        RandomSource random = level.random;
        Vec3 at = muzzle.add(dir.scale(heavy ? 0.35 : 0.25));
        add(flash(level, at.x, at.y, at.z, heavy ? 0.9F : 0.6F).life(2));
        for(int i = 0; i < (heavy ? 3 : 2); i++)
        {
            Vec3 v = dir.scale(0.15 + random.nextDouble() * 0.2);
            add(fire(level, at.x, at.y, at.z, v.x, v.y, v.z, heavy ? 0.4F : 0.28F, 3 + random.nextInt(2)).motion(0.7F, 0F, false));
        }
        if(random.nextInt(heavy ? 2 : 3) == 0)
        {
            Vec3 v = dir.scale(0.05).add(0, 0.02, 0);
            add(smoke(level, at.x, at.y, at.z, v.x, v.y, v.z, 0.2F, heavy ? 1.0F : 0.7F, 20 + random.nextInt(15), 0.7F, 0.35F).motion(0.9F, -0.03F, false));
        }
    }

    /**
     * Shot of a light shell (rapid fire) out of a launch tube: a small flash and tongue of fire, a
     * wisp of smoke
     */
    public static void launchTubeBlast(Level world, Vec3 muzzle, Vec3 dir)
    {
        if(!ready() || !(world instanceof ClientLevel level))
            return;
        RandomSource random = level.random;
        Vec3 flashAt = muzzle.add(dir.scale(0.3));
        add(flash(level, flashAt.x, flashAt.y, flashAt.z, 0.9F).life(2));
        for(int i = 0; i < count(4); i++)
        {
            Vec3 v = dir.scale(0.25 + random.nextDouble() * 0.4).add(randomDirection(random).scale(0.04));
            Vec3 p = muzzle.add(dir.scale(0.2));
            add(fire(level, p.x, p.y, p.z, v.x, v.y, v.z, 0.35F + random.nextFloat() * 0.3F, 3 + random.nextInt(3)).motion(0.72F, 0F, false));
        }
        for(int i = 0; i < count(2); i++)
        {
            Vec3 v = dir.scale(0.1 + random.nextDouble() * 0.2).add(randomDirection(random).scale(0.03));
            Vec3 p = muzzle.add(dir.scale(0.4));
            add(smoke(level, p.x, p.y, p.z, v.x, v.y, v.z, 0.25F, 0.8F + random.nextFloat() * 0.4F, 18 + random.nextInt(12), 0.7F, 0.3F)
                    .motion(0.86F, -0.04F, true));
        }
    }

    /**
     * Tracer of a machine gun bullet over one tick of its flight: a glowing streak
     */
    public static void bulletTracer(Level world, Vec3 from, Vec3 to, boolean heavy)
    {
        if(!ready() || !(world instanceof ClientLevel level) || from.distanceToSqr(to) < 1.0E-4)
            return;
        add(new Tracer(level, from, to, sparkSprites, heavy ? 0.075F : 0.05F));
    }

    /**
     * Streak between two points, facing the camera along its length (tracers)
     */
    private static class Tracer extends TankParticle
    {
        private final Vec3 from;
        private final Vec3 to;
        private final float width;

        Tracer(ClientLevel level, Vec3 from, Vec3 to, SpriteSet sprites, float width)
        {
            super(level, (from.x + to.x) / 2, (from.y + to.y) / 2, (from.z + to.z) / 2, 0, 0, 0, sprites, false);
            this.from = from;
            this.to = to;
            this.width = width;
            this.glow().life(2).color(1F, 0.78F, 0.45F, 1F).fade(0F, 2F).motion(1F, 0F, false);
        }

        @Override
        public void tick()
        {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if(this.age++ >= this.lifetime)
            {
                this.remove();
            }
        }

        @Override
        public void render(com.mojang.blaze3d.vertex.VertexConsumer consumer, net.minecraft.client.Camera camera, float partialTicks)
        {
            Vec3 cam = camera.getPosition();
            Vec3 a = this.from.subtract(cam), b = this.to.subtract(cam);
            Vec3 axis = b.subtract(a);
            Vec3 side = axis.cross(a.add(b).scale(0.5));
            if(side.lengthSqr() < 1.0E-8)
                return;
            side = side.normalize().scale(this.width);
            float t = Mth.clamp((this.age + partialTicks) / this.lifetime, 0F, 1F);
            float alpha = 1F - t * t;
            int light = this.getLightColor(partialTicks);
            float u0 = this.getU0(), u1 = this.getU1(), v0 = this.getV0(), v1 = this.getV1();
            Vec3[] corners = {a.add(side), b.add(side), b.subtract(side), a.subtract(side)};
            float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
            for(int i = 0; i < 4; i++)
            {
                consumer.vertex(corners[i].x, corners[i].y, corners[i].z).uv(uv[i][0], uv[i][1]).color(this.rCol, this.gCol, this.bCol, alpha).uv2(light).endVertex();
            }
        }
    }

    /**
     * Where a machine gun bullet hits: sparks, a puff of dust the colour of the block and a few chips
     */
    private static class BulletImpact extends NoRenderParticle
    {
        private final Vec3 normal;

        BulletImpact(ClientLevel level, double x, double y, double z, Vec3 normal)
        {
            super(level, x, y, z);
            this.normal = normal.lengthSqr() > 1.0E-4 ? normal.normalize() : new Vec3(0, 1, 0);
            this.lifetime = 1;
        }

        @Override
        public void tick()
        {
            if(ready())
            {
                RandomSource random = this.random;
                for(int i = 0; i < 6; i++)
                {
                    Vec3 v = randomDirection(random).add(this.normal.scale(1.2)).normalize().scale(0.15 + random.nextDouble() * 0.3);
                    add(spark(this.level, this.x, this.y, this.z, v.x, v.y, v.z).life(4 + random.nextInt(6)));
                }
                BlockPos pos = BlockPos.containing(this.x - this.normal.x * 0.1, this.y - this.normal.y * 0.1, this.z - this.normal.z * 0.1);
                BlockState state = this.level.getBlockState(pos);
                if(!state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE)
                {
                    int color = state.getMapColor(this.level, pos).col;
                    float r = Mth.lerp(0.4F, ((color >> 16) & 255) / 255F, 0.6F), g = Mth.lerp(0.4F, ((color >> 8) & 255) / 255F, 0.56F), b = Mth.lerp(0.4F, (color & 255) / 255F, 0.5F);
                    for(int i = 0; i < 2; i++)
                    {
                        Vec3 v = this.normal.scale(0.03 + random.nextDouble() * 0.04).add(randomDirection(random).scale(0.02));
                        add(smoke(this.level, this.x, this.y, this.z, v.x, v.y, v.z, 0.25F, 0.9F + random.nextFloat() * 0.4F, 25 + random.nextInt(20), 1F, 0.55F).color(r, g, b, 0.55F).motion(0.9F, -0.01F, false));
                    }
                    BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(pos);
                    for(int i = 0; i < 3; i++)
                    {
                        Vec3 v = randomDirection(random).add(this.normal.scale(1.5)).normalize().scale(0.12 + random.nextDouble() * 0.15);
                        Particle chip = Minecraft.getInstance().particleEngine.createParticle(debris, this.x, this.y, this.z, 0, 0, 0);
                        if(chip != null)
                        {
                            chip.setParticleSpeed(v.x, v.y, v.z);
                        }
                    }
                }
            }
            this.remove();
        }
    }

    /**
     * Explosion of a shell, spawned on the clients by the server (see TankShellEntity). The normal
     * is the side that was hit (up for an entity), the debris and the blast go that way; its length
     * is the size of the explosion (1 for a light shell, more for the 120 mm shell).
     */
    private static class Explosion extends NoRenderParticle
    {
        private final Vec3 normal;
        /** Size of the explosion: number, size and speed of everything thrown */
        private final float k;
        @Nullable
        private final BlockState ground;
        private final BlockPos groundPos;

        Explosion(ClientLevel level, double x, double y, double z, Vec3 normal)
        {
            super(level, x, y, z);
            double size = normal.length();
            this.k = size > 1.0E-2 ? (float) Mth.clamp(size, 0.5, 4.0) : 1F;
            this.normal = normal.lengthSqr() > 1.0E-4 ? normal.normalize() : new Vec3(0, 1, 0);
            this.lifetime = Math.round(5 * this.k);
            // The block that was hit (behind the impact point), or the ground under it
            BlockPos pos = BlockPos.containing(x - this.normal.x * 0.3, y - this.normal.y * 0.3, z - this.normal.z * 0.3);
            BlockState state = level.getBlockState(pos);
            if(state.isAir())
            {
                pos = BlockPos.containing(x, y - 1.0, z);
                state = level.getBlockState(pos);
            }
            this.groundPos = pos;
            this.ground = state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE ? null : state;
        }

        @Override
        public void tick()
        {
            if(!ready())
            {
                this.remove();
                return;
            }
            if(this.age == 0)
            {
                this.blast();
            }
            else
            {
                this.burn();
            }
            if(++this.age >= this.lifetime)
            {
                this.remove();
            }
        }

        private Vec3 outward(double spread)
        {
            // Mostly away from the hit surface
            Vec3 v = randomDirection(this.random);
            if(v.dot(this.normal) < 0)
            {
                v = v.subtract(this.normal.scale(2 * v.dot(this.normal)));
            }
            return v.add(this.normal.scale(spread)).normalize();
        }

        private int n(int base)
        {
            return count(Math.round(base * this.k));
        }

        private void blast()
        {
            RandomSource random = this.random;
            ClientLevel level = this.level;
            float k = this.k;
            add(flash(level, this.x, this.y, this.z, 6F * k));

            // Fire ball
            for(int i = 0; i < this.n(26); i++)
            {
                Vec3 v = this.outward(0.6).scale((0.12 + random.nextDouble() * 0.35) * k);
                Vec3 p = new Vec3(this.x, this.y, this.z).add(randomDirection(random).scale(random.nextDouble() * 0.6 * k));
                add(fire(level, p.x, p.y, p.z, v.x, v.y, v.z, (2.0F + random.nextFloat() * 1.6F) * k, Math.round((12 + random.nextInt(8)) * (0.6F + 0.4F * k))).motion(0.8F, -0.2F, false));
            }

            // Dark smoke (2.4 to 3.4 blocks wide and 2 to 3.5 seconds for a light shell, about twice
            // as wide and 1.5 times as long for the 120 mm shell)
            for(int i = 0; i < this.n(16); i++)
            {
                Vec3 v = this.outward(0.5).scale((0.05 + random.nextDouble() * 0.12) * k).add(0, 0.03, 0);
                Vec3 p = new Vec3(this.x, this.y, this.z).add(randomDirection(random).scale(random.nextDouble() * 0.9 * k));
                add(smoke(level, p.x, p.y, p.z, v.x, v.y, v.z, 1.0F * k, (2.4F + random.nextFloat() * 1.0F) * k, Math.round((40 + random.nextInt(30)) * (0.6F + 0.4F * k)), 0.22F, 0.85F)
                        .motion(0.92F, -0.06F, true));
            }

            // Sparks and burning fragments
            for(int i = 0; i < this.n(45); i++)
            {
                Vec3 v = this.outward(0.4).scale((0.3 + random.nextDouble() * 0.7) * Math.sqrt(k));
                add(spark(level, this.x, this.y, this.z, v.x, v.y, v.z));
            }

            // Blast wave: a fast ring of pale dust and vapour along the ground (big shells)
            if(k > 1.5F)
            {
                int waveCount = count(Math.round(36 * k));
                for(int i = 0; i < waveCount; i++)
                {
                    double a = Mth.TWO_PI * i / waveCount + random.nextDouble() * 0.15;
                    double speed = (0.55 + random.nextDouble() * 0.2) * k;
                    add(smoke(level, this.x, this.y + 0.4, this.z, Math.cos(a) * speed, 0.01, Math.sin(a) * speed, 0.6F * k, 1.6F * k, 10 + random.nextInt(6), 0.9F, 0.35F)
                            .fade(0.05F, 1.5F).motion(0.8F, 0F, true));
                }
            }

            if(this.ground != null)
            {
                // Pieces of the ground thrown in the air
                BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, this.ground).setPos(this.groundPos);
                for(int i = 0; i < this.n(40); i++)
                {
                    Vec3 v = this.outward(1.2).scale((0.25 + random.nextDouble() * 0.5) * Math.sqrt(k));
                    Particle particle = Minecraft.getInstance().particleEngine.createParticle(debris, this.x, this.y + 0.2, this.z, 0, 0, 0);
                    if(particle != null)
                    {
                        particle.setParticleSpeed(v.x, v.y, v.z);
                        particle.scale((1.5F + random.nextFloat() * 1.5F) * (0.7F + 0.3F * k));
                        particle.setLifetime(30 + random.nextInt(30));
                    }
                }

                // Dust ring along the ground, the colour of the ground
                int color = this.ground.getMapColor(level, this.groundPos).col;
                float r = Mth.lerp(0.5F, ((color >> 16) & 255) / 255F, 0.6F);
                float g = Mth.lerp(0.5F, ((color >> 8) & 255) / 255F, 0.56F);
                float b = Mth.lerp(0.5F, (color & 255) / 255F, 0.5F);
                int ringCount = this.n(26);
                for(int i = 0; i < ringCount; i++)
                {
                    double a = Mth.TWO_PI * i / ringCount + random.nextDouble() * 0.25;
                    double speed = (0.25 + random.nextDouble() * 0.25) * k;
                    add(smoke(level, this.x, this.y + 0.3, this.z, Math.cos(a) * speed, 0.02 + random.nextDouble() * 0.04, Math.sin(a) * speed, 0.7F * k, (2.0F + random.nextFloat() * 0.8F) * k, 25 + random.nextInt(20), 1F, 0.6F)
                            .color(r, g, b, 0.6F).motion(0.88F, -0.02F, true));
                }
            }
        }

        /** After the blast: the fire ball rises and feeds a column of smoke */
        private void burn()
        {
            RandomSource random = this.random;
            ClientLevel level = this.level;
            float k = this.k;
            double rise = this.age * 0.25 * k;
            for(int i = 0; i < count(Math.round(3 * k)); i++)
            {
                Vec3 p = new Vec3(this.x, this.y + rise, this.z).add(randomDirection(random).scale(0.6 * k));
                add(fire(level, p.x, p.y, p.z, random.nextGaussian() * 0.03, (0.12 + random.nextDouble() * 0.1) * k, random.nextGaussian() * 0.03, (1.6F + random.nextFloat()) * k, 10 + random.nextInt(6)));
            }
            for(int i = 0; i < count(Math.round(4 * k)); i++)
            {
                Vec3 p = new Vec3(this.x, this.y + rise, this.z).add(randomDirection(random).scale(0.7 * k));
                add(smoke(level, p.x, p.y, p.z, random.nextGaussian() * 0.03 * k, (0.08 + random.nextDouble() * 0.1) * k, random.nextGaussian() * 0.03 * k, 1.1F * k, (2.4F + random.nextFloat() * 1.0F) * k, Math.round((45 + random.nextInt(30)) * (0.6F + 0.4F * k)), 0.2F, 0.8F)
                        .motion(0.93F, -0.1F, true));
            }
        }
    }
}
