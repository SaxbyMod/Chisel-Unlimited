package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FoodOnAStickItem<T extends Entity & ItemSteerable> extends Item {
    private final EntityType<T> canInteractWith;
    private final int consumeItemDamage;

    public FoodOnAStickItem(EntityType<T> pCanInteractWith, int pConsumeItemDamage, Item.Properties pProperties) {
        super(pProperties);
        this.canInteractWith = pCanInteractWith;
        this.consumeItemDamage = pConsumeItemDamage;
    }

    @Override
    public InteractionResult use(Level p_41314_, Player p_41315_, InteractionHand p_41316_) {
        ItemStack itemstack = p_41315_.getItemInHand(p_41316_);
        if (p_41314_.isClientSide) {
            return InteractionResult.PASS;
        } else {
            Entity entity = p_41315_.getControlledVehicle();
            if (p_41315_.isPassenger() && entity instanceof ItemSteerable itemsteerable && entity.getType() == this.canInteractWith && itemsteerable.boost()) {
                EquipmentSlot equipmentslot = LivingEntity.getSlotForHand(p_41316_);
                ItemStack itemstack1 = itemstack.hurtAndConvertOnBreak(this.consumeItemDamage, Items.FISHING_ROD, p_41315_, equipmentslot);
                return InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(itemstack1);
            }

            p_41315_.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.PASS;
        }
    }
}