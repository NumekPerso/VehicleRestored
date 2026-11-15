package com.mrcrayfish.vehicle.block;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.init.ModBlocks;
import com.mrcrayfish.vehicle.init.ModItems;
import com.mrcrayfish.vehicle.init.ModTileEntities;
import com.mrcrayfish.vehicle.item.ICreativeTabFiller;
import com.mrcrayfish.vehicle.tileentity.MilitaryCrateTileEntity;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The crate of the military vehicles (the tank): a wooden transport crate of 2 x 2 cells, 1.5 blocks high,
 * painted olive drab, with steel corners, handles and latches and its markings (model and texture made by
 * tools/concepts/crate/crate.js). Each cell is a block of its own (PART); the main cell (front, left, bottom:
 * where the player clicked) holds the crate (MilitaryCrateTileEntity: the vehicle inside, like the vehicle crate).
 * Closed, the crate is the block model of the main cell, baked in the chunk. Opened with a wrench on its lid (at
 * once when placed in creative), its lid comes off, its four walls fall outwards one after the other and the
 * vehicle grows out of it (MilitaryCrateRenderer); then the crate is gone and the vehicle is there.
 */
public class MilitaryCrateBlock extends ObjectBlock implements EntityBlock, ICreativeTabFiller
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Cell of the crate: dx + 2 * dz + 4 * dy, dx to the right seen from the front, dz to the back, dy up */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    /** Opening: the block model is gone, the renderer draws the parts moving */
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");
    public static final int WIDTH = 2, DEPTH = 2, HEIGHT = 2, CELLS = WIDTH * DEPTH * HEIGHT;

    /** The vehicles that come in this crate */
    private static final Set<ResourceLocation> VEHICLES = Set.of(new ResourceLocation(Reference.MOD_ID, "tank"));

    /**
     * The volume of the crate, in model units (1/16 block) in the frame of its models (x to the right, z to the
     * front, origin at the left back bottom corner): closed, and once opened (the skids and the floor; the walls
     * lying around are only drawn)
     */
    private static final double[] CLOSED = {0, 0, 0, 32, 24, 32};
    private static final double[] OPEN = {0, 0, 0, 32, 5, 32};
    /** The crate seen from each cell (opened, facing, part): the outline and the collisions follow the crate */
    private static final VoxelShape[][][] SHAPES = new VoxelShape[2][4][CELLS];

    public MilitaryCrateBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).sound(SoundType.WOOD).strength(2.0F, 3.0F).noOcclusion()
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false).pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(OPENED, false));
    }

    /** True when this vehicle comes in the military crate */
    public static boolean carries(ResourceLocation vehicle)
    {
        return VEHICLES.contains(vehicle);
    }

    /** The military crate of a vehicle */
    public static ItemStack create(ResourceLocation vehicle, int color, ItemStack engine, ItemStack wheel)
    {
        return VehicleCrateBlock.createIn(ModBlocks.MILITARY_CRATE.get(), vehicle, color, engine, wheel);
    }

    @Override
    public void fillItemCategory(NonNullList<ItemStack> items)
    {
        for(ResourceLocation vehicle : VehicleCrateBlock.REGISTERED_CRATES)
        {
            if(!carries(vehicle))
                continue;
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.putString("Vehicle", vehicle.toString());
            blockEntityTag.putBoolean("Creative", true);
            CompoundTag itemTag = new CompoundTag();
            itemTag.put("BlockEntityTag", blockEntityTag);
            ItemStack stack = new ItemStack(this);
            stack.setTag(itemTag);
            items.add(stack);
        }
    }

    /* Cells: the same frame as the advanced workshop (main cell at the front left, the crate to its right and back) */

    /** Offset of a cell from the main cell, for a crate whose front faces `facing` */
    public static BlockPos offset(Direction facing, int part)
    {
        int dx = part % WIDTH, dz = (part / WIDTH) % DEPTH, dy = part / (WIDTH * DEPTH);
        Direction right = facing.getCounterClockWise(), back = facing.getOpposite();
        return new BlockPos(right.getStepX() * dx + back.getStepX() * dz, dy, right.getStepZ() * dx + back.getStepZ() * dz);
    }

    public static BlockPos getMainPos(BlockPos pos, BlockState state)
    {
        return pos.subtract(offset(state.getValue(FACING), state.getValue(PART)));
    }

    public static boolean isMain(BlockState state)
    {
        return state.getValue(PART) == 0;
    }

    /** Middle of the crate on the ground, from its main cell */
    public static Vec3 getCentre(BlockPos main, Direction facing)
    {
        Direction right = facing.getCounterClockWise(), back = facing.getOpposite();
        return new Vec3(main.getX() + 0.5 + (right.getStepX() + back.getStepX()) * 0.5, main.getY(), main.getZ() + 0.5 + (right.getStepZ() + back.getStepZ()) * 0.5);
    }

    /** A point of the models (blocks) in the world, for a crate whose main cell is at `main` */
    public static Vec3 toWorld(BlockPos main, Direction facing, double x, double y, double z)
    {
        double[] p = toMainCell(facing, x, z);
        return new Vec3(main.getX() + p[0], main.getY() + y, main.getZ() + p[1]);
    }

    /** A point of the models (blocks, x and z) in the frame of the main cell (the renderer's transform) */
    private static double[] toMainCell(Direction facing, double x, double z)
    {
        double angle = Math.toRadians(-facing.toYRot());
        double px = x - 0.5, pz = z - 1.5;
        return new double[]{px * Math.cos(angle) + pz * Math.sin(angle) + 0.5, -px * Math.sin(angle) + pz * Math.cos(angle) + 0.5};
    }

    private static VoxelShape getCrateShape(boolean opened, Direction facing, int part)
    {
        int open = opened ? 1 : 0, index = facing.get2DDataValue();
        if(SHAPES[open][index][part] == null)
        {
            double[] box = opened ? OPEN : CLOSED;
            BlockPos cell = offset(facing, part);
            double[] a = toMainCell(facing, box[0] / 16.0, box[2] / 16.0), b = toMainCell(facing, box[3] / 16.0, box[5] / 16.0);
            SHAPES[open][index][part] = Shapes.box(Math.min(a[0], b[0]) - cell.getX(), box[1] / 16.0 - cell.getY(), Math.min(a[1], b[1]) - cell.getZ(),
                    Math.max(a[0], b[0]) - cell.getX(), box[4] / 16.0 - cell.getY(), Math.max(a[1], b[1]) - cell.getZ());
        }
        return SHAPES[open][index][part];
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return getCrateShape(state.getValue(OPENED), state.getValue(FACING), state.getValue(PART));
    }

    /* Placing: the clicked cell is the front left bottom cell, the crate faces the player */

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos main = context.getClickedPos();
        for(int part = 1; part < CELLS; part++)
        {
            BlockPos pos = main.offset(offset(facing, part));
            if(pos.getY() >= level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos).canBeReplaced(context))
            {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, 0).setValue(OPENED, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        if(level.isClientSide())
            return;
        for(int part = 1; part < CELLS; part++)
        {
            level.setBlock(pos.offset(offset(state.getValue(FACING), part)), state.setValue(PART, part), Block.UPDATE_ALL);
        }
        // in creative the crate opens at once, like the vehicle crate
        if(placer instanceof Player player && player.isCreative())
        {
            this.openCrate(level, pos, level.getBlockState(pos), player);
        }
    }

    /* Opening: a wrench on the lid, from any cell */

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if(hit.getDirection() == Direction.UP && player.getItemInHand(hand).getItem() == ModItems.WRENCH.get())
        {
            BlockPos main = getMainPos(pos, state);
            BlockState mainState = level.getBlockState(main);
            if(mainState.is(this) && !level.isClientSide())
            {
                this.openCrate(level, main, mainState, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    /**
     * The walls fall onto the cells around the crate (one cell out on each side): they must be free, and the crate
     * not open yet
     */
    private boolean canOpen(Level level, BlockPos main, BlockState state)
    {
        if(state.getValue(OPENED))
            return false;
        Direction facing = state.getValue(FACING);
        Direction right = facing.getCounterClockWise(), back = facing.getOpposite();
        for(int a = -1; a <= WIDTH; a++)
        {
            for(int b = -1; b <= DEPTH; b++)
            {
                boolean ring = (a == -1 || a == WIDTH) != (b == -1 || b == DEPTH);
                if(!ring)
                    continue;
                BlockPos pos = main.offset(right.getStepX() * a + back.getStepX() * b, 0, right.getStepZ() * a + back.getStepZ() * b);
                BlockState other = level.getBlockState(pos);
                if(!other.isAir() && !other.canBeReplaced())
                    return false;
            }
        }
        return true;
    }

    private void openCrate(Level level, BlockPos main, BlockState state, Player player)
    {
        if(!this.canOpen(level, main, state) || !(level.getBlockEntity(main) instanceof MilitaryCrateTileEntity crate))
            return;
        crate.open(player.getUUID());
        if(!crate.isOpened())
            return;
        // every cell: the model of the closed crate goes, the outline shrinks to the floor
        for(int part = 0; part < CELLS; part++)
        {
            BlockPos pos = main.offset(offset(state.getValue(FACING), part));
            BlockState cell = level.getBlockState(pos);
            if(cell.is(this))
            {
                level.setBlock(pos, cell.setValue(OPENED, true), Block.UPDATE_CLIENTS);
            }
        }
    }

    /* Breaking: the whole crate goes, its item drops once (from the main cell, with the vehicle inside) */

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        if(!level.isClientSide() && !isMain(state) && !player.isCreative())
        {
            BlockPos main = getMainPos(pos, state);
            BlockState mainState = level.getBlockState(main);
            if(mainState.is(this))
            {
                Block.dropResources(mainState, level, main, level.getBlockEntity(main), player, player.getMainHandItem());
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        super.onRemove(state, level, pos, newState, isMoving);
        if(state.is(newState.getBlock()))
            return;
        BlockPos main = getMainPos(pos, state);
        for(int part = 0; part < CELLS; part++)
        {
            BlockPos other = main.offset(offset(state.getValue(FACING), part));
            if(!other.equals(pos) && level.getBlockState(other).is(this))
            {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }

    /*
     * The closed crate is the block model of the main cell (baked in the chunk); opening, the block entity renderer of
     * the main cell draws its parts. The other cells draw nothing. Light goes through the cells.
     */

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return isMain(state) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
    {
        return 1.0F;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return isMain(state) ? new MilitaryCrateTileEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return createTicker(type, ModTileEntities.MILITARY_CRATE.get(), (l, p, s, crate) -> crate.tick());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter reader, List<Component> list, TooltipFlag flag)
    {
        VehicleCrateBlock.appendCrateText(stack, list, this.getDescriptionId());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, PART, OPENED);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
