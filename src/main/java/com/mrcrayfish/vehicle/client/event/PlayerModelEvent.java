package com.mrcrayfish.vehicle.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events fired on the Forge event bus while a player model is posed and rendered. They replace
 * the player model events of the Obfuscate library, which the original mod used.
 */
public abstract class PlayerModelEvent extends Event
{
    private final Player player;
    private final PlayerModel<?> modelPlayer;
    private final float partialTicks;

    protected PlayerModelEvent(Player player, PlayerModel<?> modelPlayer, float partialTicks)
    {
        this.player = player;
        this.modelPlayer = modelPlayer;
        this.partialTicks = partialTicks;
    }

    public Player getPlayer()
    {
        return this.player;
    }

    public PlayerModel<?> getModelPlayer()
    {
        return this.modelPlayer;
    }

    public float getPartialTicks()
    {
        return this.partialTicks;
    }

    /**
     * Fired at the end of {@link PlayerModel#setupAnim}. Changes made to the arms, legs, body and
     * head are copied to the outer layers (sleeves, pants, jacket and hat) afterwards.
     */
    public static class SetupAngles extends PlayerModelEvent
    {
        protected SetupAngles(Player player, PlayerModel<?> modelPlayer, float partialTicks)
        {
            super(player, modelPlayer, partialTicks);
        }

        public static class Post extends SetupAngles
        {
            public Post(Player player, PlayerModel<?> modelPlayer, float partialTicks)
            {
                super(player, modelPlayer, partialTicks);
            }
        }
    }

    /**
     * Fired just before and after the player model is drawn. The pose stack is in model space.
     * Transformations applied in {@link Pre} also apply to the layers rendered after the model.
     */
    public static class Render extends PlayerModelEvent
    {
        private final PoseStack matrixStack;
        private final VertexConsumer builder;
        private final int light;
        private final int overlay;

        protected Render(Player player, PlayerModel<?> modelPlayer, PoseStack matrixStack, VertexConsumer builder, int light, int overlay, float partialTicks)
        {
            super(player, modelPlayer, partialTicks);
            this.matrixStack = matrixStack;
            this.builder = builder;
            this.light = light;
            this.overlay = overlay;
        }

        public PoseStack getMatrixStack()
        {
            return this.matrixStack;
        }

        public VertexConsumer getBuilder()
        {
            return this.builder;
        }

        public int getLight()
        {
            return this.light;
        }

        public int getOverlay()
        {
            return this.overlay;
        }

        public static class Pre extends Render
        {
            public Pre(Player player, PlayerModel<?> modelPlayer, PoseStack matrixStack, VertexConsumer builder, int light, int overlay, float partialTicks)
            {
                super(player, modelPlayer, matrixStack, builder, light, overlay, partialTicks);
            }
        }

        public static class Post extends Render
        {
            public Post(Player player, PlayerModel<?> modelPlayer, PoseStack matrixStack, VertexConsumer builder, int light, int overlay, float partialTicks)
            {
                super(player, modelPlayer, matrixStack, builder, light, overlay, partialTicks);
            }
        }
    }
}
