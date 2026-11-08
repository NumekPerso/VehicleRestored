package com.mrcrayfish.vehicle.client.model;

import com.mrcrayfish.vehicle.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.fml.ModList;

/**
 * Author: MrCrayfish
 */
public enum SpecialModels implements ISpecialModel
{
    ATV_BODY("atv_body"),
    ATV_HANDLES("atv_handles"),
    DUNE_BUGGY_BODY("dune_buggy_body"),
    DUNE_BUGGY_HANDLES("dune_buggy_handles"),
    GO_KART_BODY("go_kart_body"),
    GO_KART_STEERING_WHEEL("go_kart_steering_wheel"),
    SHOPPING_CART_BODY("shopping_cart_body"),
    MINI_BIKE_BODY("mini_bike_body"),
    MINI_BIKE_HANDLES("mini_bike_handles"),
    BUMPER_CAR_BODY("bumper_car_body"),
    JET_SKI_BODY("jet_ski_body"),
    SPEED_BOAT_BODY("speed_boat_body"),
    ALUMINUM_BOAT_BODY("aluminum_boat_body"),
    SMART_CAR_BODY("smart_car_body"),
    LAWN_MOWER_BODY("lawn_mower_body"),
    MOPED_BODY("moped_body"),
    MOPED_MUD_GUARD("moped_mud_guard"),
    MOPED_HANDLES("moped_handles"),
    SPORTS_PLANE("sports_plane_body"),
    SPORTS_PLANE_WING("sports_plane_wing"),
    SPORTS_PLANE_WHEEL_COVER("sports_plane_wheel_cover"),
    SPORTS_PLANE_LEG("sports_plane_leg"),
    SPORTS_PLANE_PROPELLER("sports_plane_propeller"),
    GOLF_CART_BODY("golf_cart_body"),
    OFF_ROADER_BODY("off_roader_body"),
    TRACTOR("tractor_body"),
    MINI_BUS_BODY("mini_bus_body"),
    DIRT_BIKE_BODY("dirt_bike_body"),
    DIRT_BIKE_HANDLES("dirt_bike_handles"),
    VEHICLE_TRAILER("trailer_body"),
    STORAGE_TRAILER("trailer_chest_body"),
    SEEDER_TRAILER("trailer_seeder_body"),
    FERTILIZER_TRAILER("trailer_fertilizer_body"),
    FLUID_TRAILER("trailer_fluid_body"),

    /* Vehicles from the 1.16.X dev branch (several use the vehicle_restored:open_model loader) */
    QUAD_BIKE_BODY("quad_bike/base"),
    QUAD_BIKE_HANDLES("quad_bike/handles"),
    SPORTS_CAR_BODY("sports_car/base"),
    SPORTS_CAR_STEERING_WHEEL("sports_car/steering_wheel"),
    SPORTS_CAR_HOOD("sports_car/cosmetics/hood"),
    SPORTS_CAR_LEFT_DOOR("sports_car/cosmetics/left_door"),
    SPORTS_CAR_RIGHT_DOOR("sports_car/cosmetics/right_door"),
    SPORTS_CAR_BOOT("sports_car/cosmetics/boot"),
    SPORTS_CAR_DASHBOARD("sports_car/cosmetics/dashboard"),
    SPORTS_CAR_ROOF("sports_car/cosmetics/roof"),
    SPORTS_CAR_SEAT("sports_car/cosmetics/seat"),
    COMPACT_HELICOPTER_BODY("helicopter/base"),
    COMPACT_HELICOPTER_SEAT("helicopter/cosmetics/seat"),
    COMPACT_HELICOPTER_JOYSTICK("helicopter/cosmetics/joystick"),
    COMPACT_HELICOPTER_BLADES("helicopter/cosmetics/blades"),
    COMPACT_HELICOPTER_TAIL_ROTOR("helicopter/cosmetics/tail_rotor"),

    VEHICLE_CRATE_SIDE("vehicle_crate_panel_side"),
    VEHICLE_CRATE_TOP("vehicle_crate_panel_top"),
    JACK_PISTON_HEAD("jack_piston_head"),
    SEED_SPIKER("seed_spiker"),
    NOZZLE("nozzle"),
    TOW_BAR("tow_bar"),
    BIG_TOW_BAR("big_tow_bar"),
    FUEL_DOOR_CLOSED("fuel_door_closed"),
    FUEL_DOOR_OPEN("fuel_door_open"),
    SMALL_FUEL_DOOR_CLOSED("small_fuel_door_closed"),
    SMALL_FUEL_DOOR_OPEN("small_fuel_door_open"),
    KEY_HOLE("key_hole"),
    SOFA_HELICOPTER_ARM("sofa_helicopter_arm"),
    SOFA_HELICOPTER_SKID("sofa_helicopter_skid"),

    /* Mod dependent models (Furniture Restored item models) */
    RED_SOFA(furnitureModel("red_sofa")),
    BATH(furnitureModel("bath")),
    CEILING_FAN_FANS(furnitureModel("ceiling_fan_fans"));

    // Add spray can lid
    /**
     * The location of an item model in the [MOD_ID]/models/vehicle/[NAME] folder
     */
    private final ResourceLocation modelLocation;

    /**
     * The mod that must be loaded for this model to be registered, or null if it is always registered
     */
    private final String requiredModId;

    /**
     * Cached model
     */
    @OnlyIn(Dist.CLIENT)
    private BakedModel cachedModel;

    /**
     * Sets the model's location
     *
     * @param modelName name of the model file
     */
    SpecialModels(String modelName)
    {
        this(new ResourceLocation(Reference.MOD_ID, "vehicle/" + modelName), null);
    }

    /**
     * Sets the model's location to an item model of Furniture Restored
     *
     * @param resource the model location
     */
    SpecialModels(ModelResourceLocation resource)
    {
        this(resource, Reference.FURNITURE_MOD_ID);
    }

    SpecialModels(ResourceLocation resource, String requiredModId)
    {
        this.modelLocation = resource;
        this.requiredModId = requiredModId;
    }

    private static ModelResourceLocation furnitureModel(String name)
    {
        return new ModelResourceLocation(new ResourceLocation(Reference.FURNITURE_MOD_ID, name), "inventory");
    }

    /**
     * Gets the model
     *
     * @return isolated model
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public BakedModel getModel()
    {
        if(this.cachedModel == null)
        {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(this.modelLocation);
            if(model == Minecraft.getInstance().getModelManager().getMissingModel())
            {
                return model;
            }
            this.cachedModel = model;
        }
        return this.cachedModel;
    }

    /**
     * Registers all the special models so they get loaded and baked. Mod dependent models are only
     * registered if the mod they come from is loaded. Registered from ClientHandler on the mod bus.
     */
    @OnlyIn(Dist.CLIENT)
    public static void register(ModelEvent.RegisterAdditional event)
    {
        for(SpecialModels model : values())
        {
            if(model.requiredModId == null || ModList.get().isLoaded(model.requiredModId))
            {
                event.register(model.modelLocation);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void clearModelCache()
    {
        for(SpecialModels model : values())
        {
            model.cachedModel = null;
        }
    }
}
