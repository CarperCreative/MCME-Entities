package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.head.HeadGoalMimic;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.MovementType;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.listener.MimicListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GoalMimic extends GoalVirtualEntity {

    private Listener listener;

    private McmeEntity mimic;

    private Vector velocity;

    private float bodyYaw;

    private final List<MovementSpeed> lastSpeeds = new ArrayList<>();

    public GoalMimic(VirtualEntity entity, VirtualEntityGoalFactory factory) {
        super(entity, factory);
        this.mimic = factory.getTargetEntity();
        setDefaultHeadGoal();
        bodyYaw = mimic.getYaw();
    }

    @Override
    public Vector getDirection() {
        return new Vector(1,0,0);
    }

    public McmeEntity getMimic() {
        return mimic;
    }

    @Override
    public void doTick() {
        super.doTick();

        double distance = mimic.getLocation().distanceSquared(getEntity().getLocation());
        boolean backward = false;

        if(distance > 0.0001) {
            Vector move = mimic.getLocation().subtract(getEntity().getLocation()).toVector();
            float moveYaw = mimic.getLocation().setDirection(move).getYaw();
            float bukkitYaw = mimic.getLocation().getYaw();
            float yawDiff = moveYaw-bukkitYaw;
            while(yawDiff > 180) yawDiff -= 360;
            while(yawDiff <= -180) yawDiff += 360;

//Logger.getGlobal().info("bukkit: "+mimic.getLocation().getYaw()+" move: "+moveYaw+" Diff: "+yawDiff);
            if(yawDiff > 40 && yawDiff < 100 || yawDiff < -100 && yawDiff > -140 ) {
                bodyYaw = mimic.getLocation().getYaw() + 45;
            } else if(yawDiff < -40 && yawDiff > -140 || yawDiff > 100 && yawDiff < 140) {
                bodyYaw = mimic.getLocation().getYaw() - 45;
            } else {
                bodyYaw = mimic.getLocation().getYaw();
            }
            if(yawDiff > 100 || yawDiff < -100) backward = true;
        } else {
            float bukkitYaw = mimic.getLocation().getYaw();
            while(bukkitYaw < -180) bukkitYaw += 360;
            while(bukkitYaw > 180) bukkitYaw -= 360;
            while(bodyYaw < -180) bodyYaw += 360;
            while(bodyYaw > 180) bodyYaw -= 360;
            if(bukkitYaw > 90 && bodyYaw < -90) bodyYaw += 360;
            if(bukkitYaw < -90 && bodyYaw > 90) bodyYaw -= 360;
//Logger.getGlobal().info("bukkit: "+bukkitYaw+" yaw: "+bodyYaw);
            if(bukkitYaw > bodyYaw +30) {
//Logger.getGlobal().info("minus 30");
                bodyYaw = bukkitYaw-30;
            } else if(bukkitYaw < bodyYaw -30){
//Logger.getGlobal().info("plus 30");
                bodyYaw = bukkitYaw+30;
            }
            while(bodyYaw > 180) bodyYaw -= 360;
            while(bodyYaw <= -180) bodyYaw += 360;
        }
        if(!backward) {
            if (distance < 0.0001) {
                updateMovementSpeed(MovementSpeed.STAND);
            } else if (distance < 0.001) {
                updateMovementSpeed(MovementSpeed.SLOW);
            } else if (distance < 0.1) {
                updateMovementSpeed(MovementSpeed.WALK);
            } else {
                updateMovementSpeed(MovementSpeed.SPRINT);
            }
        } else {
            if (distance < 0.0001) {
                updateMovementSpeed(MovementSpeed.STAND);
            } else if (distance < 0.001) {
                updateMovementSpeed(MovementSpeed.BACKWARD_SLOW);
            } else if (distance < 0.1) {
                updateMovementSpeed(MovementSpeed.BACKWARD_WALK);
            } else {
                updateMovementSpeed(MovementSpeed.BACKWARD_SPRINT);
            }
        }
        //getEntity().setLocation(mimic.getLocation());
        setYaw(bodyYaw);
        velocity = mimic.getLocation().clone().subtract(getEntity().getLocation()).toVector();

        // NKH HP: Force mimicked entities to upright to:
        // a) avoid getting stuck in falling state, and
        // b) always use upright animations as they're the only ones available
        getEntity().setMovementType(MovementType.UPRIGHT);
    }

    private void updateMovementSpeed(MovementSpeed speed) {
        boolean shouldUpdate = true;
        //Check if the last two speeds match, set should update to false if not
        for (MovementSpeed oldSpeed : lastSpeeds) {
            if (!oldSpeed.equals(speed)) {
                shouldUpdate = false;
                break;
            }
        }
        //Add the requested speed to the list
        lastSpeeds.add(speed);
        //Update the movement speed, but only if we had the same speed for the last 2 ticks
        if (shouldUpdate)
            movementSpeed = speed;
        //Clear out old speeds
        while (lastSpeeds.size() > 2)
            lastSpeeds.remove(0);
    }

    public boolean isDirectMovementControl() {
        return true;
    }

    @Override
    public void setDefaultHeadGoal() {
        clearHeadGoals();
        addHeadGoal(new HeadGoalMimic(getEntity(), mimic,10));
    }

    @Override
    public void activate() {
        listener = new MimicListener(getEntity(),mimic);
        Bukkit.getPluginManager().registerEvents(listener, EntitiesPlugin.getInstance());
        mimic.setInvisible(true);
    }

    @Override
    public void deactivate() {
        HandlerList.unregisterAll(listener);
        mimic.setInvisible(false);
    }

    @Override
    public VirtualEntityGoalFactory getFactory() {
        return super.getFactory().withTargetEntity(mimic);
    }

    @Override
    public Vector getVelocity() {
        return velocity;
    }


}
