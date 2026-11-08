package com.mrcrayfish.vehicle.common.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class Serializers
{
    public static final IDataSerializer<Integer> INTEGER = new IDataSerializer<>()
    {
        @Override
        public void write(FriendlyByteBuf buffer, Integer value)
        {
            buffer.writeVarInt(value);
        }

        @Override
        public Integer read(FriendlyByteBuf buffer)
        {
            return buffer.readVarInt();
        }
    };

    public static final IDataSerializer<Optional<BlockPos>> OPTIONAL_BLOCK_POS = new IDataSerializer<>()
    {
        @Override
        public void write(FriendlyByteBuf buffer, Optional<BlockPos> optional)
        {
            buffer.writeBoolean(optional.isPresent());
            optional.ifPresent(buffer::writeBlockPos);
        }

        @Override
        public Optional<BlockPos> read(FriendlyByteBuf buffer)
        {
            if(buffer.readBoolean())
            {
                return Optional.of(buffer.readBlockPos());
            }
            return Optional.empty();
        }
    };
}
