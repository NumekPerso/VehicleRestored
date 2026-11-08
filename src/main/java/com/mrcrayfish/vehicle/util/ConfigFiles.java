package com.mrcrayfish.vehicle.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.vehicle.VehicleMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the config files of the mod with all their default values before Forge loads them.
 *
 * When a config file does not exist, Forge creates an empty file and then fills it, logging a
 * warning for every missing value. Writing the complete file first avoids these warnings. The
 * server config is written to the "defaultconfigs" folder, which Forge copies into each new world.
 */
public class ConfigFiles
{
    public static void writeDefaults(ForgeConfigSpec clientSpec, String clientFileName, ForgeConfigSpec serverSpec, String serverFileName)
    {
        if(FMLEnvironment.dist.isClient())
        {
            writeIfMissing(clientSpec, FMLPaths.CONFIGDIR.get().resolve(clientFileName));
        }
        writeIfMissing(serverSpec, FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(serverFileName));
    }

    private static void writeIfMissing(ForgeConfigSpec spec, Path file)
    {
        if(Files.exists(file))
            return;

        try
        {
            Files.createDirectories(file.getParent());
            try(CommentedFileConfig config = CommentedFileConfig.builder(file, TomlFormat.instance()).sync().preserveInsertionOrder().writingMode(WritingMode.REPLACE).build())
            {
                spec.correct(config);
                config.save();
            }
        }
        catch(IOException | RuntimeException e)
        {
            VehicleMod.LOGGER.debug("Could not write the default config file {}", file, e);
        }
    }
}
