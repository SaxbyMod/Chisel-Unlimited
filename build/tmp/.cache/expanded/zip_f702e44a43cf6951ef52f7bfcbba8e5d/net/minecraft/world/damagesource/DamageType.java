package net.minecraft.world.damagesource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;

public record DamageType(String msgId, DamageScaling scaling, float exhaustion, DamageEffects effects, DeathMessageType deathMessageType) {
    public static final Codec<DamageType> DIRECT_CODEC = RecordCodecBuilder.create(
        p_270460_ -> p_270460_.group(
                    Codec.STRING.fieldOf("message_id").forGetter(DamageType::msgId),
                    DamageScaling.CODEC.fieldOf("scaling").forGetter(DamageType::scaling),
                    Codec.FLOAT.fieldOf("exhaustion").forGetter(DamageType::exhaustion),
                    DamageEffects.CODEC.optionalFieldOf("effects", DamageEffects.HURT).forGetter(DamageType::effects),
                    DeathMessageType.CODEC.optionalFieldOf("death_message_type", DeathMessageType.DEFAULT).forGetter(DamageType::deathMessageType)
                )
                .apply(p_270460_, DamageType::new)
    );
    public static final Codec<Holder<DamageType>> CODEC = RegistryFixedCodec.create(Registries.DAMAGE_TYPE);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DamageType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.DAMAGE_TYPE);

    public DamageType(String pMsgId, DamageScaling pScaling, float pExhaustion) {
        this(pMsgId, pScaling, pExhaustion, DamageEffects.HURT, DeathMessageType.DEFAULT);
    }

    public DamageType(String pMsgId, DamageScaling pScaling, float pExhaustion, DamageEffects pEffects) {
        this(pMsgId, pScaling, pExhaustion, pEffects, DeathMessageType.DEFAULT);
    }

    public DamageType(String pMsgId, float pExhaustion, DamageEffects pEffects) {
        this(pMsgId, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, pExhaustion, pEffects);
    }

    public DamageType(String pMsgId, float pExhaustion) {
        this(pMsgId, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, pExhaustion);
    }
}