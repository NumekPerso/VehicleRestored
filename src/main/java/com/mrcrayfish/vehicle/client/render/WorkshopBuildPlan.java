package com.mrcrayfish.vehicle.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrcrayfish.vehicle.Reference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * How the advanced workshop builds a miniature point by point, and the miniature itself ready to draw.
 * <p>
 * The miniature (drawn once in the frame of the build table: blocks, origin at the middle of the table
 * top, y up) is recorded quad by quad and cut along a grid: layers of {@link #LAYER} units, square cells
 * of {@link #CELL} units. Each cell the miniature goes through is a point the laser builds; the points
 * go row by row, back and forth (the rows one way on a layer, the other way on the next), layer after
 * layer from the bottom. Each piece of the miniature belongs to the point of its cell.
 * <p>
 * Drawing it costs little: the layers already made and the layers still to make are drawn from buffers
 * on the graphics card (made once, drawn after the block entities, see {@link #onRenderLevelStage}); only
 * the layer being made is sent each frame, its points made as they are and the others as the hologram.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class WorkshopBuildPlan
{
    public static final float CELL = 2.0F, LAYER = 1.0F;
    private static final float UNIT = 1F / 16F;
    private static final Map<Object, WorkshopBuildPlan> CACHE = new HashMap<>();
    /** The layers to draw from the graphics card this frame, queued by the renderer */
    private static final List<Queued> QUEUE = new ArrayList<>();
    /** Builds the buffers for the graphics card (one for all the plans: its memory is kept) */
    private static final BufferBuilder BUILDER = new BufferBuilder(1 << 16);

    /** Draws a miniature with the given pose (blocks, the frame of the table) into the given buffers */
    @FunctionalInterface
    public interface Drawing
    {
        void draw(PoseStack pose, MultiBufferSource buffers);
    }

    /** The hologram: its render type, its colour and the point of the texture it takes (a plain white one) */
    public record Hologram(RenderType type, int argb, float u, float v) {}

    private record Queued(WorkshopBuildPlan plan, Matrix4f matrix, int layer, Hologram hologram) {}

    /* The grid */
    private final float minX, minY, minZ;
    private final int columns, rows, layers;
    /* The points in building order: layer << 20 | row << 10 | column, and for each: the top of the miniature in its cell
       (y), and the box of the miniature in it (x0, x1, z0, z1), model units */
    private final int[] points;
    private final float[] tops, boxes;
    /** The first point of each layer (and the count at the end) */
    private final int[] layerStart;
    /* The pieces, in the order of their points: the first vertex of each, its vertex count, its render type; the first
       piece of each point (and the count at the end) */
    private final int[] pieceStart, pieceSize, pieceType, pointPieces;
    private final List<RenderType> types;
    /* The vertices: position (blocks, frame of the table), texture, normal, colour, overlay, light */
    private final float[] position, texture, normal;
    private final int[] colour, overlay, light;
    /* Buffers on the graphics card, made on first use: per layer, the pieces as they are (by render type) and as the
       hologram */
    @Nullable
    private List<Map<RenderType, VertexBuffer>> madeBuffers;
    @Nullable
    private VertexBuffer[] hologramBuffers;
    @Nullable
    private Hologram hologramOfBuffers;

    private WorkshopBuildPlan(float minX, float minY, float minZ, int columns, int rows, int layers, int[] points, float[] tops, float[] boxes, int[] layerStart,
                              int[] pieceStart, int[] pieceSize, int[] pieceType, int[] pointPieces, List<RenderType> types,
                              float[] position, float[] texture, float[] normal, int[] colour, int[] overlay, int[] light)
    {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.columns = columns; this.rows = rows; this.layers = layers;
        this.points = points; this.tops = tops; this.boxes = boxes; this.layerStart = layerStart;
        this.pieceStart = pieceStart; this.pieceSize = pieceSize; this.pieceType = pieceType; this.pointPieces = pointPieces; this.types = types;
        this.position = position; this.texture = texture; this.normal = normal; this.colour = colour; this.overlay = overlay; this.light = light;
    }

    /** The key of the plan of a vehicle (when it has a renderer) or of an item and its count */
    public static Object keyOf(@Nullable EntityType<?> vehicle, ItemStack item)
    {
        return vehicle != null ? vehicle : List.of(item.getItem(), item.getCount());
    }

    /** The plan of a miniature, made from its drawing the first time */
    public static WorkshopBuildPlan get(Object key, Drawing drawing)
    {
        return CACHE.computeIfAbsent(key, k -> make(drawing));
    }

    /** Forgets the plans and frees their buffers, for example when the resources (models, atlas) are reloaded */
    public static void clearCache()
    {
        RenderSystem.assertOnRenderThread();
        CACHE.values().forEach(WorkshopBuildPlan::freeBuffers);
        CACHE.clear();
        QUEUE.clear();
    }

    public int count()
    {
        return this.points.length;
    }

    public int layer(int index)
    {
        return this.points[index] >>> 20;
    }

    /** The top of the miniature in the cell of a point (model units, frame of the table) */
    public float top(int index)
    {
        return this.tops[index];
    }

    /** The box of the miniature in the cell of a point: x0, x1, z0, z1 (model units, frame of the table) */
    public float[] box(int index)
    {
        return Arrays.copyOfRange(this.boxes, index * 4, index * 4 + 4);
    }

    /* ------------------------------------------------------------------ drawing */

    /**
     * Draws the miniature made up to the point `current` (the points before it as they are, the others as
     * the hologram), with the pose of the frame of the table (blocks). `deferred`: the whole layers are
     * drawn from the graphics card after the block entities (in a world); otherwise everything is sent
     * now (the image exporter of the development client).
     */
    public void render(PoseStack pose, MultiBufferSource buffers, int current, Hologram hologram, boolean deferred)
    {
        if(this.count() == 0)
            return;
        current = Math.max(0, Math.min(this.count(), current));
        int layer = current < this.count() ? this.layer(current) : this.layers;
        PoseStack.Pose last = pose.last();
        if(deferred)
        {
            QUEUE.add(new Queued(this, new Matrix4f(last.pose()), layer, hologram));
            if(QUEUE.size() > 256)
                QUEUE.remove(0);
        }
        else
        {
            this.emitPoints(last, buffers, 0, this.layerStart[layer], null);
            this.emitPoints(last, buffers, layer + 1 < this.layerStart.length ? this.layerStart[Math.min(this.layers, layer + 1)] : this.count(), this.count(), hologram);
        }
        // the layer being made: its points made as they are, the others as the hologram
        if(layer < this.layers)
        {
            this.emitPoints(last, buffers, this.layerStart[layer], current, null);
            this.emitPoints(last, buffers, current, this.layerStart[layer + 1], hologram);
        }
    }

    /** Sends the pieces of the points [from, to) now: as they are (hologram null), or as the hologram */
    private void emitPoints(PoseStack.Pose pose, MultiBufferSource buffers, int from, int to, @Nullable Hologram hologram)
    {
        if(from >= to)
            return;
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        VertexConsumer consumer = hologram != null ? buffers.getBuffer(hologram.type()) : null;
        int lastType = -1;
        for(int piece = this.pointPieces[from]; piece < this.pointPieces[to]; piece++)
        {
            if(hologram == null && this.pieceType[piece] != lastType)
            {
                lastType = this.pieceType[piece];
                consumer = buffers.getBuffer(this.types.get(lastType));
            }
            this.emitPiece(consumer, matrix, normalMatrix, piece, hologram);
        }
    }

    /** A piece as quads (a fan; a triangle repeats its last point) */
    private void emitPiece(VertexConsumer consumer, @Nullable Matrix4f matrix, @Nullable Matrix3f normalMatrix, int piece, @Nullable Hologram hologram)
    {
        int start = this.pieceStart[piece], size = this.pieceSize[piece];
        for(int i = 1; i + 1 < size; i += 2)
        {
            int c = start + i + 2 < start + size ? start + i + 2 : start + i + 1;
            this.emitVertex(consumer, matrix, normalMatrix, start, hologram);
            this.emitVertex(consumer, matrix, normalMatrix, start + i, hologram);
            this.emitVertex(consumer, matrix, normalMatrix, start + i + 1, hologram);
            this.emitVertex(consumer, matrix, normalMatrix, c, hologram);
        }
    }

    private void emitVertex(VertexConsumer consumer, @Nullable Matrix4f matrix, @Nullable Matrix3f normalMatrix, int v, @Nullable Hologram hologram)
    {
        float x = this.position[v * 3], y = this.position[v * 3 + 1], z = this.position[v * 3 + 2];
        float nx = this.normal[v * 3], ny = this.normal[v * 3 + 1], nz = this.normal[v * 3 + 2];
        if(matrix != null)
            consumer.vertex(matrix, x, y, z);
        else
            consumer.vertex(x, y, z);
        if(hologram != null)
        {
            consumer.color(hologram.argb() >> 16 & 255, hologram.argb() >> 8 & 255, hologram.argb() & 255, hologram.argb() >>> 24).uv(hologram.u(), hologram.v())
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT);
        }
        else
        {
            int c = this.colour[v];
            consumer.color(c >> 16 & 255, c >> 8 & 255, c & 255, c >>> 24).uv(this.texture[v * 2], this.texture[v * 2 + 1]).overlayCoords(this.overlay[v]).uv2(this.light[v]);
        }
        if(normalMatrix != null)
            consumer.normal(normalMatrix, nx, ny, nz);
        else
            consumer.normal(nx, ny, nz);
        consumer.endVertex();
    }

    /** Draws the whole layers queued this frame from the graphics card: after the block entities, before the translucent blocks */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || QUEUE.isEmpty())
            return;
        Matrix4f view = RenderSystem.getModelViewMatrix();
        for(Queued queued : QUEUE)
        {
            WorkshopBuildPlan plan = queued.plan();
            if(!CACHE.containsValue(plan))
                continue;
            plan.makeBuffers(queued.hologram());
            Matrix4f matrix = new Matrix4f(view).mul(queued.matrix());
            // the layers made, by render type
            Map<RenderType, List<VertexBuffer>> made = new LinkedHashMap<>();
            for(int layer = 0; layer < queued.layer(); layer++)
                plan.madeBuffers.get(layer).forEach((type, buffer) -> made.computeIfAbsent(type, t -> new ArrayList<>()).add(buffer));
            made.forEach((type, list) -> draw(type, list, matrix));
            // the layers to make, as the hologram
            List<VertexBuffer> hologram = new ArrayList<>();
            for(int layer = queued.layer() + 1; layer < plan.layers; layer++)
                if(plan.hologramBuffers[layer] != null)
                    hologram.add(plan.hologramBuffers[layer]);
            draw(queued.hologram().type(), hologram, matrix);
        }
        QUEUE.clear();
        VertexBuffer.unbind();
    }

    private static void draw(RenderType type, List<VertexBuffer> buffers, Matrix4f matrix)
    {
        if(buffers.isEmpty())
            return;
        type.setupRenderState();
        for(VertexBuffer buffer : buffers)
        {
            buffer.bind();
            buffer.drawWithShader(matrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        }
        type.clearRenderState();
    }

    /** Makes the buffers on the graphics card (once, or again if the hologram changed) */
    private void makeBuffers(Hologram hologram)
    {
        if(this.madeBuffers != null && hologram.equals(this.hologramOfBuffers))
            return;
        this.freeBuffers();
        this.hologramOfBuffers = hologram;
        this.madeBuffers = new ArrayList<>();
        this.hologramBuffers = new VertexBuffer[this.layers];
        BufferBuilder builder = BUILDER;
        for(int layer = 0; layer < this.layers; layer++)
        {
            int from = this.layerStart[layer], to = this.layerStart[layer + 1];
            Map<RenderType, VertexBuffer> byType = new IdentityHashMap<>();
            for(int type = 0; type < this.types.size(); type++)
            {
                // each render type in its own vertex format (what its shader reads)
                builder.begin(VertexFormat.Mode.QUADS, this.types.get(type).format());
                for(int piece = this.pointPieces[from]; piece < this.pointPieces[to]; piece++)
                    if(this.pieceType[piece] == type)
                        this.emitPiece(builder, null, null, piece, null);
                VertexBuffer buffer = upload(builder);
                if(buffer != null)
                    byType.put(this.types.get(type), buffer);
            }
            this.madeBuffers.add(byType);
            builder.begin(VertexFormat.Mode.QUADS, hologram.type().format());
            for(int piece = this.pointPieces[from]; piece < this.pointPieces[to]; piece++)
                this.emitPiece(builder, null, null, piece, hologram);
            this.hologramBuffers[layer] = upload(builder);
        }
    }

    @Nullable
    private static VertexBuffer upload(BufferBuilder builder)
    {
        BufferBuilder.RenderedBuffer rendered = builder.endOrDiscardIfEmpty();
        if(rendered == null)
            return null;
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(rendered);
        VertexBuffer.unbind();
        return buffer;
    }

    private void freeBuffers()
    {
        if(this.madeBuffers != null)
            this.madeBuffers.forEach(map -> map.values().forEach(VertexBuffer::close));
        if(this.hologramBuffers != null)
            for(VertexBuffer buffer : this.hologramBuffers)
                if(buffer != null)
                    buffer.close();
        this.madeBuffers = null;
        this.hologramBuffers = null;
        this.hologramOfBuffers = null;
    }

    /* ------------------------------------------------------------------ making a plan */

    /** A vertex while the plan is made: position (model units), texture, normal, colour, overlay, light */
    private static final class V
    {
        float x, y, z, u, v, nx, ny = 1, nz;
        int colour = -1, overlay = OverlayTexture.NO_OVERLAY, light = LightTexture.FULL_BRIGHT;

        V copy()
        {
            V c = new V();
            c.x = this.x; c.y = this.y; c.z = this.z; c.u = this.u; c.v = this.v; c.nx = this.nx; c.ny = this.ny; c.nz = this.nz;
            c.colour = this.colour; c.overlay = this.overlay; c.light = this.light;
            return c;
        }

        float get(int axis)
        {
            return axis == 0 ? this.x : axis == 1 ? this.y : this.z;
        }

        static V lerp(V p, V q, float t)
        {
            V c = p.copy();
            c.x += (q.x - p.x) * t; c.y += (q.y - p.y) * t; c.z += (q.z - p.z) * t;
            c.u += (q.u - p.u) * t; c.v += (q.v - p.v) * t;
            c.nx += (q.nx - p.nx) * t; c.ny += (q.ny - p.ny) * t; c.nz += (q.nz - p.nz) * t;
            return c;
        }
    }

    private record Quad(int type, V[] vertices) {}

    private record Piece(int cell, int type, List<V> vertices) {}

    private static WorkshopBuildPlan make(Drawing drawing)
    {
        // 1. the quads of the miniature, as drawn
        Recorder recorder = new Recorder();
        try
        {
            drawing.draw(new PoseStack(), recorder);
        }
        catch(Exception e)
        {
            // a drawing that cannot be recorded is shown whole
        }
        List<Quad> quads = recorder.quads;
        if(quads.isEmpty())
        {
            return new WorkshopBuildPlan(0, 0, 0, 1, 1, 0, new int[0], new float[0], new float[0], new int[]{0}, new int[0], new int[0], new int[0], new int[]{0},
                    recorder.types, new float[0], new float[0], new float[0], new int[0], new int[0], new int[0]);
        }

        // 2. the grid round it, centred
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for(Quad quad : quads)
        {
            for(V v : quad.vertices())
            {
                minX = Math.min(minX, v.x); maxX = Math.max(maxX, v.x);
                minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
                minZ = Math.min(minZ, v.z); maxZ = Math.max(maxZ, v.z);
            }
        }
        int columns = Math.min(1000, Math.max(1, (int) Math.ceil((maxX - minX) / CELL - 1e-4)));
        int rows = Math.min(1000, Math.max(1, (int) Math.ceil((maxZ - minZ) / CELL - 1e-4)));
        int layers = Math.min(4000, Math.max(1, (int) Math.ceil((maxY - minY) / LAYER - 1e-4)));
        minX -= (columns * CELL - (maxX - minX)) / 2F;
        minZ -= (rows * CELL - (maxZ - minZ)) / 2F;

        // 3. the quads cut along the grid: pieces, each in one cell
        List<Piece> pieces = new ArrayList<>();
        float[] origin = {minX, minY, minZ}, size = {CELL, LAYER, CELL};
        int[] counts = {columns, layers, rows};
        for(Quad quad : quads)
        {
            List<int[]> cellsOf = new ArrayList<>();
            List<List<V>> parts = new ArrayList<>();
            cellsOf.add(new int[3]);
            parts.add(new ArrayList<>(Arrays.asList(quad.vertices())));
            for(int axis = 0; axis < 3; axis++)
            {
                List<int[]> nextCells = new ArrayList<>();
                List<List<V>> nextParts = new ArrayList<>();
                for(int k = 0; k < parts.size(); k++)
                {
                    List<V> part = parts.get(k);
                    float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
                    for(V v : part)
                    {
                        lo = Math.min(lo, v.get(axis));
                        hi = Math.max(hi, v.get(axis));
                    }
                    int first = clamp((int) Math.floor((lo - origin[axis]) / size[axis]), counts[axis]);
                    int last = clamp((int) Math.floor((hi - origin[axis]) / size[axis] - 1e-5), counts[axis]);
                    for(int i = first; i <= last; i++)
                    {
                        float a = i == 0 ? -Float.MAX_VALUE : origin[axis] + i * size[axis];
                        float b = i == counts[axis] - 1 ? Float.MAX_VALUE : origin[axis] + (i + 1) * size[axis];
                        List<V> cut = first == last ? part : clip(clip(part, axis, a, true), axis, b, false);
                        if(cut.size() < 3)
                            continue;
                        int[] cell = cellsOf.get(k).clone();
                        cell[axis] = i;
                        nextCells.add(cell);
                        nextParts.add(cut);
                    }
                }
                cellsOf = nextCells;
                parts = nextParts;
            }
            for(int k = 0; k < parts.size(); k++)
            {
                int[] c = cellsOf.get(k);
                pieces.add(new Piece((c[1] * rows + c[2]) * columns + c[0], quad.type(), parts.get(k)));
            }
        }

        // 4. the cells with pieces are the points: their order, the top and the box of what is in them
        int cellCount = layers * rows * columns;
        float[] cellTop = new float[cellCount], cellBox = new float[cellCount * 4];
        boolean[] used = new boolean[cellCount];
        for(Piece piece : pieces)
        {
            int c = piece.cell();
            for(V v : piece.vertices())
            {
                if(!used[c])
                {
                    used[c] = true;
                    cellTop[c] = v.y;
                    cellBox[c * 4] = v.x; cellBox[c * 4 + 1] = v.x; cellBox[c * 4 + 2] = v.z; cellBox[c * 4 + 3] = v.z;
                }
                cellTop[c] = Math.max(cellTop[c], v.y);
                cellBox[c * 4] = Math.min(cellBox[c * 4], v.x); cellBox[c * 4 + 1] = Math.max(cellBox[c * 4 + 1], v.x);
                cellBox[c * 4 + 2] = Math.min(cellBox[c * 4 + 2], v.z); cellBox[c * 4 + 3] = Math.max(cellBox[c * 4 + 3], v.z);
            }
        }
        int[] pointOfCell = new int[cellCount];
        Arrays.fill(pointOfCell, -1);
        int[] order = new int[cellCount];
        float[] tops = new float[cellCount], boxes = new float[cellCount * 4];
        int[] layerStart = new int[layers + 1];
        int count = 0;
        for(int l = 0; l < layers; l++)
        {
            layerStart[l] = count;
            for(int k = 0; k < rows; k++)
            {
                int r = l % 2 == 0 ? k : rows - 1 - k;
                boolean plusX = k % 2 == 0;
                for(int j = 0; j < columns; j++)
                {
                    int c = plusX ? j : columns - 1 - j;
                    int cell = (l * rows + r) * columns + c;
                    if(used[cell])
                    {
                        pointOfCell[cell] = count;
                        order[count] = l << 20 | r << 10 | c;
                        tops[count] = cellTop[cell];
                        System.arraycopy(cellBox, cell * 4, boxes, count * 4, 4);
                        count++;
                    }
                }
            }
        }
        layerStart[layers] = count;

        // 5. the pieces in the order of their points (by render type within a point), in flat arrays
        pieces.sort((a, b) -> pointOfCell[a.cell()] != pointOfCell[b.cell()] ? Integer.compare(pointOfCell[a.cell()], pointOfCell[b.cell()]) : Integer.compare(a.type(), b.type()));
        int vertexCount = 0;
        for(Piece piece : pieces)
            vertexCount += piece.vertices().size();
        int[] pieceStart = new int[pieces.size()], pieceSize = new int[pieces.size()], pieceType = new int[pieces.size()], pointPieces = new int[count + 1];
        float[] position = new float[vertexCount * 3], texture = new float[vertexCount * 2], normal = new float[vertexCount * 3];
        int[] colour = new int[vertexCount], overlay = new int[vertexCount], light = new int[vertexCount];
        int v = 0, point = 0;
        for(int i = 0; i < pieces.size(); i++)
        {
            Piece piece = pieces.get(i);
            int p = pointOfCell[piece.cell()];
            while(point <= p)
                pointPieces[point++] = i;
            pieceStart[i] = v;
            pieceSize[i] = piece.vertices().size();
            pieceType[i] = piece.type();
            for(V x : piece.vertices())
            {
                position[v * 3] = x.x * UNIT; position[v * 3 + 1] = x.y * UNIT; position[v * 3 + 2] = x.z * UNIT;
                texture[v * 2] = x.u; texture[v * 2 + 1] = x.v;
                normal[v * 3] = x.nx; normal[v * 3 + 1] = x.ny; normal[v * 3 + 2] = x.nz;
                colour[v] = x.colour; overlay[v] = x.overlay; light[v] = x.light;
                v++;
            }
        }
        while(point <= count)
            pointPieces[point++] = pieces.size();
        return new WorkshopBuildPlan(minX, minY, minZ, columns, rows, layers, Arrays.copyOf(order, count), Arrays.copyOf(tops, count), Arrays.copyOf(boxes, count * 4),
                layerStart, pieceStart, pieceSize, pieceType, pointPieces, recorder.types, position, texture, normal, colour, overlay, light);
    }

    /** The part of a polygon on one side of the plane `axis` = `value` (above it, or below it) */
    private static List<V> clip(List<V> polygon, int axis, float value, boolean keepAbove)
    {
        if(value == Float.MAX_VALUE || value == -Float.MAX_VALUE)
            return polygon;
        List<V> out = new ArrayList<>(polygon.size() + 2);
        for(int i = 0; i < polygon.size(); i++)
        {
            V p = polygon.get(i), q = polygon.get((i + 1) % polygon.size());
            float sp = keepAbove ? value - p.get(axis) : p.get(axis) - value, sq = keepAbove ? value - q.get(axis) : q.get(axis) - value;
            if(sp <= 0)
                out.add(p);
            if((sp <= 0) != (sq <= 0))
                out.add(V.lerp(p, q, sp / (sp - sq)));
        }
        return out;
    }

    private static int clamp(int value, int size)
    {
        return Math.max(0, Math.min(size - 1, value));
    }

    /** Records the quads of a drawing (positions in blocks, kept in model units) with their render type */
    private static class Recorder implements MultiBufferSource
    {
        final List<Quad> quads = new ArrayList<>();
        final List<RenderType> types = new ArrayList<>();
        private final Map<RenderType, Consumer> consumers = new IdentityHashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderType type)
        {
            return this.consumers.computeIfAbsent(type, t -> {
                this.types.add(t);
                return new Consumer(this.types.size() - 1, t.mode() == VertexFormat.Mode.QUADS);
            });
        }

        private class Consumer implements VertexConsumer
        {
            private final int type;
            private final boolean quads;
            private final V[] quad = new V[4];
            private int count;
            private boolean defaultColour;
            private int defaultArgb;

            Consumer(int type, boolean quads)
            {
                this.type = type;
                this.quads = quads;
            }

            @Override
            public VertexConsumer vertex(double x, double y, double z)
            {
                V v = new V();
                v.x = (float) x * 16F; v.y = (float) y * 16F; v.z = (float) z * 16F;
                if(this.defaultColour)
                    v.colour = this.defaultArgb;
                this.quad[this.count] = v;
                return this;
            }

            @Override
            public VertexConsumer color(int red, int green, int blue, int alpha)
            {
                this.quad[this.count].colour = alpha << 24 | red << 16 | green << 8 | blue;
                return this;
            }

            @Override
            public VertexConsumer uv(float u, float v)
            {
                this.quad[this.count].u = u;
                this.quad[this.count].v = v;
                return this;
            }

            @Override
            public VertexConsumer overlayCoords(int u, int v)
            {
                this.quad[this.count].overlay = u & 0xFFFF | v << 16;
                return this;
            }

            @Override
            public VertexConsumer uv2(int u, int v)
            {
                // the miniature is lit by the lights inside the machine: full bright
                this.quad[this.count].light = LightTexture.FULL_BRIGHT;
                return this;
            }

            @Override
            public VertexConsumer normal(float x, float y, float z)
            {
                V v = this.quad[this.count];
                v.nx = x; v.ny = y; v.nz = z;
                return this;
            }

            @Override
            public void endVertex()
            {
                if(++this.count == 4)
                {
                    this.count = 0;
                    if(this.quads)
                        Recorder.this.quads.add(new Quad(this.type, this.quad.clone()));
                }
            }

            @Override
            public void defaultColor(int red, int green, int blue, int alpha)
            {
                this.defaultColour = true;
                this.defaultArgb = alpha << 24 | red << 16 | green << 8 | blue;
            }

            @Override
            public void unsetDefaultColor()
            {
                this.defaultColour = false;
            }
        }
    }
}
