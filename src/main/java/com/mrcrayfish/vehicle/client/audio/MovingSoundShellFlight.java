package com.mrcrayfish.vehicle.client.audio;

import com.mrcrayfish.vehicle.entity.TankShellEntity;
import com.mrcrayfish.vehicle.init.ModSounds;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Sound of a 120 mm shell in flight: it follows the shell and fades out quickly when the shell is
 * gone (it exploded).
 */
public class MovingSoundShellFlight extends AbstractTickableSoundInstance
{
    private static final float VOLUME = 2.5F;

    private final WeakReference<TankShellEntity> shellRef;
    private boolean fading;

    private MovingSoundShellFlight(TankShellEntity shell)
    {
        super(ModSounds.ENTITY_TANK_SHELL_FLY.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.shellRef = new WeakReference<>(shell);
        this.volume = VOLUME;
        this.pitch = 0.95F + shell.level().random.nextFloat() * 0.1F;
        this.x = shell.getX();
        this.y = shell.getY();
        this.z = shell.getZ();
    }

    /** Starts the flight sound of a shell (client) */
    public static void play(TankShellEntity shell)
    {
        Minecraft.getInstance().getSoundManager().play(new MovingSoundShellFlight(shell));
    }

    @Override
    public void tick()
    {
        TankShellEntity shell = this.shellRef.get();
        Level level = Minecraft.getInstance().level;
        if(shell == null || shell.isRemoved() || shell.level() != level)
        {
            this.fading = true;
        }
        if(this.fading)
        {
            this.volume *= 0.55F;
            if(this.volume < 0.03F)
            {
                this.stop();
            }
            return;
        }
        this.x = shell.getX();
        this.y = shell.getY();
        this.z = shell.getZ();
    }
}
