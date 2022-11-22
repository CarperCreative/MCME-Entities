package com.mcmiddleearth.entities.entities.simple;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.api.ActionType;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import com.mcmiddleearth.entities.protocol.packets.simple.SimpleEntityAnimationPacket;
import com.mcmiddleearth.entities.protocol.packets.simple.SimpleEntityStatusPacket;
import com.mcmiddleearth.entities.protocol.packets.simple.SimpleLivingEntitySpawnPacket;
import org.bukkit.scheduler.BukkitRunnable;

public class SimpleLivingEntity extends SimpleEntity {

    protected SimpleEntityAnimationPacket animationPacket;

    private ActionType animation = null;

    public SimpleLivingEntity(int entityId, VirtualEntityFactory factory) throws InvalidLocationException, InvalidDataException {
        super(entityId, factory);
        spawnPacket = new SimpleLivingEntitySpawnPacket(this);
        animationPacket = new SimpleEntityAnimationPacket(entityId);
    }

    @Override
    public void doTick() {
        if(animation!=null) {
            switch(animation) {
                case HURT:
                    animationPacket.setAnimation(SimpleEntityAnimationPacket.AnimationType.TAKE_DAMAGE);
                    animationPacket.send(getViewers());
                    break;
                case ATTACK:
                    animationPacket.setAnimation(SimpleEntityAnimationPacket.AnimationType.SWING_MAIN_ARM);
                    animationPacket.send(getViewers());
                    break;
                case DEATH:
                    ((SimpleEntityStatusPacket) statusPacket).setStatusCode(SimpleEntityStatusPacket.StatusCode.ENTITY_DEATH);
                    statusPacket.send(getViewers());
                    break;
            }
            animation = null;
        }
        super.doTick();
    }

    @Override
    public void playAnimation(ActionType type, boolean manualOverride, Payload payload, int delay) {
        this.animation = type;
        new BukkitRunnable() {
            @Override
            public void run() {
                payload.execute();
            }
        }.runTaskLater(EntitiesPlugin.getInstance(),delay);
    }

}
