package net.paulm.hacksaw.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.paulm.hacksaw.Hacksaw;
import net.paulm.hacksaw.item.HacksawItems;

public class BouncyBallEntity extends ThrownItemEntity {

    public ItemStack itemForm;
    public int stillTime = 0;

    public BouncyBallEntity(EntityType<? extends BouncyBallEntity> entityType, World world) {
        super(entityType, world);
    }

    public BouncyBallEntity(EntityType<? extends BouncyBallEntity> entityType, LivingEntity owner, World world, ItemStack item) {
        super(entityType, owner, world);
        this.itemForm = item.copy();
        this.itemForm.setCount(1);
    }

    public BouncyBallEntity(EntityType<? extends BouncyBallEntity> entityType, double x, double y, double z, World world) {
        super(entityType, x, y, z, world);
    }

    public void tick() {
        //Do the tick() from entity
        this.baseTick();

        this.parentalTick();

        //And now, the supposedly actually functional collisions
        this.bouncyBallCollision(MovementType.SELF, this.getVelocity());
        //Plop back into item if not moving for a bit
        if (this.getVelocity().length() <= 0.05) {
            this.stillTime++;
        }
        else {
            this.stillTime = 0;
        }
        if (this.stillTime >= 100) {
            this.returnToItem();
        }
    }

    //The bounciness of the collision
    public static float getBounciness() {
        return 0.5f;
    }
    //The proportion of velocity kept when colliding vertically
    public static float getDrag() {
        return 0.7f;
    }

    public void parentalTick() {
        //This is a basically the interesting part of the ThrownEntity tick()
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        boolean bl = false;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
            BlockState blockState = this.getWorld().getBlockState(blockPos);
            if (blockState.isOf(Blocks.NETHER_PORTAL)) {
                this.setInNetherPortal(blockPos);
                bl = true;
            } else if (blockState.isOf(Blocks.END_GATEWAY)) {
                BlockEntity blockEntity = this.getWorld().getBlockEntity(blockPos);
                if (blockEntity instanceof EndGatewayBlockEntity && EndGatewayBlockEntity.canTeleport(this)) {
                    EndGatewayBlockEntity.tryTeleportingEntity(this.getWorld(), blockPos, blockState, this, (EndGatewayBlockEntity)blockEntity);
                }
                bl = true;
            }
        }
        if (hitResult.getType() != HitResult.Type.MISS && !bl) {
            this.onCollision(hitResult);
        }
    }

    public void bouncyBallCollision(MovementType movementType, Vec3d movement) {
        //More bits from ThrownEntity
        this.checkBlockCollision();
        double g;
        float h;
        this.updateRotation();
        if (this.isTouchingWater()) {
            for (int i = 0; i < 4; ++i) {
                this.getWorld().addParticle(ParticleTypes.BUBBLE, this.getX(), this.getY(), this.getZ(), movement.x, movement.y, movement.z);
            }
            h = 0.8f;
        } else {
            h = 1f;
        }
        this.setVelocity(movement.multiply(h));
        movement = this.getVelocity();
        if (!this.hasNoGravity()) {
            this.setVelocity(movement.x, movement.y - (double)this.getGravity(), movement.z);
        }


        //Stuff from Entity move() for the actual collisions
        Vec3d vec3d;
        if ((g = (vec3d = Entity.adjustMovementForCollisions(this, movement = this.adjustMovementForSneaking(movement, movementType), this.getBoundingBox(), this.getWorld(), this.getWorld().getEntityCollisions(this, this.getBoundingBox().stretch(movement)))).lengthSquared()) > 1.0E-7) {
            BlockHitResult blockHitResult;
            if (this.fallDistance != 0.0f && g >= 1.0 && (blockHitResult = this.getWorld().raycast(new RaycastContext(this.getPos(), this.getPos().add(vec3d), RaycastContext.ShapeType.FALLDAMAGE_RESETTING, RaycastContext.FluidHandling.WATER, this))).getType() != HitResult.Type.MISS) {
                this.onLanding();
            }
            this.setPosition(this.getX() + vec3d.x, this.getY() + vec3d.y, this.getZ() + vec3d.z);
        }
        boolean bl = !MathHelper.approximatelyEquals(movement.x, vec3d.x);
        boolean bl2 = !MathHelper.approximatelyEquals(movement.z, vec3d.z);
        //:) boing boing collisions
        this.horizontalCollision = bl || bl2;
        this.verticalCollision = movement.y != vec3d.y;
        Vec3d vec3d1 = this.getVelocity();
        if (bl) {
            vec3d1 = vec3d1.multiply(-getBounciness(), 1, 1);
        }
        if (this.verticalCollision) {
            vec3d1 = vec3d1.multiply(getDrag(), -getBounciness(), getDrag());
        }
        if (bl2) {
            vec3d1 = vec3d1.multiply(1, 1, -getBounciness());
        }
        this.setVelocity(vec3d1);
    }

    public void returnToItem() {
        if (this.itemForm != null) {
            this.dropStack(this.itemForm);
        }
        this.discard();
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.itemForm != null) {
            player.giveItemStack(this.itemForm);
        }
        this.discard();
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Box entityBox = entityHitResult.getEntity().getBoundingBox();
        Box intersection  = entityBox.intersection(this.getBoundingBox());
        Hacksaw.LOGGER.info("Collision!");
        //Get slimmest dim
        if((intersection.getXLength()/entityBox.getXLength() < intersection.getYLength()/entityBox.getYLength()) && (intersection.getXLength()/entityBox.getXLength() < intersection.getZLength()/entityBox.getZLength())) {
            //Change x velocity
            if (intersection.getCenter().getX() < entityBox.getCenter().getX()) {
                this.setVelocity(-Math.abs(this.getVelocity().getX()), this.getVelocity().getY(), this.getVelocity().getZ());
            } else {
                this.setVelocity(Math.abs(this.getVelocity().getX()), this.getVelocity().getY(), this.getVelocity().getZ());
            }
        } else if (intersection.getYLength()/entityBox.getYLength() < intersection.getZLength()/entityBox.getZLength()) {
            //Change y velocity
            if (intersection.getCenter().getY() < entityBox.getCenter().getY()) {
                this.setVelocity(this.getVelocity().getX(), -Math.abs(this.getVelocity().getY()), this.getVelocity().getZ());
            } else {
                this.setVelocity(this.getVelocity().getX(), Math.abs(this.getVelocity().getY()), this.getVelocity().getZ());
            }
        } else {
            //Change z velocity
            if (intersection.getCenter().getZ() < entityBox.getCenter().getZ()) {
                this.setVelocity(this.getVelocity().getX(), this.getVelocity().getY(), -Math.abs(this.getVelocity().getZ()));
            } else {
                this.setVelocity(this.getVelocity().getX(), this.getVelocity().getY(), Math.abs(this.getVelocity().getZ()));
            }
        }

    }

    @Override
    protected Item getDefaultItem() {
        return HacksawItems.BOUNCY_BALL;
    }
}
