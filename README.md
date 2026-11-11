# Vehicle Restored

An unofficial port of **MrCrayfish's Vehicle Mod** to Minecraft 1.20.1 (Forge).

## Why this project

The last version of the Vehicle Mod was made for Minecraft 1.16.5. Vehicle Restored brings it to 1.20.1, starting from the last released version, 0.45.2, so that players can drive, sail and fly its vehicles in the current version of the game.

## What it adds

- **Land vehicles:** ATV, dune buggy, go-kart, shopping cart, mini bike, bumper car, smart car, lawn mower, moped, golf cart, off-roader, tractor, mini bus, dirt bike.
- **Boats:** jet ski, speed boat, aluminum boat.
- **Aircraft:** sports plane.
- **Trailers:** vehicle trailer, storage trailer, fluid trailer, seeder, fertilizer.
- **Three vehicles from the unreleased development version of the mod:** sports car, quad bike and compact helicopter.
- **Parts and tools:** engines in four tiers, wheels, keys, wrench, hammer, spray can, jerry cans, jack.
- **Fuel production:** fluid extractor, fluid mixer, fluid pipes and pumps, fuel drums, gas pump, and the fuelium, ender sap and blaze juice fluids.
- **The workstation** to build vehicles, the vehicle crate, and traffic cones you can wear as a hat.
- **With [Furniture Restored](https://github.com/NumekPerso/FurnitureRestored) installed:** a sofa you can drive, a flying bath and a sofacopter, as in the 1.12.2 version.

Vehicle Restored works on its own: it does not need the Obfuscate or Framework libraries that the original mod used.

Some features are not included: support for the Controllable gamepad mod, and the openable doors, hood and trunk of the sports car.

## Requirements

- Minecraft 1.20.1
- Forge 47.0.14 or later
- Optional: Furniture Restored 1.0 or later, for the furniture vehicles

The mod ID is `vehicle_restored`. Worlds made with the original Vehicle Mod cannot be opened in 1.20.1.

## Building

```
./gradlew build
```

The jar, `VehicleRestored-forge-1.20.1-1.3.jar`, is written to `build/libs`.

To test the furniture vehicles in the development client, put `FurnitureRestored-forge-1.20.1-1.0.jar` in the `libs` folder.

## Credits

**MrCrayfish** created the Vehicle Mod: its code, models, textures and sounds. All the vehicles in this project are his work. Phylogeny worked on the original mod. Vehicle Restored only ports it and fixes it for 1.20.1.

- Original mod: https://github.com/MrCrayfish/MrCrayfishVehicleMod
- MrCrayfish's website: https://mrcrayfish.com

Vehicle Restored is not affiliated with, or endorsed by, MrCrayfish. Please do not report problems with this version to him.

## License

This project is a modified version of MrCrayfish's Vehicle Mod, distributed under the GNU General Public License v3. See [LICENSE](LICENSE).

Modified in November 2026. The changes from the original 0.45.2 version are listed in [CHANGES.md](CHANGES.md).
