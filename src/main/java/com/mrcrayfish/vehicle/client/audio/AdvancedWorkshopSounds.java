package com.mrcrayfish.vehicle.client.audio;

import com.mrcrayfish.vehicle.block.AdvancedWorkshopBlock;
import com.mrcrayfish.vehicle.init.ModSounds;
import com.mrcrayfish.vehicle.tileentity.AdvancedWorkshopTileEntity;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sounds of the advanced workshop on the client, while it works: two loops that fade in and out with
 * the work, the hum of the machine and its head moving (a long recording looped where it matches its
 * start), and the laser welding: short pieces of the welding recording picked at random and played one
 * over the other at uneven moments, each a little higher or lower and louder or softer, so it never
 * sounds like the same loop. The start, the end and the cancel are played by the server.
 */
public final class AdvancedWorkshopSounds
{
    /** Ticks between two pieces of welding: from, to (the pieces last 11 to 17 ticks, so they overlap) */
    private static final int WELD_GAP_MIN = 4, WELD_GAP_MAX = 11;
    private static final Map<AdvancedWorkshopTileEntity, State> STATES = new WeakHashMap<>();

    private AdvancedWorkshopSounds() {}

    private static class State
    {
        final Loop[] loops = new Loop[2];
        long nextWeld;
    }

    /** Called each client tick of the workshop */
    public static void tick(AdvancedWorkshopTileEntity workshop)
    {
        Level level = workshop.getLevel();
        if(level == null || !working(workshop))
            return;
        State state = STATES.computeIfAbsent(workshop, w -> new State());
        start(state.loops, 0, workshop, ModSounds.BLOCK_ADVANCED_WORKSHOP_RUNNING::get, 0.45F);
        start(state.loops, 1, workshop, ModSounds.BLOCK_ADVANCED_WORKSHOP_MOVING::get, 1.0F);

        // the welding: a random piece now and then, overlapping the last one
        long now = level.getGameTime();
        if(now >= state.nextWeld)
        {
            RandomSource random = level.random;
            state.nextWeld = now + WELD_GAP_MIN + random.nextInt(WELD_GAP_MAX - WELD_GAP_MIN + 1);
            Vec3 centre = centre(workshop);
            level.playLocalSound(centre.x, centre.y, centre.z, ModSounds.BLOCK_ADVANCED_WORKSHOP_WELDING.get(), SoundSource.BLOCKS,
                    0.35F + random.nextFloat() * 0.3F, 0.88F + random.nextFloat() * 0.24F, false);
        }
    }

    private static void start(Loop[] loops, int index, AdvancedWorkshopTileEntity workshop, Supplier<SoundEvent> sound, float volume)
    {
        if(loops[index] == null || loops[index].isStopped())
        {
            loops[index] = new Loop(workshop, sound.get(), volume);
            Minecraft.getInstance().getSoundManager().play(loops[index]);
        }
    }

    private static boolean working(AdvancedWorkshopTileEntity workshop)
    {
        return !workshop.isRemoved() && workshop.isCrafting() && workshop.isPowered() && !workshop.isBlocked() && workshop.getProgress() < workshop.getTotal();
    }

    private static Vec3 centre(AdvancedWorkshopTileEntity workshop)
    {
        return AdvancedWorkshopBlock.getCentre(workshop.getBlockPos(), workshop.getBlockState().getValue(AdvancedWorkshopBlock.FACING));
    }

    /** One looping sound of the machine at work, from its middle */
    private static class Loop extends AbstractTickableSoundInstance
    {
        private final WeakReference<AdvancedWorkshopTileEntity> workshop;
        private final float full;

        Loop(AdvancedWorkshopTileEntity workshop, SoundEvent sound, float full)
        {
            super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.workshop = new WeakReference<>(workshop);
            this.full = full;
            Vec3 centre = centre(workshop);
            this.x = centre.x;
            this.y = centre.y;
            this.z = centre.z;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.05F;
        }

        @Override
        public void tick()
        {
            AdvancedWorkshopTileEntity workshop = this.workshop.get();
            boolean on = workshop != null && workshop.getLevel() == Minecraft.getInstance().level && working(workshop);
            this.volume = on ? Math.min(this.full, this.volume + 0.08F) : this.volume - 0.08F;
            if(this.volume <= 0.02F)
            {
                this.stop();
            }
        }
    }
}
