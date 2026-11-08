package com.mrcrayfish.vehicle.common.data;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Replaces the data serializer of the Obfuscate library, which the original mod used.
 */
public interface IDataSerializer<T>
{
    void write(FriendlyByteBuf buffer, T value);

    T read(FriendlyByteBuf buffer);
}
