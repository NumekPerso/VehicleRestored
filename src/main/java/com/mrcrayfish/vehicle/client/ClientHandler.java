package com.mrcrayfish.vehicle.client;

import com.mrcrayfish.vehicle.client.tooltip.VehicleCrateTooltipComponent;
import com.mrcrayfish.vehicle.item.VehicleCrateItem;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.client.handler.CameraHandler;
import com.mrcrayfish.vehicle.client.handler.FuelingHandler;
import com.mrcrayfish.vehicle.client.handler.HeldVehicleHandler;
import com.mrcrayfish.vehicle.client.handler.InputHandler;
import com.mrcrayfish.vehicle.client.handler.OverlayHandler;
import com.mrcrayfish.vehicle.client.handler.PlayerModelHandler;
import com.mrcrayfish.vehicle.client.handler.SprayCanHandler;
import com.mrcrayfish.vehicle.client.model.SpecialModels;
import com.mrcrayfish.vehicle.client.render.AbstractVehicleRenderer;
import com.mrcrayfish.vehicle.client.render.EntityVehicleRenderer;
import com.mrcrayfish.vehicle.client.render.VehicleRenderRegistry;
import com.mrcrayfish.vehicle.client.render.tileentity.FluidExtractorRenderer;
import com.mrcrayfish.vehicle.client.render.tileentity.FluidPumpRenderer;
import com.mrcrayfish.vehicle.client.render.tileentity.FuelDrumRenderer;
import com.mrcrayfish.vehicle.client.render.tileentity.GasPumpRenderer;
import com.mrcrayfish.vehicle.client.render.tileentity.GasPumpTankRenderer;
import com.mrcrayfish.vehicle.client.render.tileentity.VehicleCrateRenderer;
import com.mrcrayfish.vehicle.client.render.vehicle.*;
import com.mrcrayfish.vehicle.client.screen.EditVehicleScreen;
import com.mrcrayfish.vehicle.client.screen.FluidExtractorScreen;
import com.mrcrayfish.vehicle.client.screen.FluidMixerScreen;
import com.mrcrayfish.vehicle.client.screen.StorageScreen;
import com.mrcrayfish.vehicle.client.screen.WorkstationScreen;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.init.ModBlocks;
import com.mrcrayfish.vehicle.init.ModContainers;
import com.mrcrayfish.vehicle.init.ModEntities;
import com.mrcrayfish.vehicle.init.ModFluids;
import com.mrcrayfish.vehicle.init.ModTileEntities;
import com.mrcrayfish.vehicle.item.PartItem;
import com.mrcrayfish.vehicle.item.SprayCanItem;
import com.mrcrayfish.vehicle.util.FluidUtils;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.nbt.Tag;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class ClientHandler
{
    /**
     * Registers the client listeners of the mod event bus. Called from the mod constructor on the
     * physical client only.
     *
     * @param modBus the mod event bus
     */
    public static void registerModEvents(IEventBus modBus)
    {
        modBus.addListener(ClientHandler::onRegisterRenderers);
        modBus.addListener(ClientHandler::onAddLayers);
        modBus.addListener(ClientHandler::onRegisterKeyMappings);
        modBus.addListener(ClientHandler::onRegisterAdditionalModels);
        modBus.addListener(ClientHandler::onBakingCompleted);
        modBus.addListener(ClientHandler::onRegisterItemColors);
        modBus.addListener(ClientHandler::onRegisterGuiOverlays);
        modBus.addListener(ClientHandler::onRegisterReloadListeners);
        modBus.addListener(ClientHandler::onRegisterTooltipComponents);
    }

    private static void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(VehicleCrateItem.Tooltip.class, VehicleCrateTooltipComponent::new);
    }

    /**
     * Called on the main thread during FMLClientSetupEvent
     */
    public static void setup()
    {
        MinecraftForge.EVENT_BUS.register(EntityRayTracer.instance());
        MinecraftForge.EVENT_BUS.register(new CameraHandler());
        MinecraftForge.EVENT_BUS.register(new FuelingHandler());
        MinecraftForge.EVENT_BUS.register(new HeldVehicleHandler());
        MinecraftForge.EVENT_BUS.register(new InputHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerModelHandler());
        MinecraftForge.EVENT_BUS.register(new SprayCanHandler());
        MinecraftForge.EVENT_BUS.register(new ClientEvents());

        setupRenderLayers();
        setupScreenFactories();
    }

    /**
     * The block models don't specify a "render_type", so the render layers are still set the
     * legacy (deprecated, but working) way, exactly like 1.16.5 did.
     */
    @SuppressWarnings("deprecation")
    private static void setupRenderLayers()
    {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.WORKSTATION.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLUID_EXTRACTOR.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GAS_PUMP.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModFluids.FUELIUM.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_FUELIUM.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModFluids.ENDER_SAP.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_ENDER_SAP.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModFluids.BLAZE_JUICE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_BLAZE_JUICE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.FUEL_DRUM.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.INDUSTRIAL_FUEL_DRUM.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TRAFFIC_CONE.get(), RenderType.cutout());
    }

    private static void setupScreenFactories()
    {
        MenuScreens.register(ModContainers.FLUID_EXTRACTOR.get(), FluidExtractorScreen::new);
        MenuScreens.register(ModContainers.FLUID_MIXER.get(), FluidMixerScreen::new);
        MenuScreens.register(ModContainers.EDIT_VEHICLE.get(), EditVehicleScreen::new);
        MenuScreens.register(ModContainers.WORKSTATION.get(), WorkstationScreen::new);
        MenuScreens.register(ModContainers.STORAGE.get(), StorageScreen::new);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        /* Register Vehicles */
        registerVehicleRenderer(event, ModEntities.ATV.get(), ATVRenderer::new);
        registerVehicleRenderer(event, ModEntities.DUNE_BUGGY.get(), DuneBuggyRenderer::new);
        registerVehicleRenderer(event, ModEntities.GO_KART.get(), GoKartRenderer::new);
        registerVehicleRenderer(event, ModEntities.SHOPPING_CART.get(), ShoppingCartRenderer::new);
        registerVehicleRenderer(event, ModEntities.MINI_BIKE.get(), MiniBikeRenderer::new);
        registerVehicleRenderer(event, ModEntities.BUMPER_CAR.get(), BumperCarModel::new);
        registerVehicleRenderer(event, ModEntities.JET_SKI.get(), JetSkiRenderer::new);
        registerVehicleRenderer(event, ModEntities.SPEED_BOAT.get(), SpeedBoatRenderer::new);
        registerVehicleRenderer(event, ModEntities.ALUMINUM_BOAT.get(), AluminumBoatRenderer::new);
        registerVehicleRenderer(event, ModEntities.SMART_CAR.get(), SmartCarRenderer::new);
        registerVehicleRenderer(event, ModEntities.LAWN_MOWER.get(), LawnMowerRenderer::new);
        registerVehicleRenderer(event, ModEntities.MOPED.get(), MopedRenderer::new);
        registerVehicleRenderer(event, ModEntities.SPORTS_PLANE.get(), SportsPlaneRenderer::new);
        registerVehicleRenderer(event, ModEntities.GOLF_CART.get(), GolfCartRenderer::new);
        registerVehicleRenderer(event, ModEntities.OFF_ROADER.get(), OffRoaderRenderer::new);
        registerVehicleRenderer(event, ModEntities.TRACTOR.get(), TractorRenderer::new);
        registerVehicleRenderer(event, ModEntities.MINI_BUS.get(), MiniBusRenderer::new);
        registerVehicleRenderer(event, ModEntities.DIRT_BIKE.get(), DirtBikeRenderer::new);
        registerVehicleRenderer(event, ModEntities.QUAD_BIKE.get(), QuadBikeRenderer::new);
        registerVehicleRenderer(event, ModEntities.SPORTS_CAR.get(), SportsCarRenderer::new);
        registerVehicleRenderer(event, ModEntities.COMPACT_HELICOPTER.get(), CompactHelicopterRenderer::new);

        /* Register Trailers */
        registerVehicleRenderer(event, ModEntities.VEHICLE_TRAILER.get(), VehicleTrailerRenderer::new);
        registerVehicleRenderer(event, ModEntities.STORAGE_TRAILER.get(), StorageTrailerRenderer::new);
        registerVehicleRenderer(event, ModEntities.FLUID_TRAILER.get(), FluidTrailerRenderer::new);
        registerVehicleRenderer(event, ModEntities.SEEDER.get(), SeederTrailerRenderer::new);
        registerVehicleRenderer(event, ModEntities.FERTILIZER.get(), FertilizerTrailerRenderer::new);

        /* Register Mod Exclusive Vehicles */
        if(ModList.get().isLoaded(Reference.FURNITURE_MOD_ID))
        {
            registerVehicleRenderer(event, ModEntities.SOFA.get(), SofaCarRenderer::new);
            registerVehicleRenderer(event, ModEntities.BATH.get(), BathModel::new);
            registerVehicleRenderer(event, ModEntities.SOFACOPTER.get(), SofaHelicopterRenderer::new);
        }

        event.registerEntityRenderer(ModEntities.JACK.get(), com.mrcrayfish.vehicle.client.render.JackRenderer::new);

        /* Register Block Entity Renderers */
        event.registerBlockEntityRenderer(ModTileEntities.FLUID_EXTRACTOR.get(), FluidExtractorRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.FUEL_DRUM.get(), FuelDrumRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.INDUSTRIAL_FUEL_DRUM.get(), FuelDrumRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.VEHICLE_CRATE.get(), VehicleCrateRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.JACK.get(), com.mrcrayfish.vehicle.client.render.tileentity.JackRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.GAS_PUMP.get(), GasPumpRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.GAS_PUMP_TANK.get(), GasPumpTankRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.FLUID_PUMP.get(), FluidPumpRenderer::new);
    }

    /**
     * Registers the entity renderer of a vehicle. The vehicle renderer (which needs the vehicle
     * properties, loaded during common setup) is created when the entity renderers are built,
     * at the end of the first resource reload.
     */
    private static <T extends VehicleEntity & EntityRayTracer.IEntityRayTraceable> void registerVehicleRenderer(EntityRenderersEvent.RegisterRenderers event, EntityType<T> type, Function<VehicleProperties, AbstractVehicleRenderer<T>> rendererFunction)
    {
        EntityRendererProvider<T> provider = context -> createVehicleRenderer(context, type, rendererFunction);
        event.registerEntityRenderer(type, provider);
    }

    private static <T extends VehicleEntity & EntityRayTracer.IEntityRayTraceable> EntityRenderer<T> createVehicleRenderer(EntityRendererProvider.Context context, EntityType<T> type, Function<VehicleProperties, AbstractVehicleRenderer<T>> rendererFunction)
    {
        VehicleProperties properties = VehicleProperties.get(type);
        AbstractVehicleRenderer<T> renderer = rendererFunction.apply(properties);
        VehicleRenderRegistry.registerVehicleRendererFunction(type, rendererFunction, renderer);

        EntityRayTracer.IRayTraceTransforms transforms = renderer.getRayTraceTransforms();
        if(transforms != null)
        {
            EntityRayTracer.instance().registerTransforms(type, transforms);
        }
        return new EntityVehicleRenderer<>(context, renderer);
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event)
    {
        HeldVehicleHandler.addLayers(event);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        KeyBinds.register(event);
    }

    private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event)
    {
        SpecialModels.register(event);
    }

    private static void onBakingCompleted(ModelEvent.BakingCompleted event)
    {
        SpecialModels.clearModelCache();
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event)
    {
        ItemColor color = (stack, index) ->
        {
            if(index == 0 && stack.hasTag() && stack.getTag().contains("Color", Tag.TAG_INT))
            {
                return stack.getTag().getInt("Color");
            }
            return 0xFFFFFF;
        };

        Item[] items = ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> item instanceof SprayCanItem || (item instanceof PartItem && ((PartItem) item).isColored()))
                .toArray(Item[]::new);
        event.register(color, items);
    }

    private static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event)
    {
        OverlayHandler.register(event);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener((PreparableReloadListener) (stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            return stage.wait(Unit.INSTANCE).thenRunAsync(() -> {
                FluidUtils.clearCacheFluidColor();
                VehicleCrateTooltipComponent.clearCache();
                EntityRayTracer.instance().clearDataForReregistration();
                SpecialModels.clearModelCache();
            }, gameExecutor);
        });
    }
}
