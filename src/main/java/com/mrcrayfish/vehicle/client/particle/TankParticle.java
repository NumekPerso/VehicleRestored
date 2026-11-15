package com.mrcrayfish.vehicle.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/**
 * One soft particle of the tank effects: a fire ball, a smoke puff, a muzzle flash or a spark. It
 * grows from its start size to its end size, fades in and out, turns slowly, and glowing ones are
 * full bright and add their light (see the tank_particle shader). Made by {@link TankEffects}.
 */
public class TankParticle extends TextureSheetParticle
{
    /** Light value read by the shader as "glowing" (the world never gives more than 240) */
    private static final int GLOW_LIGHT = 248 | 248 << 16;

    private final SpriteSet sprites;
    private final boolean animated;
    private boolean glow;
    private float startSize = 1F;
    private float endSize = 1F;
    private float maxAlpha = 1F;
    /** Part of the life used to fade in, and power of the fade out (higher fades later) */
    private float fadeIn = 0.05F;
    private float fadeOutPower = 1F;
    private float rollSpeed;

    public TankParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, boolean animated)
    {
        super(level, x, y, z);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.sprites = sprites;
        this.animated = animated;
        this.gravity = 0F;
        this.friction = 0.9F;
        this.hasPhysics = false;
        this.roll = this.oRoll = this.random.nextFloat() * Mth.TWO_PI;
        if(animated)
        {
            this.setSpriteFromAge(sprites);
        }
        else
        {
            this.pickSprite(sprites);
        }
    }

    /* Settings, used when the particle is made */

    public TankParticle glow()
    {
        this.glow = true;
        return this;
    }

    public TankParticle size(float start, float end)
    {
        this.startSize = start;
        this.endSize = end;
        this.quadSize = start;
        return this;
    }

    public TankParticle life(int ticks)
    {
        this.lifetime = Math.max(1, ticks);
        return this;
    }

    public TankParticle color(float r, float g, float b, float a)
    {
        this.setColor(r, g, b);
        this.maxAlpha = a;
        this.alpha = a;
        return this;
    }

    public TankParticle fade(float fadeIn, float fadeOutPower)
    {
        this.fadeIn = fadeIn;
        this.fadeOutPower = fadeOutPower;
        return this;
    }

    public TankParticle motion(float friction, float gravity, boolean physics)
    {
        this.friction = friction;
        this.gravity = gravity;
        this.hasPhysics = physics;
        return this;
    }

    public TankParticle spin(float speed)
    {
        this.rollSpeed = speed;
        return this;
    }

    @Override
    public void tick()
    {
        super.tick();
        if(this.removed)
            return;
        this.oRoll = this.roll;
        this.roll += this.rollSpeed;
        this.rollSpeed *= 0.97F;
        if(this.animated)
        {
            this.setSpriteFromAge(this.sprites);
        }
    }

    private float progress(float partialTicks)
    {
        return Mth.clamp((this.age + partialTicks) / (float) this.lifetime, 0F, 1F);
    }

    @Override
    public float getQuadSize(float partialTicks)
    {
        float t = this.progress(partialTicks);
        float grow = 1F - (float) Math.pow(1F - t, 2.5);
        return Mth.lerp(grow, this.startSize, this.endSize);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks)
    {
        float t = this.progress(partialTicks);
        float in = this.fadeIn > 0F ? Mth.clamp(t / this.fadeIn, 0F, 1F) : 1F;
        float out = 1F - (float) Math.pow(t, this.fadeOutPower);
        this.alpha = this.maxAlpha * in * out;
        super.render(consumer, camera, partialTicks);
    }

    @Override
    protected int getLightColor(float partialTicks)
    {
        return this.glow ? GLOW_LIGHT : super.getLightColor(partialTicks);
    }

    /* The texture is sampled with linear filtering: stay half a texel inside the sprite */

    @Override
    protected float getU0()
    {
        return this.sprite.getU(0.2);
    }

    @Override
    protected float getU1()
    {
        return this.sprite.getU(15.8);
    }

    @Override
    protected float getV0()
    {
        return this.sprite.getV(0.2);
    }

    @Override
    protected float getV1()
    {
        return this.sprite.getV(15.8);
    }

    @Override
    public boolean shouldCull()
    {
        // Big quads: the small bounding box would make them vanish at the edge of the screen
        return false;
    }

    @Override
    public ParticleRenderType getRenderType()
    {
        return TankEffects.RENDER_TYPE;
    }
}
