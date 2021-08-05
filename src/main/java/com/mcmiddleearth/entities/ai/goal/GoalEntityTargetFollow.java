package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.ai.pathfinding.Pathfinder;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.events.goal.GoalVirtualEntityIsClose;

public class GoalEntityTargetFollow extends GoalEntityTarget {

    private double followDistance = GoalDistance.TALK;

    public GoalEntityTargetFollow(GoalType type, VirtualEntity entity, Pathfinder pathfinder, McmeEntity target) {
        super(type, entity, pathfinder, target);
    }

    @Override
    public void doTick() {
        super.doTick();
        if(isCloseToTarget(followDistance)) {
//Logger.getGlobal().info("delete path as entity is close.");
            EntitiesPlugin.getEntityServer().handleEvent(new GoalVirtualEntityIsClose(getEntity(),this));
            setIsMoving(false);//deletePath();
            movementSpeed = MovementSpeed.STAND;
            setRotation(getEntity().getLocation().clone().setDirection(getTarget().getLocation().toVector()
                                                         .subtract(getEntity().getLocation().toVector())).getYaw());
        } else {
            setIsMoving(true);
            movementSpeed = MovementSpeed.WALK;
        }
    }

    /*@Override
    public void update() {
        if(!(isCloseToTarget(GoalDistance.CAUTION))) {
            super.update();
        }
    }*/

    public void setFollowDistance(double distance) {
        this.followDistance = distance;
    }

}
