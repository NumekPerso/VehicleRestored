package com.mrcrayfish.vehicle.block;

import com.mrcrayfish.vehicle.init.ModTileEntities;
import com.mrcrayfish.vehicle.tileentity.AdvancedWorkshopPartTileEntity;
import com.mrcrayfish.vehicle.tileentity.AdvancedWorkshopTileEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/**
 * The advanced workshop: one machine of 3 x 2 cells, 3 high (2.5 blocks: it stands on a base half a
 * block high), where the special vehicles (the tank) and
 * their ammunition are made, in phases that take time and energy (FE from other mods). Each cell is
 * a block of its own (PART); the main cell (front, left, bottom: PART 0, where the player clicked)
 * holds the machine (AdvancedWorkshopTileEntity) and draws it, the others forward energy, items and
 * clicks to it, so cables and hoppers work on any face. Breaking any cell removes the whole machine.
 */
public class AdvancedWorkshopBlock extends ObjectBlock implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Cell of the machine: dx + 3 * dz + 6 * dy, dx to the right seen from the front, dz to the back, dy up */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 17);
    public static final int WIDTH = 3, DEPTH = 2, HEIGHT = 3, CELLS = WIDTH * DEPTH * HEIGHT;

    /**
     * The volume of the machine, in model units (1/16 block) in the frame of its models (x to the right, z to
     * the front, origin at the left back bottom corner): base, enclosure, roof extractor, electrical
     * cabinet with its console, cooling unit
     */
    private static final double[][] VOLUME = {
        {1, 0, 1, 47, 11, 31},
        {1.5, 11, 2, 34.5, 37.5, 31.4},
        {4.23, 37.5, 6.18, 10.23, 39.3, 12.18},
        {34.5, 11, 3.5, 46.5, 32, 31.4},
        {37.55, 32, 6.2, 43.05, 35, 14.2}
    };
    /** The whole machine seen from each cell (facing, part): so the outline and the collisions follow the machine */
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][CELLS];

    public AdvancedWorkshopBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(3.5F, 6.0F).requiresCorrectToolForDrops().noOcclusion()
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false).pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    /**
     * A point of the models (blocks, x and z) in the frame of the main cell, for a machine facing `facing`:
     * the same transform as the renderer (AdvancedWorkshopRenderer)
     */
    private static double[] toMainCell(Direction facing, double x, double z)
    {
        double angle = Math.toRadians(-facing.toYRot());
        double px = x - 0.5, pz = z - 1.5;
        return new double[]{px * Math.cos(angle) + pz * Math.sin(angle) + 0.5, -px * Math.sin(angle) + pz * Math.cos(angle) + 0.5};
    }

    private static VoxelShape getMachineShape(Direction facing, int part)
    {
        int index = facing.get2DDataValue();
        if(SHAPES[index][part] == null)
        {
            BlockPos cell = offset(facing, part);
            VoxelShape shape = Shapes.empty();
            for(double[] box : VOLUME)
            {
                double[] a = toMainCell(facing, box[0] / 16.0, box[2] / 16.0), b = toMainCell(facing, box[3] / 16.0, box[5] / 16.0);
                shape = Shapes.or(shape, Shapes.box(Math.min(a[0], b[0]) - cell.getX(), box[1] / 16.0 - cell.getY(), Math.min(a[1], b[1]) - cell.getZ(),
                        Math.max(a[0], b[0]) - cell.getX(), box[4] / 16.0 - cell.getY(), Math.max(a[1], b[1]) - cell.getZ()));
            }
            SHAPES[index][part] = shape.optimize();
        }
        return SHAPES[index][part];
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return getMachineShape(state.getValue(FACING), state.getValue(PART));
    }

    /** Offset of a cell from the main cell, for a machine whose front faces `facing` */
    public static BlockPos offset(Direction facing, int part)
    {
        int dx = part % WIDTH, dz = (part / WIDTH) % DEPTH, dy = part / (WIDTH * DEPTH);
        Direction right = facing.getCounterClockWise(), back = facing.getOpposite();
        return new BlockPos(right.getStepX() * dx + back.getStepX() * dz, dy, right.getStepZ() * dx + back.getStepZ() * dz);
    }

    /** Middle of the machine (of its working space, for its sounds), from its main cell */
    public static Vec3 getCentre(BlockPos main, Direction facing)
    {
        Direction right = facing.getCounterClockWise(), back = facing.getOpposite();
        return new Vec3(main.getX() + 0.5 + right.getStepX() + back.getStepX() * 0.5, main.getY() + 1.25, main.getZ() + 0.5 + right.getStepZ() + back.getStepZ() * 0.5);
    }

    public static BlockPos getMainPos(BlockPos pos, BlockState state)
    {
        return pos.subtract(offset(state.getValue(FACING), state.getValue(PART)));
    }

    public static boolean isMain(BlockState state)
    {
        return state.getValue(PART) == 0;
    }

    /* Placing: the clicked cell is the front left bottom cell, the machine faces the player */

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
        return this.defaultBlockState().setValue(FACING, facing).setValue(PART, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        if(!level.isClientSide())
        {
            for(int part = 1; part < CELLS; part++)
            {
                level.setBlock(pos.offset(offset(state.getValue(FACING), part)), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    /* Breaking: the whole machine goes, its item drops once (from the main cell) */

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        if(!level.isClientSide() && !isMain(state) && !player.isCreative() && player.hasCorrectToolForDrops(state))
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
        if(state.is(newState.getBlock()))
        {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        if(isMain(state) && level.getBlockEntity(pos) instanceof AdvancedWorkshopTileEntity workshop)
        {
            workshop.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if(!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
        {
            BlockPos main = getMainPos(pos, state);
            if(level.getBlockEntity(main) instanceof AdvancedWorkshopTileEntity workshop)
            {
                workshop.syncToClient();
                NetworkHooks.openScreen(serverPlayer, workshop, main);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /*
     * The still machine is the block model of the main cell (baked in the chunk once: frame and glass), the
     * moving and lit parts are drawn by the renderer of the main cell; the other cells draw nothing. Light
     * goes through the cells so the machine is lit.
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
        return isMain(state) ? new AdvancedWorkshopTileEntity(pos, state) : new AdvancedWorkshopPartTileEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        // the client ticks too: the work goes on smoothly between two updates from the server
        return createTicker(type, ModTileEntities.ADVANCED_WORKSHOP.get(), (l, p, s, workshop) -> workshop.tick());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, PART);
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
