package com.mrcrayfish.vehicle.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.client.render.AbstractVehicleRenderer;
import com.mrcrayfish.vehicle.client.render.VehiclePreviews;
import com.mrcrayfish.vehicle.crafting.AdvancedWorkshopRecipe;
import com.mrcrayfish.vehicle.crafting.WorkstationIngredient;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.inventory.container.AdvancedWorkshopContainer;
import com.mrcrayfish.vehicle.tileentity.AdvancedWorkshopTileEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * Screen of the advanced workshop, in the look of the machine (background made by
 * tools/concepts/workshop/gui.js): the list of its recipes on the left (name and category); on the
 * right, in one column as wide as the inventory: the chosen recipe (a turning preview of the vehicle
 * or the item, its energy and time, its ingredients with how many are in the machine), the state of
 * the work with the start / cancel button, the bars of the phase and of the whole work, the input
 * slots, the output slot and the energy gauge, then the player's inventory.
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedWorkshopScreen extends AbstractContainerScreen<AdvancedWorkshopContainer>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(Reference.MOD_ID, "textures/gui/container/advanced_workshop.png");
    private static final int TEXTURE_SIZE = 512;

    /* Layout: the insides of the recessed panels of the background (GUI pixels) */
    private static final int LIST_X = 8, LIST_Y = 22, LIST_WIDTH = 97, ROW_HEIGHT = 24, VISIBLE_ROWS = 9;
    private static final int SCROLL_X = 108, SCROLL_Y = 22, SCROLL_WIDTH = 3, SCROLL_HEIGHT = 218;
    private static final int PREVIEW_X = 118, PREVIEW_Y = 24, PREVIEW_WIDTH = 52, PREVIEW_HEIGHT = 50;
    private static final int DETAIL_X = 175, DETAIL_WIDTH = 99, MATERIALS_Y = 57, MATERIAL_STEP = 20, MATERIALS_SHOWN = 5;
    private static final int STATUS_X = 115, STATUS_Y = 80, STATUS_WIDTH = 112;
    private static final int BUTTON_X = 230, BUTTON_Y = 79, BUTTON_WIDTH = 47, BUTTON_HEIGHT = 15;
    private static final int BAR_X = 116, BAR_WIDTH = 160, PHASE_BAR_Y = 98, PHASE_BAR_HEIGHT = 4, TOTAL_BAR_Y = 106, TOTAL_BAR_HEIGHT = 2;
    private static final int GAUGE_X = 267, GAUGE_Y = 115, GAUGE_WIDTH = 9, GAUGE_HEIGHT = 34;

    private static final int TEXT_LIGHT = 0xE6E8E4, TEXT_DIM = 0x9DA2A9, TEXT_DARK = 0x2B2E33;
    private static final int ROW = 0xFF33373D, ROW_HOVER = 0xFF3F444B, ROW_SELECTED = 0xFF5A4532, ACCENT = 0xFFD8701C;
    private static final int GOOD = 0x7CE07A, BAD = 0xF0705A;

    private Button button;
    private int scroll;
    private boolean draggingScroll;

    public AdvancedWorkshopScreen(AdvancedWorkshopContainer menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        this.imageWidth = 284;
        this.imageHeight = 249;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 116;
        this.inventoryLabelY = 155;
    }

    @Override
    protected void init()
    {
        super.init();
        this.button = this.addRenderableWidget(Button.builder(Component.empty(), b -> this.pressButton())
                .bounds(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.updateButton();
    }

    private AdvancedWorkshopTileEntity workshop()
    {
        return this.menu.getWorkshop();
    }

    private List<AdvancedWorkshopRecipe> recipes()
    {
        return this.minecraft != null && this.minecraft.level != null ? AdvancedWorkshopRecipe.list(this.minecraft.level.getRecipeManager()) : List.of();
    }

    /** The recipe shown: the one being made, else the one chosen */
    @Nullable
    private AdvancedWorkshopRecipe shownRecipe()
    {
        return this.menu.isCrafting() && this.workshop().getCraftingRecipe() != null ? this.workshop().getCraftingRecipe() : this.workshop().getSelectedRecipe();
    }

    private boolean canStart()
    {
        AdvancedWorkshopRecipe recipe = this.workshop().getSelectedRecipe();
        return !this.menu.isCrafting() && recipe != null && recipe.hasIngredients(this.workshop().getItems(), 0, AdvancedWorkshopTileEntity.INPUT_SLOTS);
    }

    private void pressButton()
    {
        if(this.minecraft != null && this.minecraft.gameMode != null)
        {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.menu.isCrafting() ? AdvancedWorkshopContainer.BUTTON_CANCEL : AdvancedWorkshopContainer.BUTTON_START);
        }
    }

    private void updateButton()
    {
        boolean crafting = this.menu.isCrafting();
        this.button.setMessage(Component.translatable(crafting ? "gui.vehicle_restored.advanced_workshop.cancel" : "gui.vehicle_restored.advanced_workshop.start"));
        this.button.active = crafting || this.canStart();
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        this.updateButton();
    }

    /* ------------------------------------------------------------------ drawing */

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.renderOwnTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        this.renderList(graphics, mouseX, mouseY);
        AdvancedWorkshopRecipe recipe = this.shownRecipe();
        if(recipe != null)
        {
            this.renderPreview(graphics, recipe, partialTicks);
            this.renderDetail(graphics, recipe);
        }
        this.renderBars(graphics, recipe);
    }

    /** The recipes, one per row: the result, its name and its category; the chosen one in orange */
    private void renderList(GuiGraphics graphics, int mouseX, int mouseY)
    {
        List<AdvancedWorkshopRecipe> recipes = this.recipes();
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, recipes.size() - VISIBLE_ROWS));
        ResourceLocation selected = this.workshop().getSelected();
        for(int row = 0; row < VISIBLE_ROWS && this.scroll + row < recipes.size(); row++)
        {
            AdvancedWorkshopRecipe recipe = recipes.get(this.scroll + row);
            int x = this.leftPos + LIST_X, y = this.topPos + LIST_Y + row * ROW_HEIGHT, x1 = x + LIST_WIDTH, y1 = y + ROW_HEIGHT - 1;
            boolean isSelected = recipe.getId().equals(selected);
            boolean hovered = !this.menu.isCrafting() && mouseX >= x && mouseX < x1 && mouseY >= y && mouseY < y1;
            graphics.fill(x, y, x1, y1, isSelected ? ROW_SELECTED : hovered ? ROW_HOVER : ROW);
            if(isSelected)
            {
                graphics.renderOutline(x, y, LIST_WIDTH, ROW_HEIGHT - 1, ACCENT);
            }
            graphics.renderItem(recipe.getResult(), x + 4, y + 3);
            graphics.drawString(this.font, fit(recipe.getDisplayName(), LIST_WIDTH - 26), x + 23, y + 3, isSelected ? 0xFFFFFF : TEXT_LIGHT, false);
            Component category = Component.translatable("gui.vehicle_restored.advanced_workshop.category." + recipe.getCategory());
            drawSmall(graphics, category, x + 23, y + 14, isSelected ? 0xF0C9A8 : TEXT_DIM, LIST_WIDTH - 26);
        }
        // scroll thumb, in the track right of the list
        boolean scrolls = recipes.size() > VISIBLE_ROWS;
        int thumbHeight = scrolls ? Math.max(14, SCROLL_HEIGHT * VISIBLE_ROWS / recipes.size()) : SCROLL_HEIGHT;
        int range = Math.max(1, recipes.size() - VISIBLE_ROWS);
        int x = this.leftPos + SCROLL_X, y = this.topPos + SCROLL_Y + (SCROLL_HEIGHT - thumbHeight) * this.scroll / range;
        if(scrolls)
        {
            graphics.fill(x, y, x + SCROLL_WIDTH, y + thumbHeight, 0xFF8C9199);
            graphics.fill(x, y, x + SCROLL_WIDTH, y + 1, 0xFFB5BAC1);
            graphics.fill(x, y + thumbHeight - 1, x + SCROLL_WIDTH, y + thumbHeight, 0xFF5D6268);
        }
    }

    /** The vehicle turning in the preview box, or the item, large */
    private void renderPreview(GuiGraphics graphics, AdvancedWorkshopRecipe recipe, float partialTicks)
    {
        int x = this.leftPos + PREVIEW_X, y = this.topPos + PREVIEW_Y;
        EntityType<?> vehicle = recipe.getVehicle();
        AbstractVehicleRenderer<?> renderer = vehicle != null ? VehiclePreviews.getRenderer(vehicle) : null;
        graphics.enableScissor(x, y, x + PREVIEW_WIDTH, y + PREVIEW_HEIGHT);
        if(renderer != null)
        {
            VehicleProperties properties = VehicleProperties.get(vehicle);
            VehiclePreviews.prepare(renderer, properties, null, ItemStack.EMPTY, true, ItemStack.EMPTY);
            VehiclePreviews.Bounds bounds = VehiclePreviews.getBounds(vehicle, renderer);
            float tilt = 20F, cos = Mth.cos(tilt * Mth.DEG_TO_RAD), sin = Mth.sin(tilt * Mth.DEG_TO_RAD);
            float width = bounds.radius() * 2F, height = (bounds.maxY() - bounds.minY()) * cos + bounds.radius() * 2F * sin;
            float scale = Math.min((PREVIEW_WIDTH - 4) / Math.max(width, 0.01F), (PREVIEW_HEIGHT - 4) / Math.max(height, 0.01F));
            float centreY = (bounds.maxY() + bounds.minY()) / 2F * cos;
            float rotation = (Util.getMillis() % 9000L) / 25F;
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + PREVIEW_WIDTH / 2F, y + PREVIEW_HEIGHT / 2F + centreY * scale, 150F);
            pose.mulPoseMatrix(new Matrix4f().scaling(-scale, -scale, -scale));
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-tilt));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-rotation));
            pose.translate(-bounds.centreX(), 0, -bounds.centreZ());
            VehiclePreviews.applyDisplayPosition(pose, properties);
            Lighting.setupForEntityInInventory();
            renderer.setupTransformsAndRender(null, pose, graphics.bufferSource(), partialTicks, 15728880);
            graphics.flush();
            Lighting.setupFor3DItems();
            pose.popPose();
        }
        else
        {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + PREVIEW_WIDTH / 2F - 20, y + PREVIEW_HEIGHT / 2F - 20, 0);
            pose.scale(2.5F, 2.5F, 1F);
            graphics.renderItem(recipe.getResult(), 0, 0);
            pose.popPose();
        }
        graphics.disableScissor();
    }

    /** Name, energy and time, and the ingredients with their counts (green when the machine has them) */
    private void renderDetail(GuiGraphics graphics, AdvancedWorkshopRecipe recipe)
    {
        int x = this.leftPos + DETAIL_X;
        List<FormattedCharSequence> lines = this.font.split(recipe.getDisplayName(), DETAIL_WIDTH);
        for(int i = 0; i < Math.min(2, lines.size()); i++)
        {
            graphics.drawString(this.font, lines.get(i), x, this.topPos + 25 + i * 10, 0xFFFFFF, false);
        }
        Component info = Component.translatable("gui.vehicle_restored.advanced_workshop.info", recipe.getEnergyPerTick(), seconds(recipe.getTotalTicks()));
        drawSmall(graphics, info, x, this.topPos + 47, TEXT_DIM, DETAIL_WIDTH);

        List<WorkstationIngredient> materials = recipe.getMaterials();
        for(int i = 0; i < Math.min(MATERIALS_SHOWN, materials.size()); i++)
        {
            WorkstationIngredient ingredient = materials.get(i);
            ItemStack[] items = ingredient.getItems();
            if(items.length == 0)
                continue;
            int ix = x + i * MATERIAL_STEP, iy = this.topPos + MATERIALS_Y;
            ItemStack shown = items[(int) ((Util.getMillis() / 1000L) % items.length)];
            graphics.renderItem(shown, ix, iy);
            int have = AdvancedWorkshopRecipe.count(this.workshop().getItems(), 0, AdvancedWorkshopTileEntity.INPUT_SLOTS, ingredient);
            String count = String.valueOf(ingredient.getCount());
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, 0, 200);
            graphics.drawString(this.font, count, ix + 17 - this.font.width(count), iy + 9, have >= ingredient.getCount() || this.menu.isCrafting() ? GOOD : BAD, true);
            pose.popPose();
        }
    }

    /** The bar of the phase, the bar of the whole work, and the energy gauge (filled from the bottom) */
    private void renderBars(GuiGraphics graphics, @Nullable AdvancedWorkshopRecipe recipe)
    {
        int x = this.leftPos + BAR_X;
        if(this.menu.isCrafting() && recipe != null && this.menu.getTotal() > 0)
        {
            int[] phase = recipe.getPhaseProgress(this.menu.getProgress());
            int phaseWidth = Math.round(Mth.clamp(phase[0] / (float) Math.max(1, phase[1]), 0F, 1F) * BAR_WIDTH);
            if(phaseWidth > 0)
            {
                graphics.fill(x, this.topPos + PHASE_BAR_Y, x + phaseWidth, this.topPos + PHASE_BAR_Y + PHASE_BAR_HEIGHT, ACCENT);
                graphics.fill(x, this.topPos + PHASE_BAR_Y, x + phaseWidth, this.topPos + PHASE_BAR_Y + 1, 0xFFF2A15A);
            }
            int totalWidth = Math.round(Mth.clamp(this.menu.getProgress() / (float) this.menu.getTotal(), 0F, 1F) * BAR_WIDTH);
            if(totalWidth > 0)
            {
                graphics.fill(x, this.topPos + TOTAL_BAR_Y, x + totalWidth, this.topPos + TOTAL_BAR_Y + TOTAL_BAR_HEIGHT, 0xFF6FB6FF);
            }
        }
        int energy = Math.round(Mth.clamp(this.menu.getEnergy() / (float) AdvancedWorkshopTileEntity.ENERGY_CAPACITY, 0F, 1F) * GAUGE_HEIGHT);
        if(energy > 0)
        {
            int gx = this.leftPos + GAUGE_X, bottom = this.topPos + GAUGE_Y + GAUGE_HEIGHT;
            graphics.fill(gx, bottom - energy, gx + GAUGE_WIDTH, bottom, 0xFF5FCF5A);
            graphics.fill(gx, bottom - energy, gx + 1, bottom, 0xFF8EE58A);
            graphics.fill(gx, bottom - energy, gx + GAUGE_WIDTH, bottom - energy + 1, 0xFF8EE58A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_LIGHT, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_DARK, false);
        // state of the work, on two small lines left of the button
        Component first, second = null;
        int firstColour = TEXT_DARK;
        AdvancedWorkshopRecipe recipe = this.shownRecipe();
        if(this.menu.isCrafting() && recipe != null)
        {
            int index = recipe.getPhaseIndex(this.menu.getProgress());
            first = Component.translatable("gui.vehicle_restored.advanced_workshop.phase_of", index + 1, recipe.getPhases().size());
            if(this.menu.isBlocked())
            {
                first = Component.translatable("gui.vehicle_restored.advanced_workshop.status.output_full");
                firstColour = 0xB8321F;
            }
            else if(!this.menu.isPowered() && this.menu.getProgress() < this.menu.getTotal())
            {
                first = Component.translatable("gui.vehicle_restored.advanced_workshop.status.no_energy");
                firstColour = 0xB8321F;
            }
            second = recipe.getPhases().get(index).getDisplayName();
        }
        else if(this.workshop().getSelectedRecipe() == null)
        {
            first = Component.translatable("gui.vehicle_restored.advanced_workshop.status.choose");
        }
        else
        {
            boolean ready = this.canStart();
            first = Component.translatable(ready ? "gui.vehicle_restored.advanced_workshop.status.ready" : "gui.vehicle_restored.advanced_workshop.status.missing");
            firstColour = ready ? 0x2F7A2C : 0xB8321F;
        }
        if(second == null)
        {
            drawSmall(graphics, first, STATUS_X, STATUS_Y + 4, firstColour, STATUS_WIDTH);
        }
        else
        {
            drawSmall(graphics, first, STATUS_X, STATUS_Y, firstColour, STATUS_WIDTH);
            drawSmall(graphics, second, STATUS_X, STATUS_Y + 7, TEXT_DARK, STATUS_WIDTH);
        }
    }

    /** One line of text, cut with an ellipsis to the given width */
    private FormattedCharSequence fit(Component text, int width)
    {
        if(this.font.width(text) <= width)
        {
            return text.getVisualOrderText();
        }
        FormattedText cut = this.font.substrByWidth(text, Math.max(0, width - this.font.width("…")));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("…")));
    }

    /** Text at 3/4 size, cut to the given width (GUI pixels) */
    private void drawSmall(GuiGraphics graphics, Component text, int x, int y, int colour, int width)
    {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(0.75F, 0.75F, 1F);
        graphics.drawString(this.font, this.fit(text, Math.round(width / 0.75F)), 0, 0, colour, false);
        pose.popPose();
    }

    private static String seconds(int ticks)
    {
        return String.valueOf(Math.round(ticks / 20F));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Tooltips of the list, the ingredients, the bars and the energy gauge */
    private void renderOwnTooltips(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int left = this.leftPos, top = this.topPos;
        if(inside(mouseX, mouseY, left + LIST_X, top + LIST_Y, LIST_WIDTH, VISIBLE_ROWS * ROW_HEIGHT))
        {
            List<AdvancedWorkshopRecipe> recipes = this.recipes();
            int index = this.scroll + (mouseY - top - LIST_Y) / ROW_HEIGHT;
            if(index < recipes.size())
            {
                AdvancedWorkshopRecipe recipe = recipes.get(index);
                List<Component> lines = new ArrayList<>();
                lines.add(recipe.getDisplayName());
                lines.add(Component.translatable("gui.vehicle_restored.advanced_workshop.category." + recipe.getCategory()).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("gui.vehicle_restored.advanced_workshop.info", recipe.getEnergyPerTick(), seconds(recipe.getTotalTicks())).withStyle(ChatFormatting.DARK_GRAY));
                graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
            return;
        }
        AdvancedWorkshopRecipe recipe = this.shownRecipe();
        if(recipe != null)
        {
            List<WorkstationIngredient> materials = recipe.getMaterials();
            for(int i = 0; i < Math.min(MATERIALS_SHOWN, materials.size()); i++)
            {
                if(inside(mouseX, mouseY, left + DETAIL_X + i * MATERIAL_STEP, top + MATERIALS_Y, 16, 16))
                {
                    WorkstationIngredient ingredient = materials.get(i);
                    ItemStack[] items = ingredient.getItems();
                    if(items.length == 0)
                        return;
                    ItemStack shown = items[(int) ((Util.getMillis() / 1000L) % items.length)];
                    int have = AdvancedWorkshopRecipe.count(this.workshop().getItems(), 0, AdvancedWorkshopTileEntity.INPUT_SLOTS, ingredient);
                    graphics.renderComponentTooltip(this.font, List.of(shown.getHoverName(),
                            Component.translatable("gui.vehicle_restored.advanced_workshop.in_machine", have, ingredient.getCount()).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                    return;
                }
            }
            if(this.menu.isCrafting() && inside(mouseX, mouseY, left + BAR_X - 1, top + PHASE_BAR_Y - 1, BAR_WIDTH + 2, PHASE_BAR_HEIGHT + 2))
            {
                int[] phase = recipe.getPhaseProgress(this.menu.getProgress());
                graphics.renderTooltip(this.font, Component.translatable("gui.vehicle_restored.advanced_workshop.phase_time", seconds(phase[0]), seconds(phase[1])), mouseX, mouseY);
                return;
            }
            if(this.menu.isCrafting() && inside(mouseX, mouseY, left + BAR_X - 1, top + TOTAL_BAR_Y - 1, BAR_WIDTH + 2, TOTAL_BAR_HEIGHT + 2))
            {
                graphics.renderTooltip(this.font, Component.translatable("gui.vehicle_restored.advanced_workshop.total_time", seconds(this.menu.getProgress()), seconds(this.menu.getTotal())), mouseX, mouseY);
                return;
            }
        }
        if(inside(mouseX, mouseY, left + GAUGE_X - 1, top + GAUGE_Y - 1, GAUGE_WIDTH + 2, GAUGE_HEIGHT + 2))
        {
            graphics.renderTooltip(this.font, Component.translatable("gui.vehicle_restored.advanced_workshop.energy", String.format("%,d", this.menu.getEnergy()), String.format("%,d", AdvancedWorkshopTileEntity.ENERGY_CAPACITY)), mouseX, mouseY);
        }
    }

    /* ------------------------------------------------------------------ input */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int buttonId)
    {
        int left = this.leftPos, top = this.topPos;
        if(buttonId == 0 && this.recipes().size() > VISIBLE_ROWS && inside(mouseX, mouseY, left + SCROLL_X - 1, top + SCROLL_Y, SCROLL_WIDTH + 2, SCROLL_HEIGHT))
        {
            this.draggingScroll = true;
            this.scrollTo(mouseY);
            return true;
        }
        if(buttonId == 0 && !this.menu.isCrafting() && inside(mouseX, mouseY, left + LIST_X, top + LIST_Y, LIST_WIDTH, VISIBLE_ROWS * ROW_HEIGHT))
        {
            int index = this.scroll + (int) (mouseY - top - LIST_Y) / ROW_HEIGHT;
            if(index < this.recipes().size() && this.minecraft != null && this.minecraft.gameMode != null)
            {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, buttonId);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int buttonId, double dragX, double dragY)
    {
        if(this.draggingScroll)
        {
            this.scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, buttonId, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int buttonId)
    {
        this.draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, buttonId);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if(inside(mouseX, mouseY, this.leftPos + LIST_X, this.topPos + LIST_Y, SCROLL_X + SCROLL_WIDTH - LIST_X, SCROLL_HEIGHT))
        {
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(delta), 0, Math.max(0, this.recipes().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void scrollTo(double mouseY)
    {
        int range = Math.max(0, this.recipes().size() - VISIBLE_ROWS);
        float part = (float) ((mouseY - this.topPos - SCROLL_Y) / SCROLL_HEIGHT);
        this.scroll = Mth.clamp(Math.round(part * range), 0, range);
    }
}
