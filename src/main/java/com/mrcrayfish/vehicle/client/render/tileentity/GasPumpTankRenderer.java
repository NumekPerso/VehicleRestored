package com.mrcrayfish.vehicle.client.render.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.block.RotatedObjectBlock;
import com.mrcrayfish.vehicle.client.render.Axis;
import com.mrcrayfish.vehicle.init.ModBlocks;
import com.mrcrayfish.vehicle.tileentity.GasPumpTankTileEntity;
import com.mrcrayfish.vehicle.util.FluidUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * Author: MrCrayfish
 */
public class GasPumpTankRenderer implements BlockEntityRenderer<GasPumpTankTileEntity>
{
    private final BlockEntityRenderDispatcher renderer;

    /* After the rotation below, the glass of the 1.12.2 pump model faces the x sides of the fluid box,
     * which FluidUtils.drawFluidInWorld names NORTH (x) and SOUTH (x + width) */
    private static final FluidUtils.FluidSides FLUID_SIDES = new FluidUtils.FluidSides(Direction.NORTH, Direction.SOUTH, Direction.UP);

    public GasPumpTankRenderer(BlockEntityRendererProvider.Context context)
    {
        this.renderer = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(GasPumpTankTileEntity gasPump, float partialTicks, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light, int overlay)
    {
        Level world = gasPump.getLevel();
        BlockState state = gasPump.getBlockState();
        if(state.getBlock() != ModBlocks.GAS_PUMP.get())
            return;

        FluidTank tank = gasPump.getFluidTank();
        if(tank.isEmpty())
            return;

        matrixStack.pushPose();
        Direction direction = state.getValue(RotatedObjectBlock.DIRECTION);
        matrixStack.translate(0.5, 0.5, 0.5);
        matrixStack.mulPose(Axis.POSITIVE_Y.rotationDegrees(direction.get2DDataValue() * -90F - 90F));
        matrixStack.translate(-0.5, -0.5, -0.5);
        // The fluid fills the glass box of the 1.12.2 pump model (x 4-12, y 3-14, z 3-13 in the
        // model), slightly inset so it stays behind the glass. The box of the 0.45.2 model stuck out
        // of the glass by one pixel at the front, at the back and at the top.
        float height = 10.98F * (tank.getFluidAmount() / (float) tank.getCapacity());
        FluidUtils.drawFluidInWorld(tank, world, gasPump.getBlockPos(), matrixStack, renderTypeBuffer, 3.05F * 0.0625F, 3.01F * 0.0625F, 4.01F * 0.0625F, 9.9F * 0.0625F, height * 0.0625F, 7.98F * 0.0625F, light, FLUID_SIDES);
        matrixStack.popPose();
    }
}
