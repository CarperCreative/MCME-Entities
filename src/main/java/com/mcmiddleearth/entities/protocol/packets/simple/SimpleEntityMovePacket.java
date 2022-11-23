package com.mcmiddleearth.entities.protocol.packets.simple;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.protocol.packets.AbstractPacket;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SimpleEntityMovePacket extends AbstractPacket {

    private final PacketContainer move;
    private final PacketContainer moveLook;
    private final PacketContainer head;

    private final McmeEntity entity;

    private MoveType moveType;

    public SimpleEntityMovePacket(McmeEntity entity) {
        this.entity = entity;
        moveLook = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        moveLook.getIntegers().write(0,entity.getEntityId());

        move = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE);
        move.getIntegers().write(0, entity.getEntityId());

        head = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        head.getIntegers().write(0, entity.getEntityId());

        update();
    }

    @Override
    public void update() {
        moveType = getMoveType();
        switch (moveType) {
            case MOVE:
                Vector dir = getShift();
                move.getShorts()
                        .write(0, (short) (dir.getBlockX() > 0 ? Math.min(dir.getBlockX(),32767) : Math.max(dir.getBlockX(),-32768)))
                        .write(1, (short) (dir.getBlockY() > 0 ? Math.min(dir.getBlockY(),32767) : Math.max(dir.getBlockY(),-32768)))
                        .write(2, (short) (dir.getBlockZ() > 0 ? Math.min(dir.getBlockZ(),32767) : Math.max(dir.getBlockZ(),-32768)));
                move.getBooleans().write(0,entity.onGround());
                break;
            /*case LOOK:
                byte yaw = getAngle(entity.getRotation());
                byte pitch = getAngle(entity.getLocation().getPitch());
                look.getBytes()
                        .write(0, yaw)
                        .write(1, pitch);
                look.getBooleans().write(0,entity.onGround());
                break;*/
            case LOOK:
            case STAND:
            case MOVE_LOOK:
                dir = getShift();
                byte yaw = getAngle(entity.getYaw());
                byte pitch = getAngle(entity.getHeadPitch());
//Logger.getGlobal().info("write packet: "+yaw+" "+pitch+" head: "+getAngle(entity.getLocation().getYaw()));
                moveLook.getShorts()
                        .write(0, (short) (dir.getBlockX() > 0 ? Math.min(dir.getBlockX(),32767) : Math.max(dir.getBlockX(),-32768)))
                        .write(1, (short) (dir.getBlockY() > 0 ? Math.min(dir.getBlockY(),32767) : Math.max(dir.getBlockY(),-32768)))
                        .write(2, (short) (dir.getBlockZ() > 0 ? Math.min(dir.getBlockZ(),32767) : Math.max(dir.getBlockZ(),-32768)));
                moveLook.getBytes()
                        .write(0, yaw)
                        .write(1, pitch);
                moveLook.getBooleans().write(0,entity.onGround());
                break;
        }
        if(entity.hasLookUpdate()) {
            head.getBytes().write(0,getAngle(entity.getHeadYaw()));
        }
    }

    @Override
    public void send(Player recipient) {
//Logger.getGlobal().info(""+moveType.name());
        switch(moveType) {
            case MOVE:
                send(move, recipient);
                break;
            case LOOK:
            case STAND:
            case MOVE_LOOK:
//Logger.getGlobal().info("send movelook to : "+recipient.getName()+" "+moveLook.getShorts().read(0)
//        +" "+moveLook.getShorts().read(1)+" "+moveLook.getShorts().read(2));
                send(moveLook, recipient);
                break;
        }
        if(entity.hasLookUpdate()) {
            send(head,recipient);
        }
    }

    private Vector getShift() {
/*if(entity.getName().equals("bone4")) {
    Logger.getGlobal().info("Packet velo: "+entity.getVelocity());
}*/
        return entity.getVelocity().clone().multiply(32*128);
    }

    private byte getAngle(float bukkitAngle) {
        return (byte)(bukkitAngle*256/360);
    }

    private MoveType getMoveType() {
        Vector velocity = entity.getVelocity();
        if(velocity.getX() == 0 && velocity.getY() == 0 && velocity.getZ() == 0) {
            if(entity.hasLookUpdate() || entity.hasRotationUpdate()) {
                return MoveType.LOOK;
            } else {
                return MoveType.STAND;
            }
        } else {
            if(entity.hasLookUpdate() || entity.hasRotationUpdate()) {
                return MoveType.MOVE_LOOK;
            } else {
                return MoveType.MOVE;
            }
        }
    }

    public enum MoveType {
        STAND, MOVE, MOVE_LOOK, LOOK
    }
}
