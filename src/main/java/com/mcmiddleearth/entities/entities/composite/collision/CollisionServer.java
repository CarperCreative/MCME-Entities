package com.mcmiddleearth.entities.entities.composite.collision;

import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.provider.EntityProvider;
import com.mcmiddleearth.entities.provider.PlayerProvider;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CollisionServer {
    private final EntityProvider entityProvider;
    private final PlayerProvider playerProvider;

    public CollisionServer(EntityProvider entityProvider, PlayerProvider playerProvider) {
        this.entityProvider = entityProvider;
        this.playerProvider = playerProvider;
    }

    public void doTick() {
        // Get current list of collision entities (ideally we'd already have this, but there's no way to observe entity spawns and removals)
        List<CollisionEntity> entities = new ArrayList<>();
        for (McmeEntity mcmeEntity : entityProvider.getEntities()) {
            if (mcmeEntity instanceof CollisionEntity) {
                entities.add((CollisionEntity) mcmeEntity);
            }
        }

        // Update all collision boxes to match the current entity state
        for (CollisionEntity entity : entities) {
            entity.updateColliders();
        }

        HashMap<World, List<Entity>> realWorldEntities = new HashMap<>();

        for (CollisionEntity entity : entities) {
            World world = entity.getLocation().getWorld();
            Vector entityOffset = entity.getLocation().toVector().multiply(-1f);
            List<Entity> realEntities = realWorldEntities.computeIfAbsent(world, CollisionServer::getRealEntitiesForWorld);
            BoundingBox boundingBox = entity.getCollisionBoundingBox();

            for (Entity otherEntity : realEntities) {
                BoundingBox otherBoundingBox = otherEntity.getBoundingBox();

                // Translate other bounding box to our entity's local space
                otherBoundingBox.shift(entityOffset);

                // Figure out the step count to ultimately decide if we want to do stepping at all (in which case steps = 1)
                Vector otherEntityVelocity = otherEntity.getVelocity().clone();
                double speed = otherEntityVelocity.length();
                double shortestBoundingBoxEdge = Math.min(Math.min(otherBoundingBox.getWidthX(), otherBoundingBox.getWidthZ()), otherBoundingBox.getHeight());
                double adjacentStepBoundingBoxOverlap = 0.5d;
                int steps = (int) Math.ceil(speed / (shortestBoundingBoxEdge * adjacentStepBoundingBoxOverlap));
                // Ensure we do at least one step even if the other entity is not moving
                if (steps < 1) steps = 1;
                Vector stepOffset = new Vector();

                // Do a coarse check first
                BoundingBox coarseOtherBoundingBox = otherBoundingBox;

                if (steps > 1) {
                    // Expand the bounding box to cover the current and next location
                    coarseOtherBoundingBox = coarseOtherBoundingBox.clone().shift(otherEntityVelocity).union(otherBoundingBox);
                    // Step offset gets negated because it'll be used to move the entity's colliders "into" the other entity, not the other way around
                    stepOffset.copy(otherEntityVelocity).multiply(-1d / steps);
                }

                if (!boundingBox.overlaps(coarseOtherBoundingBox)) continue;

                // Do more precise collision checks
                testOtherEntity: for (Iterator<BoundingBox> it = entity.getColliders().iterator(); it.hasNext(); ) {
                    BoundingBox entityCollider = it.next().clone();

                    // Unroll loop to avoid unnecessary shifting when there's no extra steps
                    if (entityCollider.overlaps(otherBoundingBox)) {
                        // Collision detected
                        onCollision(entity, otherEntity);
                        break testOtherEntity;
                    }

                    for (int step = 1; step < steps; step++) {
                        entityCollider.shift(stepOffset);

                        if (entityCollider.overlaps(otherBoundingBox)) {
                            // Collision detected
                            onCollision(entity, otherEntity);
                            break testOtherEntity;
                        }
                    }
                }
            }
        }
    }

    private void onCollision(CollisionEntity target, Entity attacker) {
        // TODO: handle other types of projectile entities
        boolean targetIsMcmeEntity = target instanceof McmeEntity;
        boolean targetIsDamageable = target instanceof Damageable;

        // Only some entities are capable of receiving damage
        if (targetIsMcmeEntity || targetIsDamageable) {
            double damage = -1d;
            double knockbackStrength = -1d;
            Entity realAttacker = attacker;

            if (attacker instanceof Projectile) {
                Projectile projectile = (Projectile) attacker;

                if (projectile.getShooter() instanceof Player) {
                    realAttacker = (Player) projectile.getShooter();
                }
            }

            if (attacker instanceof AbstractArrow) {
                AbstractArrow arrow = (AbstractArrow) attacker;

                damage = arrow.getDamage();
                knockbackStrength = arrow.getKnockbackStrength();
            } else if (attacker instanceof Snowball) {
                // TODO: damage blaze targets
                damage = 0d;
                knockbackStrength = 1d;
            }

            if (damage >= 0d) {
                if (targetIsMcmeEntity) {
                    McmeEntity mcmeEntity = (McmeEntity) target;

                    if (realAttacker instanceof Player) {
                        // Special case to allow setting enemies for AI
                        mcmeEntity.receiveAttack(playerProvider.getOrCreateMcmePlayer((Player) realAttacker), damage, knockbackStrength);
                    } else {
                        mcmeEntity.receiveAttack(null, damage, knockbackStrength);
                    }
                } else if (targetIsDamageable) {
                    Damageable damageable = (Damageable) target;

                    // TODO: apply knockback?
                    damageable.damage(damage);
                }
            }
        }

        // Projectiles get destroyed on hit
        if (attacker instanceof Projectile) {
            attacker.remove();

            // Mimic behavior of the other Projectile implementation that I couldn't be bothered to modify
            Location location = attacker.getLocation();
            location.getWorld().spawnParticle(Particle.SPELL_INSTANT, location, 1, 0, 0, 0, 2, null, true);
        }
    }

    private static List<Entity> getRealEntitiesForWorld(World world) {
        return world.getEntities().stream()
                .filter(entity -> isEntityCollidable(entity))
                .collect(Collectors.toList());
    }

    private static boolean isEntityCollidable(Entity entity) {
        EntityType entityType = entity.getType();

        switch (entityType) {
            case ARROW:
            case SPECTRAL_ARROW:
            case TRIDENT:
                // Do not collide with arrow-likes which already hit a block.
                return !((AbstractArrow) entity).isInBlock();
            case SNOWBALL:
                return true;
            default:
                return false;
        }
    }
}
