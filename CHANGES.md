1. Ported the 0.45.2 version of the Vehicle Mod (Minecraft 1.16.5) to Minecraft 1.20.1 and Forge 47.
2. Replaced the Obfuscate library with code inside the mod: the data synced on players (trailer being hitched, gas pump nozzle in hand) and the events used to pose the player model. The mod no longer needs any other mod.
3. Removed the support for the Controllable gamepad mod.
4. Added the sports car, the quad bike and the compact helicopter from the unreleased development version of the mod, adapted to the vehicle system of the released version, with their workstation recipes. Their openable doors, hood and trunk are not included.
5. Added a model loader that reads the model format of MrCrayfish's Framework library, used by the models of these three vehicles, so the mod does not need Framework.
6. The furniture vehicles (sofa, bath, sofacopter) now work with Furniture Restored instead of MrCrayfish's Furniture Mod, and use its sofa, bath and ceiling fan.
7. The bath now renders the bath of Furniture Restored. In 0.45.2 it showed the ATV body as a placeholder.
8. The sofacopter rotor now renders the blades of the ceiling fan of Furniture Restored, as in the 1.12.2 version.
9. Added the workstation recipes of the bath and the sofacopter, which were missing in 0.45.2, using the ingredients of the 1.12.2 version. The sofa vehicle is crafted with a red sofa.
10. The surfaces under the wheels are now recognised with block tags, because 1.20 removed block materials. The categories are the same as in 0.45.2.
11. The key bindings now show in the controls menu.
12. The speedometer is drawn as a HUD overlay.
13. The vehicles from the furniture mod are ignored instead of causing an error when a world that contains them is opened without Furniture Restored.
14. Fixed the vehicle crate forgetting who opened it after the world is reloaded.
15. Fixed a crash when a client joins a dedicated server, caused by the workstation ingredient not being registered.
16. Updated the recipes, loot tables, advancements, models and block tags to the 1.20.1 formats, and added the missing tags for mining with a pickaxe or an axe.
17. Removed the data generators of the original project; the resources they generated are kept as files.
18. Added a French translation of the mod, and the missing English texts.
19. Renamed the mod to Vehicle Restored, changed the mod ID from vehicle to vehicle_restored, and renamed the jar to VehicleRestored.
20. Changed the version number to 1.0.
21. Removed the update link, the logo, the issue tracker link and the website link of the original mod, and rewrote the mod information shown in the mods list.
22. Removed the README, the Forge credits and the Forge license of the original project.
23. The traffic cone, the workstation, the gas pump, its nozzle and the jerry cans use their models from the 1.12.2 version again, with the vanilla textures of 1.20.1. The gas pump screen shows the fill level and the fuel name again, as in 1.12.2.
24. The hitboxes of the traffic cone and the workstation follow their models.
25. Holding shift over a vehicle crate shows a turning 3D preview of the vehicle inside, with its color, engine and wheels.
26. The bath and the sofacopter have a vehicle crate in the creative tab. The sofa vehicle uses the red sofa, like the sofacopter, and is crafted with it.
27. The third person camera is further back for large vehicles (compact helicopter, sports plane, mini bus, off-roader, golf cart, tractor, sports car), with the distances of the development version of the mod.
28. Fixed the thin slits at the corners of the vehicle crate.
29. Fixed the missing particles when breaking a fuel drum.
30. Fixed a crash when a smart car, bumper car, dune buggy or go-kart was created with recent versions of Forge.
31. The mod writes its config files with their default values, so Forge no longer logs warnings when it creates them.
32. Fixed the bath not moving when driven: it has no engine, and the plane controls only accelerated with an engine installed.
33. Fixed endless chests with the moped: attaching a chest now takes it from the player's hand (except in creative mode).
34. A vehicle can only be picked up by its owner or by an operator, and only within reach. Trailers have no owner and can still be picked up by anyone.
35. Only the owner or a passenger of a vehicle can take its key out, and only within reach. A blank key no longer causes an error.
36. A player can only hitch a trailer to themselves, and only within reach.
37. The server now checks the network messages sent by the clients: each message can only travel in its own direction, picking up a vehicle, using its key, hitching a trailer and opening or attaching a chest need the player to be within reach, and invalid steering, power and helicopter values are rejected.
38. The workstation menu closes when the player is more than 8 blocks away or the workstation is gone, and the dye, engine and wheels inside it drop when it is broken.
39. The workstation slots only accept one item of the right type, also from hoppers and pipes, and crafting a vehicle uses one dye, one engine and one wheel instead of the whole stacks. Vehicles without an engine type no longer ask for an engine.
40. Fixed an error that could stop crafting at the workstation halfway through and lose the materials already taken, and fixed shift-clicking items in the workstation menu.
41. The storage menus of the trailers and the moped close when the player is more than 8 blocks away.
42. The lawn mower, the seeder and the fertilizer respect the spawn protection and the claim mods: they only change blocks where their driver is allowed to, and follow the mobGriefing rule when nobody drives them.
43. The seeder only plants on the server side, so it no longer leaves ghost crops on the client, and only uses a seed when the crop stays. It no longer picks saplings from a storage trailer.
