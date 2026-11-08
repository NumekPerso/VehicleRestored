package com.mrcrayfish.vehicle.client.handler;

import com.mrcrayfish.vehicle.client.event.PlayerModelEvent;
import com.mrcrayfish.vehicle.client.render.layer.LayerHeldVehicle;
import com.mrcrayfish.vehicle.common.entity.HeldVehicleDataHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Author: MrCrayfish
 */
public class HeldVehicleHandler
{
    /**
     * Adds the held vehicle layer to the player renderers. In 1.16.5 the layer list of the player
     * renderers was patched with reflection the first time a player was rendered; the layers are
     * now added through {@link EntityRenderersEvent.AddLayers} (mod event bus).
     */
    public static void addLayers(EntityRenderersEvent.AddLayers event)
    {
        for(String skin : event.getSkins())
        {
            PlayerRenderer renderer = event.getSkin(skin);
            if(renderer != null)
            {
                renderer.addLayer(new LayerHeldVehicle(renderer));
            }
        }
    }

    public static final Map<UUID, AnimationCounter> idToCounter = new HashMap<>();

    @SubscribeEvent
    public void onSetupAngles(PlayerModelEvent.SetupAngles.Post event)
    {
        PlayerModel model = event.getModelPlayer();
        Player player = event.getPlayer();

        boolean holdingVehicle = HeldVehicleDataHandler.isHoldingVehicle(player);
        if(holdingVehicle && !idToCounter.containsKey(player.getUUID()))
        {
            idToCounter.put(player.getUUID(), new AnimationCounter(40));
        }
        else if(idToCounter.containsKey(player.getUUID()))
        {
            if(idToCounter.get(player.getUUID()).getProgress(event.getPartialTicks()) == 0F)
            {
                idToCounter.remove(player.getUUID());
                return;
            }
            if(!holdingVehicle)
            {
                AnimationCounter counter = idToCounter.get(player.getUUID());
                player.yBodyRot = player.getYHeadRot() - (player.getYHeadRot() - player.yBodyRotO) * counter.getProgress(event.getPartialTicks());
            }
        }
        else
        {
            return;
        }

        AnimationCounter counter = idToCounter.get(player.getUUID());
        counter.update(holdingVehicle);
        float progress = counter.getProgress(event.getPartialTicks());
        model.rightArm.xRot = (float) Math.toRadians(-180F * progress);
        model.rightArm.zRot = (float) Math.toRadians(-5F * progress);
        model.rightArm.y = (player.isCrouching() ? 3.0F : -0.5F) * progress;
        model.leftArm.xRot = (float) Math.toRadians(-180F * progress);
        model.leftArm.zRot = (float) Math.toRadians(5F * progress);
        model.leftArm.y = (player.isCrouching() ? 3.0F : -0.5F) * progress;
    }

    public static class AnimationCounter
    {
        private final int MAX_COUNT;
        private int prevCount;
        private int currentCount;

        private AnimationCounter(int maxCount)
        {
            this.MAX_COUNT = maxCount;
        }

        public int update(boolean increment)
        {
            prevCount = currentCount;
            if(increment)
            {
                if(currentCount < MAX_COUNT)
                {
                    currentCount++;
                }
            }
            else
            {
                if(currentCount > 0)
                {
                    currentCount = Math.max(0, currentCount - 2);
                }
            }
            return currentCount;
        }

        public int getMaxCount()
        {
            return MAX_COUNT;
        }

        public int getCurrentCount()
        {
            return currentCount;
        }

        public float getProgress(float partialTicks)
        {
            return (prevCount + (currentCount - prevCount) * partialTicks) / (float) MAX_COUNT;
        }
    }
}
