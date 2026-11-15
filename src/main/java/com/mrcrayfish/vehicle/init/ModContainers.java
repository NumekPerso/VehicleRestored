package com.mrcrayfish.vehicle.init;

import net.minecraft.world.flag.FeatureFlags;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.common.inventory.IStorage;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import com.mrcrayfish.vehicle.inventory.container.EditVehicleContainer;
import com.mrcrayfish.vehicle.inventory.container.FluidExtractorContainer;
import com.mrcrayfish.vehicle.inventory.container.FluidMixerContainer;
import com.mrcrayfish.vehicle.inventory.container.StorageContainer;
import com.mrcrayfish.vehicle.inventory.container.TankAmmoContainer;
import com.mrcrayfish.vehicle.inventory.container.AdvancedWorkshopContainer;
import com.mrcrayfish.vehicle.tileentity.AdvancedWorkshopTileEntity;
import net.minecraft.world.inventory.SimpleContainerData;
import com.mrcrayfish.vehicle.entity.vehicle.TankAmmo;
import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import net.minecraft.world.SimpleContainer;
import com.mrcrayfish.vehicle.inventory.container.WorkstationContainer;
import com.mrcrayfish.vehicle.tileentity.FluidExtractorTileEntity;
import com.mrcrayfish.vehicle.tileentity.FluidMixerTileEntity;
import com.mrcrayfish.vehicle.tileentity.WorkstationTileEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Author: MrCrayfish
 */
public class ModContainers
{
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Reference.MOD_ID);

    public static final RegistryObject<MenuType<FluidExtractorContainer>> FLUID_EXTRACTOR = register("fluid_extractor", (IContainerFactory<FluidExtractorContainer>) (windowId, playerInventory, data) -> {
        FluidExtractorTileEntity fluidExtractor = (FluidExtractorTileEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos());
        return new FluidExtractorContainer(windowId, playerInventory, fluidExtractor);
    });
    public static final RegistryObject<MenuType<FluidMixerContainer>> FLUID_MIXER = register("fluid_mixer", (IContainerFactory<FluidMixerContainer>) (windowId, playerInventory, data) -> {
        FluidMixerTileEntity fluidMixer = (FluidMixerTileEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos());
        return new FluidMixerContainer(windowId, playerInventory, fluidMixer);
    });
    public static final RegistryObject<MenuType<EditVehicleContainer>> EDIT_VEHICLE = register("edit_vehicle", (IContainerFactory<EditVehicleContainer>) (windowId, playerInventory, data) -> {
        PoweredVehicleEntity entity = (PoweredVehicleEntity) playerInventory.player.level().getEntity(data.readInt());
        return new EditVehicleContainer(windowId, entity.getVehicleInventory(), entity, playerInventory.player, playerInventory);
    });
    public static final RegistryObject<MenuType<WorkstationContainer>> WORKSTATION = register("workstation", (IContainerFactory<WorkstationContainer>) (windowId, playerInventory, data) -> {
        WorkstationTileEntity workstation = (WorkstationTileEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos());
        return new WorkstationContainer(windowId, playerInventory, workstation);
    });
    public static final RegistryObject<MenuType<StorageContainer>> STORAGE = register("storage", (IContainerFactory<StorageContainer>) (windowId, playerInventory, data) -> {
        IStorage storage = (IStorage) playerInventory.player.level().getEntity(data.readVarInt());
        return new StorageContainer(windowId, playerInventory, storage, playerInventory.player);
    });

    public static final RegistryObject<MenuType<TankAmmoContainer>> TANK_AMMO = register("tank_ammo", (IContainerFactory<TankAmmoContainer>) (windowId, playerInventory, data) -> {
        // the client gets the contents of the stores through the slots of the menu
        TankEntity tank = playerInventory.player.level().getEntity(data.readVarInt()) instanceof TankEntity t ? t : null;
        return new TankAmmoContainer(windowId, playerInventory, new SimpleContainer(TankAmmo.values().length * TankAmmo.SLOTS_PER_TYPE),
                new SimpleContainer(com.mrcrayfish.vehicle.entity.vehicle.TankArmorFace.values().length), tank);
    });

    public static final RegistryObject<MenuType<AdvancedWorkshopContainer>> ADVANCED_WORKSHOP = register("advanced_workshop", (IContainerFactory<AdvancedWorkshopContainer>) (windowId, playerInventory, data) -> {
        AdvancedWorkshopTileEntity workshop = (AdvancedWorkshopTileEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos());
        return new AdvancedWorkshopContainer(windowId, playerInventory, workshop, new SimpleContainerData(AdvancedWorkshopTileEntity.DATA_COUNT));
    });

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> register(String id, MenuType.MenuSupplier<T> factory)
    {
        return REGISTER.register(id, () -> new MenuType<>(factory, FeatureFlags.VANILLA_SET));
    }
}