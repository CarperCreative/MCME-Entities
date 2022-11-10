package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalWatch;
import com.mcmiddleearth.entities.ai.pathfinding.Pathfinder;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.events.goal.GoalCheckpointReachedEvent;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class GoalLocationTargetFollowCheckpointsWondering extends GoalLocationTarget {

    private final Location[] checkpoints;
    private final boolean loop;
    protected McmeEntity target;
    protected UUID uniqueId;
    protected boolean targetIncomplete = false;
    private int currentCheckpoint;
    private boolean targetPlayer;
    private int tickCounter = 0;
    private final HeadGoalWatch headGoalWatch;

    public GoalLocationTargetFollowCheckpointsWondering(VirtualEntity entity, VirtualEntityGoalFactory factory, Pathfinder pathfinder) {
        super(entity, factory, pathfinder);

        this.checkpoints = factory.getCheckpoints();
        this.loop = factory.isLoop();
        this.target = factory.getTargetEntity();
        this.uniqueId = this.target.getUniqueId();
        this.targetPlayer = false;
        this.currentCheckpoint = factory.getStartCheckpoint();

        this.setTarget(this.checkpoints[this.currentCheckpoint]);
        this.setPathTarget(this.checkpoints[this.currentCheckpoint].toVector());

        this.headGoalWatch = new HeadGoalWatch(this.target, this.getEntity());

        this.clearHeadGoals();
        this.addHeadGoal(this.headGoalWatch);
    }

    @Override
    public void update() {
        if (this.isFinished()) {
            if (this.targetPlayer) {
                this.selectTarget();
                this.lookAtTarget();
            }
            return;
        }

        if (this.isCloseToTarget(GoalDistance.POINT)) {
            EntitiesPlugin.getEntityServer().handleEvent(new GoalCheckpointReachedEvent(this.getEntity(), this));
            this.currentCheckpoint++;
            if (this.currentCheckpoint == this.checkpoints.length && this.loop) {
                this.currentCheckpoint = 0;
            }
            this.deletePath();
            this.setFinished();

            if (this.currentCheckpoint < this.checkpoints.length) {
                this.targetPlayer = true;

                Bukkit.getScheduler().runTaskLater(EntitiesPlugin.getInstance(), () -> {
                    this.unsetFinished();
                    this.setTarget(this.checkpoints[this.currentCheckpoint]);
                    this.setPathTarget(this.checkpoints[this.currentCheckpoint].toVector());
                }, 60L);
            } else {
                this.targetPlayer = false;
                this.movementSpeed = MovementSpeed.STAND;
            }
        }
        super.update();
    }


    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory()
                .withStartCheckpoint(Math.min(this.currentCheckpoint, this.checkpoints.length - 1))
                .withCheckpoints(this.checkpoints)
                .withTargetEntity(this.target)
                .withLoop(this.loop);
    }

    private void lookAtTarget() {
        if (this.targetIncomplete) {
            final McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(this.uniqueId);
            if (search != null) {
                this.target = search;
                this.targetIncomplete = false;
                this.headGoalWatch.setTarget(this.target);
            }
        }

        if (this.target == null || !this.target.isOnline() || this.target.isDead()) {
            this.targetIncomplete = true;
            return;
        }

        if (!this.targetIncomplete) {
            if (this.tickCounter % 3 == 0) {
                final Location orientation = this.getEntity().getLocation().clone()
                        .setDirection(this.target.getLocation().toVector().subtract(this.getEntity().getLocation().toVector()));
                this.setYaw(orientation.getYaw());
                this.setPitch(orientation.getPitch());
                this.tickCounter = 0;
                //hasRotation = true;
            }
            //secondUpdate = !secondUpdate;
            this.tickCounter++;
        }
    }

    private void selectTarget() {
        this.targetIncomplete = false;

        final Location entityLocation = this.getEntity().getLocation();
        final Collection<Player> nearbyPlayers = this.getEntity().getLocation().getWorld()
                .getNearbyEntities(entityLocation, 10, 10, 10, Player.class::isInstance)
                .stream()
                .map(Player.class::cast)
                .collect(Collectors.toList());
        if (nearbyPlayers.size() == 1 && this.target != null && this.target.isOnline()) {
            return;
        }

        final Optional<Player> playerOptional = nearbyPlayers.stream()
                .min(Comparator.comparingDouble(o -> o.getLocation().distanceSquared(entityLocation)));
        if(!playerOptional.isPresent()) {
            this.target = null;
            return;
        }

        final Player player = playerOptional.get();
        final McmeEntity search = EntitiesPlugin.getEntityServer().getEntity(player.getUniqueId());
        if (search != null) {
            if (this.target != null && this.target.getUniqueId().equals(search.getUniqueId())) return;

            this.target = search;
            this.setDefaultHeadGoal();
        }
    }


    public void setDefaultHeadGoal() {
        this.clearHeadGoals();
        this.addHeadGoal(new HeadGoalWatch(this.target, this.getEntity()));
    }
}
