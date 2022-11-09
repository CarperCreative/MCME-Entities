package com.mcmiddleearth.entities.entities.composite;

import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.MovementType;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.composite.bones.BoneThreeAxis;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class WingedFlightEntity extends BakedAnimationEntity {
    private float currentRoll, currentPitch;

    private float maxRotationStepFlight = 2f;

    private final Vector attackPoint;

    public WingedFlightEntity(int entityId, VirtualEntityFactory factory) throws InvalidLocationException, InvalidDataException {
        super(entityId, factory, RotationMode.YAW_PITCH_ROLL);
        maxRotationStepFlight = factory.getMaxRotationStepFlight();
        currentRoll = factory.getRoll();
        currentPitch = getPitch();
        instantAnimationSwitching = false;
        attackPoint = factory.getAttackPoint();
    }

    @Override
    protected void updateBodyBones() {
        if(getMovementType().equals(MovementType.FLYING) || getMovementType().equals(MovementType.GLIDING)) {
            currentPitch = turn(currentPitch, getLocation().getPitch(), maxRotationStepFlight);
            float yawDiff = getLocation().getYaw() - currentYaw;
            while (yawDiff < -180) yawDiff += 360;
            while (yawDiff > 180) yawDiff -= 360;
            float rollTarget;
            if(yawDiff>0) {
                rollTarget = Math.min(70, yawDiff * 1.6f);
            } else {
                rollTarget = Math.max(-70, yawDiff * 1.6f);
            }
            currentRoll = turn(currentRoll, rollTarget, maxRotationStepFlight);
            currentYaw = turn(currentYaw, getLocation().getYaw(), Math.max(Math.abs(maxRotationStepFlight / 60 * currentRoll),maxRotationStepFlight/4f));
            getBones().stream().filter(bone -> !bone.isHeadBone() || !goalHasHeadControl()).forEach(bone -> bone.setRotation(currentYaw, currentPitch, -currentRoll));
        } else {
            getLocation().setPitch(0);
            currentPitch = 0;
            currentRoll = 0;
            super.updateBodyBones();
        }
    }

    @Override
    public float getRoll() {
        return currentRoll;
    }

    @Override
    public float getPitch() {
        return getLocation().getPitch();
    }

    @Override
    public void setRotation(float yaw, float pitch, float roll) {
        this.getLocation().setPitch(pitch);
        super.setRotation(yaw,pitch,roll);
        //currentRoll = roll;
    }

    @Override
    public void setMovementType(MovementType movementType) {
        super.setMovementType(movementType);
        currentRoll = 0; // When updating movement type set roll to 0
        currentPitch = 0;
    }

    @Override
    public boolean hasRotationUpdate() {
        return currentPitch!=getPitch() || currentYaw != getLocation().getYaw();
    }

    public float getCurrentPitch() {
        return currentPitch;
    }

    public float getCurrentYaw() {
        return currentYaw;
    }

    public float getMaxRotationStepFlight() {
        return maxRotationStepFlight;
    }

    public void setMaxRotationStepFlight(float maxRotationStepFlight) {
        this.maxRotationStepFlight = maxRotationStepFlight;
    }

    public Vector getAttackPoint() {
        return attackPoint;
    }

    @Override
    public VirtualEntityFactory getFactory() {
        VirtualEntityFactory factory = super.getFactory()
                .withRoll(currentRoll)
                .withMaxRotationStepFlight(maxRotationStepFlight)
                .withAttackPoint(attackPoint);
        return factory;
    }

    @Override
    public MovementSpeed getMovementSpeedAnimation() {
        if(getMovementType() == MovementType.FLYING || getMovementType() == MovementType.GLIDING){
            float angle = getVelocity().angle(new Vector(getVelocity().getX(),0,getVelocity().getZ()));
            if(angle < 0.174) { //10 * PI / 180
                return MovementSpeed.WALK;
            } else if(getVelocity().getY() < 0) {
                return MovementSpeed.SPRINT;
            } else {
                return MovementSpeed.SLOW;
            }
        }
        else {
            return super.getMovementSpeedAnimation();
        }
    }
}
