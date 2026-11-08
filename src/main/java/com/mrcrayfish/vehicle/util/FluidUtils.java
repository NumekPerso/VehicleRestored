package com.mrcrayfish.vehicle.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.joml.Matrix4f;

/**
 * Author: MrCrayfish
 */
public class FluidUtils
{
    private static final Map<ResourceLocation, Integer> CACHE_FLUID_COLOR = new HashMap<>();

    @OnlyIn(Dist.CLIENT)
    public static void clearCacheFluidColor()
    {
        CACHE_FLUID_COLOR.clear();
    }

    @OnlyIn(Dist.CLIENT)
    public static int getAverageFluidColor(Fluid fluid)
    {
        Integer cachedColor = CACHE_FLUID_COLOR.get(ForgeRegistries.FLUIDS.getKey(fluid));
        if(cachedColor != null)
        {
            return cachedColor;
        }
        else
        {
            int fluidColor = -1;
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IClientFluidTypeExtensions.of(fluid).getStillTexture());
            if(sprite != null)
            {
                NativeImage image = sprite.contents().getOriginalImage();
                int spriteWidth = sprite.contents().width();
                int spriteHeight = sprite.contents().height();
                long totalRed = 0;
                long totalGreen = 0;
                long totalBlue = 0;
                int pixelCount = spriteWidth * spriteHeight;
                int red, green, blue;
                for(int i = 0; i < spriteHeight; i++)
                {
                    for(int j = 0; j < spriteWidth; j++)
                    {
                        int color = image.getPixelRGBA(j, i);
                        red = color & 255;
                        green = color >> 8 & 255;
                        blue = color >> 16 & 255;
                        totalRed += red * red;
                        totalGreen += green * green;
                        totalBlue += blue * blue;
                    }
                }
                fluidColor = (((int) Math.sqrt(totalRed / pixelCount) & 255) << 16) | (((int) Math.sqrt(totalGreen / pixelCount) & 255) << 8) | (((int) Math.sqrt(totalBlue / pixelCount) & 255));
            }
            CACHE_FLUID_COLOR.put(ForgeRegistries.FLUIDS.getKey(fluid), fluidColor);
            return fluidColor;
        }
    }

    public static int transferFluid(IFluidHandler source, IFluidHandler target, int maxAmount)
    {
        FluidStack drained = source.drain(maxAmount, IFluidHandler.FluidAction.SIMULATE);
        if(drained.getAmount() > 0)
        {
            int filled = target.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if(filled > 0)
            {
                drained = source.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                return target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return 0;
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawFluidTankInGUI(GuiGraphics graphics, FluidStack fluid, double x, double y, double percent, int height)
    {
        if(fluid == null || fluid.isEmpty())
            return;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid));
        if(sprite != null)
        {
            float minU = sprite.getU0();
            float maxU = sprite.getU1();
            float minV = sprite.getV0();
            float maxV = sprite.getV1();
            graphics.flush();
            float deltaV = maxV - minV;
            double tankLevel = percent * height;

            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Matrix4f matrix = graphics.pose().last().pose();

            RenderSystem.enableBlend();
            int count = 1 + ((int) Math.ceil(tankLevel)) / 16;
            for(int i = 0; i < count; i++)
            {
                double subHeight = Math.min(16.0, tankLevel - (16.0 * i));
                double offsetY = height - 16.0 * i - subHeight;
                drawQuad(matrix, x, y + offsetY, 16, subHeight, minU, (float) (maxV - deltaV * (subHeight / 16.0)), maxU, maxV);
            }
            RenderSystem.disableBlend();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawQuad(Matrix4f matrix, double x, double y, double width, double height, float minU, float minV, float maxU, float maxV)
    {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, (float) x, (float) (y + height), 0).uv(minU, maxV).endVertex();
        buffer.vertex(matrix, (float) (x + width), (float) (y + height), 0).uv(maxU, maxV).endVertex();
        buffer.vertex(matrix, (float) (x + width), (float) y, 0).uv(maxU, minV).endVertex();
        buffer.vertex(matrix, (float) x, (float) y, 0).uv(minU, minV).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawFluidInWorld(FluidTank tank, Level world, BlockPos pos, PoseStack matrixStack, MultiBufferSource renderTypeBuffer, float x, float y, float z, float width, float height, float depth, int light, FluidSides sides)
    {
        if(tank.isEmpty())
            return;

        FluidState fluidState = tank.getFluid().getFluid().defaultFluidState();
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluidState);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluidState, world, pos));
        int waterColor = extensions.getTintColor(fluidState, world, pos);
        float red = (float) (waterColor >> 16 & 255) / 255.0F;
        float green = (float) (waterColor >> 8 & 255) / 255.0F;
        float blue = (float) (waterColor & 255) / 255.0F;
        float side = 0.9F;
        float minU = sprite.getU0();
        float maxU = Math.min(minU + (sprite.getU1() - minU) * depth, sprite.getU1());
        float minV = sprite.getV0();
        float maxV = Math.min(minV + (sprite.getV1() - minV) * height, sprite.getV1());

        VertexConsumer buffer = renderTypeBuffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = matrixStack.last().pose();

        //left side
        if(sides.test(Direction.WEST))
        {
            buffer.vertex(matrix, x + width, y, z).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(maxU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x, y, z).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(minU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x, y + height, z).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(minU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y + height, z).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(maxU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
        }

        //right side
        if(sides.test(Direction.EAST))
        {
            buffer.vertex(matrix, x, y, z + depth).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(maxU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y, z + depth).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(minU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y + height, z + depth).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(minU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x, y + height, z + depth).color(red - 0.25F, green - 0.25F, blue - 0.25F, 1.0F).uv(maxU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
        }

        maxU = Math.min(minU + (sprite.getU1() - minU) * depth, sprite.getU1());

        if(sides.test(Direction.SOUTH))
        {
            buffer.vertex(matrix, x + width, y, z + depth).color(red * side, green * side, blue * side, 1.0F).uv(maxU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y, z).color(red * side, green * side, blue * side, 1.0F).uv(minU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y + height, z).color(red * side, green * side, blue * side, 1.0F).uv(minU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y + height, z + depth).color(red * side, green * side, blue * side, 1.0F).uv(maxU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
        }

        if(sides.test(Direction.NORTH))
        {
            buffer.vertex(matrix, x, y, z).color(red * side, green * side, blue * side, 1.0F).uv(minU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x, y, z + depth).color(red * side, green * side, blue * side, 1.0F).uv(maxU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x, y + height, z + depth).color(red * side, green * side, blue * side, 1.0F).uv(maxU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x, y + height, z).color(red * side, green * side, blue * side, 1.0F).uv(minU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
        }

        maxV = Math.min(minV + (sprite.getV1() - minV) * width, sprite.getV1());

        if(sides.test(Direction.UP))
        {
            buffer.vertex(matrix, x, y + height, z).color(red, green, blue, 1.0F).uv(maxU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x, y + height, z + depth).color(red, green, blue, 1.0F).uv(minU, minV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y + height, z + depth).color(red, green, blue, 1.0F).uv(minU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, x + width, y + height, z).color(red, green, blue, 1.0F).uv(maxU, maxV).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
        }
    }

    public static class FluidSides
    {
        private final EnumMap<Direction, Boolean> map = new EnumMap<>(Direction.class);

        public FluidSides(Direction ... sides)
        {
            Stream.of(Direction.values()).forEach(direction -> this.map.put(direction, false));
            Stream.of(sides).forEach(direction -> this.map.put(direction, true));
        }

        public boolean test(Direction direction)
        {
            return this.map.get(direction);
        }
    }
}
