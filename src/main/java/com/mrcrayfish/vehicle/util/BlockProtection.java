package com.mrcrayfish.vehicle.util;

import com.mrcrayfish.vehicle.entity.TrailerEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Vehicles that change blocks on their own (lawn mower, seeder, fertilizer) used to bypass every
 * protection: no Forge event was posted, so claim mods and spawn protection never saw them. The
 * change is now made in the name of the player driving the vehicle, or pulling the trailer, and
 * goes through the same events as if that player had done it by hand.
 *
 * Call these on the server only.
 */
public final class BlockProtection
{
    /* A chain of trailers is short, this only guards against a loop */
    private static final int MAX_CHAIN = 8;

    private BlockProtection() {}

    /**
     * The player responsible for the entity: the driver of a vehicle, or, for a trailer, the one
     * driving (or pulling by hand) the head of the chain.
     */
    @Nullable
    public static Player getResponsiblePlayer(Entity entity)
    {
        Entity current = entity;
        for(int i = 0; i < MAX_CHAIN && current != null; i++)
        {
            if(current instanceof Player player)
                return player;
            Entity driver = current.getControllingPassenger();
            if(driver instanceof Player player)
                return player;
            current = current instanceof TrailerEntity trailer ? trailer.getPullingEntity() : null;
        }
        return null;
    }

    /**
     * Without a player, the vehicle acts like a mob: it follows the mobGriefing game rule.
     */
    private static boolean canActWithoutPlayer(ServerLevel level, Entity source)
    {
        return ForgeEventFactory.getMobGriefingEvent(level, source);
    }

    /** Can the entity break the block at this position? */
    public static boolean canBreak(ServerLevel level, Entity source, BlockPos pos, BlockState state)
    {
        Player player = getResponsiblePlayer(source);
        if(player == null)
            return canActWithoutPlayer(level, source);
        if(!level.mayInteract(player, pos))
            return false;
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player));
    }

    /**
     * Places the block in the name of the responsible player, and undoes it if a protection
     * refuses it, the way vanilla places blocks. Returns true if the block stays.
     */
    public static boolean tryPlace(ServerLevel level, Entity source, BlockPos pos, BlockState state)
    {
        Player player = getResponsiblePlayer(source);
        if(player == null && !canActWithoutPlayer(level, source))
            return false;
        if(player != null && !level.mayInteract(player, pos))
            return false;

        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if(!level.setBlockAndUpdate(pos, state))
            return false;
        if(ForgeEventFactory.onBlockPlace(player != null ? player : source, snapshot, Direction.UP))
        {
            snapshot.restore(true, false);
            return false;
        }
        return true;
    }

    /** Can the entity use bone meal on this block? */
    public static boolean canFertilize(ServerLevel level, Entity source, BlockPos pos, BlockState state, ItemStack boneMeal)
    {
        Player player = getResponsiblePlayer(source);
        if(player == null)
            return canActWithoutPlayer(level, source);
        if(!level.mayInteract(player, pos))
            return false;
        // -1: a mod cancelled it. 1: a mod already applied it, which would use the item twice here
        return ForgeEventFactory.onApplyBonemeal(player, level, pos, state, boneMeal) == 0;
    }
}
