package com.mrcrayfish.vehicle.tileentity;

import com.mrcrayfish.vehicle.block.MilitaryCrateBlock;
import com.mrcrayfish.vehicle.client.VehicleHelper;
import com.mrcrayfish.vehicle.init.ModSounds;
import com.mrcrayfish.vehicle.init.ModTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The military crate (main cell of MilitaryCrateBlock): the vehicle inside and its opening, as the vehicle crate
 * (same timing: the walls land at 90, 110, 130 and 150 with their sound, the burst at 150, the vehicle is there at
 * 250), from the middle of the 2 x 2 crate. The lid is thrown off as the opening starts: it flies over one side of
 * the crate (not where the vehicle comes out), lands flat on the ground 3 to 4 blocks away at LID_LANDED, and
 * breaks apart in splinters at LID_GONE (MilitaryCrateRenderer draws it until then).
 */
public class MilitaryCrateTileEntity extends VehicleCrateTileEntity
{
    /** Timer values at which the lid lands, and at which it is gone */
    public static final int LID_LANDED = 50, LID_GONE = 150;
    /** How far the middle of the lid lands from the side of the crate (blocks) */
    private static final float LID_DISTANCE = 3.5F;
    /** Height of the middle of the lid on the crate, and lying on the ground (blocks) */
    public static final float LID_HEIGHT = 22.5F / 16F, LID_GROUND = 1.5F / 16F;

    public MilitaryCrateTileEntity(BlockPos pos, BlockState state)
    {
        super(ModTileEntities.MILITARY_CRATE.get(), pos, state);
    }

    /**
     * Where the lid lands, in the frame of the models (blocks): x and z of its middle, its turn about the vertical
     * (degrees) and the side it is thrown to (-1 left, 1 right). The same for every player: it comes from the place of
     * the crate.
     */
    public static float[] lidLanding(BlockPos main)
    {
        long seed = Mth.getSeed(main);
        float side = (seed & 1) == 0 ? -1F : 1F;
        float along = ((seed >> 4) & 15) / 15F * 0.8F - 0.4F;
        float turn = (((seed >> 8) & 15) / 15F - 0.5F) * 50F;
        return new float[]{1F + side * (1F + LID_DISTANCE), 1F + along, turn, side};
    }

    private Direction getFacing()
    {
        return this.getBlockState().getValue(MilitaryCrateBlock.FACING);
    }

    @Override
    protected Vec3 getVehiclePosition()
    {
        return MilitaryCrateBlock.getCentre(this.worldPosition, this.getFacing());
    }

    @Override
    protected float getVehicleYaw()
    {
        // the vehicle comes out facing the front of the crate
        return this.getFacing().toYRot();
    }

    @Override
    protected Vec3 getCentre()
    {
        return this.getVehiclePosition().add(0, 0.75, 0);
    }

    @Override
    public void tick()
    {
        super.tick();
        if(this.level == null || !this.level.isClientSide() || !this.isOpened())
            return;
        int timer = this.getTimer();
        if(timer != LID_LANDED && timer != LID_GONE)
            return;
        float[] landing = lidLanding(this.worldPosition);
        Vec3 lid = MilitaryCrateBlock.toWorld(this.worldPosition, this.getFacing(), landing[0], LID_GROUND, landing[1]);
        BlockPos lidPos = BlockPos.containing(lid);
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, this.getBlockState());
        if(timer == LID_LANDED)
        {
            // the lid hits the ground: a thud and a little dust under it
            VehicleHelper.playSound(ModSounds.BLOCK_VEHICLE_CRATE_PANEL_LAND.get(), lidPos, 1.0F, 0.75F);
            for(int i = 0; i < 12; i++)
            {
                double x = (this.level.random.nextDouble() - 0.5) * 2.0, z = (this.level.random.nextDouble() - 0.5) * 2.0;
                this.level.addParticle(particle, lid.x + x, lid.y, lid.z + z, x * 0.05, 0.05, z * 0.05);
            }
        }
        else
        {
            // the lid breaks apart where it lies
            for(int i = 0; i < 48; i++)
            {
                double x = (this.level.random.nextDouble() - 0.5) * 2.0, z = (this.level.random.nextDouble() - 0.5) * 2.0;
                this.level.addParticle(particle, lid.x + x, lid.y + 0.1, lid.z + z, x * 0.12, 0.1 + this.level.random.nextDouble() * 0.15, z * 0.12);
            }
            VehicleHelper.playSound(SoundEvents.WOOD_BREAK, lidPos, 1.0F, 0.8F);
        }
    }
}
