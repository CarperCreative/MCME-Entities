package com.mcmiddleearth.entities.ai.goal.head;

import com.mcmiddleearth.entities.ai.goal.GoalEntityTarget;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import org.bukkit.Location;

public class HeadGoalEntityTarget extends HeadGoal {

    private final GoalEntityTarget goal;

    public HeadGoalEntityTarget(GoalEntityTarget goal) {
        this.goal = goal;
    }

    public HeadGoalEntityTarget(GoalEntityTarget goal, int duration) {
        this(goal);
        setDuration(duration);
    }

    @Override
    public void doTick() {
        Location target = goal.getTarget().getLocation();
        VirtualEntity entity = goal.getEntity();
        if(target!=null) {
            Location targetDir = entity.getLocation().clone()
                    .setDirection(target.toVector()
                            .subtract(entity.getLocation().toVector()));
            yaw = targetDir.getYaw();
            pitch = targetDir.getPitch();
        }
    }
}
