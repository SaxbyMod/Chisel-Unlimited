package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class AdultSensor extends Sensor<AgeableMob> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }

    protected void doTick(ServerLevel p_148248_, AgeableMob p_148249_) {
        p_148249_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).ifPresent(p_186145_ -> this.setNearestVisibleAdult(p_148249_, p_186145_));
    }

    private void setNearestVisibleAdult(AgeableMob pMob, NearestVisibleLivingEntities pNearbyEntities) {
        Optional<AgeableMob> optional = pNearbyEntities.findClosest(p_359100_ -> p_359100_.getType() == pMob.getType() && !p_359100_.isBaby())
            .map(AgeableMob.class::cast);
        pMob.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, optional);
    }
}