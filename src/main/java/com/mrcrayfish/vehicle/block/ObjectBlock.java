package com.mrcrayfish.vehicle.block;

import com.mrcrayfish.vehicle.util.RenderUtil;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Author: MrCrayfish
 */
public class ObjectBlock extends Block
{
    public ObjectBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    /**
     * Equivalent of BaseEntityBlock#createTickerHelper for blocks that implement EntityBlock directly
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(BlockEntityType<A> type, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker)
    {
        return expectedType == type ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter reader, List<Component> list, TooltipFlag flag)
    {
        if(Screen.hasShiftDown())
        {
            list.addAll(RenderUtil.lines(Component.translatable(this.getDescriptionId() + ".info"), 150));
        }
        else
        {
            list.add(Component.translatable("vehicle_restored.info_help").withStyle(ChatFormatting.YELLOW));
        }
    }
}
