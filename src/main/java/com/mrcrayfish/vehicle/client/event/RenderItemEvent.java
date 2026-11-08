package com.mrcrayfish.vehicle.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events fired on the Forge event bus when an item held by an entity is rendered in third person.
 * They replace the item render events of the Obfuscate library, which the original mod used.
 */
public abstract class RenderItemEvent extends Event
{
    private final LivingEntity entity;
    private final ItemStack heldItem;
    private final ItemDisplayContext displayContext;
    private final PoseStack matrixStack;
    private final MultiBufferSource renderTypeBuffer;
    private final int light;

    protected RenderItemEvent(LivingEntity entity, ItemStack heldItem, ItemDisplayContext displayContext, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light)
    {
        this.entity = entity;
        this.heldItem = heldItem;
        this.displayContext = displayContext;
        this.matrixStack = matrixStack;
        this.renderTypeBuffer = renderTypeBuffer;
        this.light = light;
    }

    public LivingEntity getEntity()
    {
        return this.entity;
    }

    public ItemStack getItem()
    {
        return this.heldItem;
    }

    public ItemDisplayContext getDisplayContext()
    {
        return this.displayContext;
    }

    public PoseStack getMatrixStack()
    {
        return this.matrixStack;
    }

    public MultiBufferSource getRenderTypeBuffer()
    {
        return this.renderTypeBuffer;
    }

    public int getLight()
    {
        return this.light;
    }

    public static class Held extends RenderItemEvent
    {
        protected Held(LivingEntity entity, ItemStack heldItem, ItemDisplayContext displayContext, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light)
        {
            super(entity, heldItem, displayContext, matrixStack, renderTypeBuffer, light);
        }

        /**
         * Cancel to prevent the held item from rendering
         */
        @Cancelable
        public static class Pre extends Held
        {
            public Pre(LivingEntity entity, ItemStack heldItem, ItemDisplayContext displayContext, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light)
            {
                super(entity, heldItem, displayContext, matrixStack, renderTypeBuffer, light);
            }
        }
    }
}
