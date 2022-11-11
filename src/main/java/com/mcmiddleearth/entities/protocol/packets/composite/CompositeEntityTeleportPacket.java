package com.mcmiddleearth.entities.protocol.packets.composite;

import com.mcmiddleearth.entities.entities.composite.CompositeEntity;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.protocol.packets.AbstractPacket;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class CompositeEntityTeleportPacket extends AbstractPacket {

    private final CompositeEntity entity;

    public CompositeEntityTeleportPacket(CompositeEntity entity) {
        this.entity = entity;
    }

    @Override
    public void send(Player recipient) {
        for (final Bone bone : this.entity.getBones()) {
            bone.getTeleportPacket().send(recipient);
            bone.getMetaPacket().send(recipient);
        }
    }

    @Override
    public void update() {
        for (final Bone bone : this.entity.getBones()) {
            bone.getTeleportPacket().update();
            bone.getMetaPacket().update();
        }

    }
}
