package net.paulm.hacksaw.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.paulm.hacksaw.entity.DynamiteEntity;
import net.paulm.hacksaw.entity.HacksawEntities;
import net.paulm.hacksaw.entity.ImpactDynamiteEntity;

public class ImpactDynamiteItem extends DynamiteItem {
    public ImpactDynamiteItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        //no fuse for this one
    }

    @Override
    public boolean canThrow(PlayerEntity user, ItemStack itemStack, Hand hand) {
        return true;
    }

    @Override
    public void throwDynamite(World world, PlayerEntity user, ItemStack itemStack) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClient) {
            ImpactDynamiteEntity dynamiteEntity = new ImpactDynamiteEntity(HacksawEntities.IMPACT_DYNAMITE_STICK, user, world);
            dynamiteEntity.setItem(itemStack);
            dynamiteEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.2f, 0.75f);
            dynamiteEntity.setFuseTime(getFuse(itemStack));
            dynamiteEntity.setOnImpact(true);
            world.spawnEntity(dynamiteEntity);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }
    }


    @Override
    public int getFuse(ItemStack stack) {
        return 1000;
    }

    @Override
    public boolean isLit(ItemStack itemStack) {
        return false;
    }
}
