package com.mcmiddleearth.entities.ai.goal.head;

import com.mcmiddleearth.entities.ai.goal.Goal;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.Placeholder;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;

public class HeadGoalWatch extends HeadGoal {

    private McmeEntity target;

    private McmeEntity entity;

    public HeadGoalWatch(McmeEntity target, McmeEntity entity) {
        this.target = target;
        this.entity = entity;
    }

    public HeadGoalWatch(McmeEntity target, McmeEntity entity, int duration) {
        this(target, entity);
        setDuration(duration);
    }

    public void setEntity(VirtualEntity entity) {
        this.entity = entity;
    }

    @Override
    public void doTick() {
        if(target != null && !(target instanceof Placeholder)) {
            final Location location = this.entity.getLocation().clone();
            final Location targetLocation = this.target.getLocation().clone();

            final Location orientation = location.setDirection(
                targetLocation.toVector().subtract(
                    location.toVector()
                )
            );
            this.yaw = orientation.getYaw();
            this.pitch = orientation.getPitch();
        }
    }

    public McmeEntity getTarget() {
        return target;
    }

    public void setTarget(McmeEntity target) {
        this.target = target;
    }

    @Override
    public boolean provideGoalAndEntity(Goal goal, McmeEntity entity) {
        if(entity != null) {
            this.entity = entity;
            return true;
        }
        return false;
    }


}
