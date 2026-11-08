package com.mrcrayfish.vehicle;

import com.mrcrayfish.vehicle.item.ICreativeTabFiller;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import com.mrcrayfish.vehicle.client.ClientHandler;
import com.mrcrayfish.vehicle.common.CommonEvents;
import com.mrcrayfish.vehicle.common.FluidNetworkHandler;
import com.mrcrayfish.vehicle.common.ItemLookup;
import com.mrcrayfish.vehicle.common.data.SyncedPlayerData;
import com.mrcrayfish.vehicle.common.entity.HeldVehicleDataHandler;
import com.mrcrayfish.vehicle.crafting.VehicleRecipeTypes;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.init.ModBlocks;
import com.mrcrayfish.vehicle.init.ModContainers;
import com.mrcrayfish.vehicle.init.ModDataKeys;
import com.mrcrayfish.vehicle.init.ModEntities;
import com.mrcrayfish.vehicle.init.ModFluids;
import com.mrcrayfish.vehicle.init.ModItems;
import com.mrcrayfish.vehicle.init.ModLootFunctions;
import com.mrcrayfish.vehicle.init.ModRecipeSerializers;
import com.mrcrayfish.vehicle.init.ModSounds;
import com.mrcrayfish.vehicle.init.ModTileEntities;
import com.mrcrayfish.vehicle.network.PacketHandler;
import com.mrcrayfish.vehicle.util.ConfigFiles;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Author: MrCrayfish
 */
@Mod(Reference.MOD_ID)
public class VehicleMod
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TAB_REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Reference.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = TAB_REGISTER.register("creative_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Reference.MOD_ID))
            .icon(() -> new ItemStack(ModItems.IRON_SMALL_ENGINE.get()))
            .displayItems((parameters, output) -> {
                for(RegistryObject<Item> object : ModItems.REGISTER.getEntries())
                {
                    Item item = object.get();
                    ICreativeTabFiller filler = item instanceof ICreativeTabFiller f ? f : item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ICreativeTabFiller f ? f : null;
                    if(filler != null)
                    {
                        NonNullList<ItemStack> stacks = NonNullList.create();
                        filler.fillItemCategory(stacks);
                        stacks.forEach(output::accept);
                    }
                    else
                    {
                        output.accept(item);
                    }
                }
            }).build());

    public VehicleMod()
    {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.REGISTER.register(eventBus);
        ModItems.REGISTER.register(eventBus);
        ModEntities.REGISTER.register(eventBus);
        ModTileEntities.REGISTER.register(eventBus);
        ModContainers.REGISTER.register(eventBus);
        ModSounds.REGISTER.register(eventBus);
        ModRecipeSerializers.REGISTER.register(eventBus);
        VehicleRecipeTypes.REGISTER.register(eventBus);
        ModLootFunctions.REGISTER.register(eventBus);
        ModFluids.TYPE_REGISTER.register(eventBus);
        ModFluids.REGISTER.register(eventBus);
        TAB_REGISTER.register(eventBus);
        ConfigFiles.writeDefaults(Config.clientSpec, Reference.MOD_ID + "-client.toml", Config.serverSpec, Reference.MOD_ID + "-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onClientSetup);
        eventBus.addListener(HeldVehicleDataHandler::registerCapabilities);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.registerModEvents(eventBus));
        MinecraftForge.EVENT_BUS.register(new CommonEvents());
        MinecraftForge.EVENT_BUS.register(FluidNetworkHandler.instance());
        MinecraftForge.EVENT_BUS.register(SyncedPlayerData.instance());
        ModDataKeys.register();
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            VehicleProperties.loadProperties();
            ItemLookup.init();
        });
        PacketHandler.register();
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(ClientHandler::setup);
    }
}
