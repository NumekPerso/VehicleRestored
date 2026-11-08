package com.mrcrayfish.vehicle.client.model.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.ElementsModel;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads block models whose cubes are listed under "components" instead of "elements". The models
 * of the sports car, the quad bike and the compact helicopter were made for MrCrayfish's Framework
 * library ("framework:open_model"), which removes two limits of vanilla models: cubes can go past
 * the -16 to 32 range, and cubes can be rotated by any angle (vanilla only allows multiples of
 * 22.5 degrees). This loader reads the same format, so the mod does not need Framework.
 *
 * Use it in a model with: "loader": "vehicle_restored:open_model"
 */
public class OpenModelLoader implements IGeometryLoader<ElementsModel>
{
    public static final OpenModelLoader INSTANCE = new OpenModelLoader();

    private OpenModelLoader() {}

    @Override
    public ElementsModel read(JsonObject json, JsonDeserializationContext context) throws JsonParseException
    {
        String key = json.has("components") ? "components" : "elements";
        if(!json.has(key))
            throw new JsonParseException("An open model must have a \"components\" member");

        List<BlockElement> elements = new ArrayList<>();
        for(JsonElement element : GsonHelper.getAsJsonArray(json, key))
        {
            elements.add(readElement(GsonHelper.convertToJsonObject(element, "component")));
        }
        return new ElementsModel(elements);
    }

    private static BlockElement readElement(JsonObject json)
    {
        Vector3f from = readVector(json, "from");
        Vector3f to = readVector(json, "to");
        BlockElementRotation rotation = readRotation(json);
        Map<Direction, BlockElementFace> faces = readFaces(json);
        boolean shade = GsonHelper.getAsBoolean(json, "shade", true);
        return new BlockElement(from, to, faces, rotation, shade);
    }

    private static BlockElementRotation readRotation(JsonObject json)
    {
        if(!json.has("rotation"))
            return null;
        JsonObject object = GsonHelper.getAsJsonObject(json, "rotation");
        Vector3f origin = readVector(object, "origin");
        origin.mul(0.0625F);
        String axisName = GsonHelper.getAsString(object, "axis");
        Direction.Axis axis = Direction.Axis.byName(axisName.toLowerCase(Locale.ROOT));
        if(axis == null)
            throw new JsonParseException("Invalid rotation axis: " + axisName);
        float angle = GsonHelper.getAsFloat(object, "angle");
        boolean rescale = GsonHelper.getAsBoolean(object, "rescale", false);
        return new BlockElementRotation(origin, axis, angle, rescale);
    }

    private static Map<Direction, BlockElementFace> readFaces(JsonObject json)
    {
        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        JsonObject object = GsonHelper.getAsJsonObject(json, "faces");
        for(Map.Entry<String, JsonElement> entry : object.entrySet())
        {
            Direction direction = Direction.byName(entry.getKey());
            if(direction == null)
                throw new JsonParseException("Unknown face: " + entry.getKey());
            faces.put(direction, readFace(GsonHelper.convertToJsonObject(entry.getValue(), "face")));
        }
        if(faces.isEmpty())
            throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
        return faces;
    }

    private static BlockElementFace readFace(JsonObject json)
    {
        Direction cullFace = null;
        if(json.has("cullface"))
        {
            cullFace = Direction.byName(GsonHelper.getAsString(json, "cullface"));
        }
        int tintIndex = GsonHelper.getAsInt(json, "tintindex", -1);
        String texture = GsonHelper.getAsString(json, "texture");
        float[] uvs = null;
        if(json.has("uv"))
        {
            JsonArray array = GsonHelper.getAsJsonArray(json, "uv");
            if(array.size() != 4)
                throw new JsonParseException("Expected 4 uv values, found: " + array.size());
            uvs = new float[4];
            for(int i = 0; i < 4; i++)
            {
                uvs[i] = GsonHelper.convertToFloat(array.get(i), "uv[" + i + "]");
            }
        }
        int uvRotation = GsonHelper.getAsInt(json, "rotation", 0);
        if(uvRotation < 0 || uvRotation % 90 != 0 || uvRotation / 90 > 3)
            throw new JsonParseException("Invalid uv rotation " + uvRotation + " found, only 0/90/180/270 allowed");
        return new BlockElementFace(cullFace, tintIndex, texture, new BlockFaceUV(uvs, uvRotation));
    }

    private static Vector3f readVector(JsonObject json, String name)
    {
        JsonArray array = GsonHelper.getAsJsonArray(json, name);
        if(array.size() != 3)
            throw new JsonParseException("Expected 3 " + name + " values, found: " + array.size());
        return new Vector3f(GsonHelper.convertToFloat(array.get(0), name + "[0]"), GsonHelper.convertToFloat(array.get(1), name + "[1]"), GsonHelper.convertToFloat(array.get(2), name + "[2]"));
    }
}
