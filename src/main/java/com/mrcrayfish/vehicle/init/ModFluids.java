package com.mrcrayfish.vehicle.init;

import com.mrcrayfish.vehicle.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class ModFluids
{
    public static final DeferredRegister<FluidType> TYPE_REGISTER = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Reference.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> REGISTER = DeferredRegister.create(ForgeRegistries.FLUIDS, Reference.MOD_ID);

    public static final RegistryObject<FluidType> FUELIUM_TYPE = TYPE_REGISTER.register("fuelium", () -> createType("fuelium", 900, 900));
    public static final RegistryObject<FluidType> ENDER_SAP_TYPE = TYPE_REGISTER.register("ender_sap", () -> createType("ender_sap", 1000, 3000));
    public static final RegistryObject<FluidType> BLAZE_JUICE_TYPE = TYPE_REGISTER.register("blaze_juice", () -> createType("blaze_juice", 1000, 800));

    public static final RegistryObject<FlowingFluid> FUELIUM = REGISTER.register("fuelium", () -> new ForgeFlowingFluid.Source(ModFluids.fuelium()));
    public static final RegistryObject<FlowingFluid> FLOWING_FUELIUM = REGISTER.register("flowing_fuelium", () -> new ForgeFlowingFluid.Flowing(ModFluids.fuelium()));
    public static final RegistryObject<FlowingFluid> ENDER_SAP = REGISTER.register("ender_sap", () -> new ForgeFlowingFluid.Source(ModFluids.enderSap()));
    public static final RegistryObject<FlowingFluid> FLOWING_ENDER_SAP = REGISTER.register("flowing_ender_sap", () -> new ForgeFlowingFluid.Flowing(ModFluids.enderSap()));
    public static final RegistryObject<FlowingFluid> BLAZE_JUICE = REGISTER.register("blaze_juice", () -> new ForgeFlowingFluid.Source(ModFluids.blazeJuice()));
    public static final RegistryObject<FlowingFluid> FLOWING_BLAZE_JUICE = REGISTER.register("flowing_blaze_juice", () -> new ForgeFlowingFluid.Flowing(ModFluids.blazeJuice()));

    private static ForgeFlowingFluid.Properties fuelium()
    {
        return properties(FUELIUM_TYPE, FUELIUM, FLOWING_FUELIUM).bucket(ModItems.FUELIUM_BUCKET).block(ModBlocks.FUELIUM);
    }

    private static ForgeFlowingFluid.Properties enderSap()
    {
        return properties(ENDER_SAP_TYPE, ENDER_SAP, FLOWING_ENDER_SAP).bucket(ModItems.ENDER_SAP_BUCKET).block(ModBlocks.ENDER_SAP);
    }

    private static ForgeFlowingFluid.Properties blazeJuice()
    {
        return properties(BLAZE_JUICE_TYPE, BLAZE_JUICE, FLOWING_BLAZE_JUICE).bucket(ModItems.BLAZE_JUICE_BUCKET).block(ModBlocks.BLAZE_JUICE);
    }

    private static ForgeFlowingFluid.Properties properties(Supplier<FluidType> type, Supplier<FlowingFluid> still, Supplier<FlowingFluid> flowing)
    {
        return new ForgeFlowingFluid.Properties(type, still, flowing);
    }

    private static FluidType createType(String name, int density, int viscosity)
    {
        FluidType.Properties properties = FluidType.Properties.create()
                .descriptionId("block." + Reference.MOD_ID + "." + name)
                .density(density)
                .viscosity(viscosity)
                .canExtinguish(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
        return new FluidType(properties)
        {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer)
            {
                ResourceLocation still = new ResourceLocation(Reference.MOD_ID, "block/" + name + "_still");
                ResourceLocation flowing = new ResourceLocation(Reference.MOD_ID, "block/" + name + "_flowing");
                consumer.accept(new IClientFluidTypeExtensions()
                {
                    @Override
                    public ResourceLocation getStillTexture()
                    {
                        return still;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture()
                    {
                        return flowing;
                    }
                });
            }
        };
    }
}
