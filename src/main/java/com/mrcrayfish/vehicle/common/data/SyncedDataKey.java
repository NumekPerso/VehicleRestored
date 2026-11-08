package com.mrcrayfish.vehicle.common.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A value stored on a player and synced to the clients that can see that player.
 * Replaces the synced data key of the Obfuscate library, which the original mod used.
 */
public class SyncedDataKey<T>
{
    private final ResourceLocation id;
    private final IDataSerializer<T> serializer;
    private final Supplier<T> defaultValueSupplier;
    private final boolean resetOnDeath;

    private SyncedDataKey(ResourceLocation id, IDataSerializer<T> serializer, Supplier<T> defaultValueSupplier, boolean resetOnDeath)
    {
        this.id = id;
        this.serializer = serializer;
        this.defaultValueSupplier = defaultValueSupplier;
        this.resetOnDeath = resetOnDeath;
    }

    public ResourceLocation getId()
    {
        return this.id;
    }

    public IDataSerializer<T> getSerializer()
    {
        return this.serializer;
    }

    public T getDefaultValue()
    {
        return this.defaultValueSupplier.get();
    }

    public boolean isResetOnDeath()
    {
        return this.resetOnDeath;
    }

    public static <T> Builder<T> builder(IDataSerializer<T> serializer)
    {
        return new Builder<>(serializer);
    }

    public static class Builder<T>
    {
        private final IDataSerializer<T> serializer;
        private ResourceLocation id;
        private Supplier<T> defaultValueSupplier;
        private boolean resetOnDeath;

        private Builder(IDataSerializer<T> serializer)
        {
            this.serializer = serializer;
        }

        public Builder<T> id(ResourceLocation id)
        {
            this.id = id;
            return this;
        }

        public Builder<T> defaultValueSupplier(Supplier<T> supplier)
        {
            this.defaultValueSupplier = supplier;
            return this;
        }

        public Builder<T> resetOnDeath()
        {
            this.resetOnDeath = true;
            return this;
        }

        public SyncedDataKey<T> build()
        {
            return new SyncedDataKey<>(Objects.requireNonNull(this.id), this.serializer, Objects.requireNonNull(this.defaultValueSupplier), this.resetOnDeath);
        }
    }
}
