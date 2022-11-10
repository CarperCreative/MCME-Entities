package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.ai.goal.head.HeadGoalLook;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class GoalWatchLocation extends GoalVirtualEntity {

    private final Location targetLocation;

    public GoalWatchLocation(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);

        this.targetLocation = factory.getTargetLocation().clone();
        this.movementSpeed = MovementSpeed.STAND;

        final Location location = this.getEntity().getLocation().clone();

        final Location orientation = location.setDirection(
            this.targetLocation.toVector().subtract(
                location.toVector()
            )
        );

        this.setYaw(orientation.getYaw());
        this.setPitch(orientation.getPitch());

        this.setDefaultHeadGoal();
    }

    @Override
    public Vector getDirection() {
        return null;
    }

    public void setDefaultHeadGoal() {
        this.clearHeadGoals();
        this.addHeadGoal(new HeadGoalLook(this.targetLocation, this.getEntity()));
    }

    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory().withTargetLocation(this.targetLocation);
    }

}
