package com.mcmiddleearth.entities.entities;

import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import com.mcmiddleearth.entities.protocol.packets.*;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public abstract class SimpleEntity extends VirtualEntity {

    int entityId;

    protected AbstractPacket namePacket;


    public SimpleEntity(int entityId, VirtualEntityFactory factory) throws InvalidLocationException, InvalidDataException {
        super(factory);
        this.entityId = entityId;
        teleportPacket = new SimpleEntityTeleportPacket(this);
        movePacket = new SimpleEntityMovePacket(this);
        removePacket = new VirtualEntityDestroyPacket(entityId);
        statusPacket = new SimpleEntityStatusPacket(entityId);
        namePacket = new DisplayNamePacket(entityId);
        if(this.getDisplayName()!=null) ((DisplayNamePacket)namePacket).setName(this.getDisplayName());
    }

    @Override
    public int getEntityId() {
        return entityId;
    }

    @Override
    public int getEntityQuantity() {
        return 1;
    }

    @Override
    public synchronized void addViewer(Player player) {
        super.addViewer(player);
//Logger.getGlobal().info("Send Display Name: "+ getDisplayName());
        if(getDisplayName()!=null) {
            namePacket.send(player);
        }
    }

    public void setDisplayName(String displayName) {
        super.setDisplayName(displayName);
        ((DisplayNamePacket)namePacket).setName(displayName);
        namePacket.send(getViewers());
    }


}
