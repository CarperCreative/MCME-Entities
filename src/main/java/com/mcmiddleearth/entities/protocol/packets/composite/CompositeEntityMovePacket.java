package com.mcmiddleearth.entities.protocol.packets.composite;

import com.mcmiddleearth.entities.entities.composite.CompositeEntity;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.protocol.packets.AbstractPacket;
import org.bukkit.entity.Player;

public class CompositeEntityMovePacket extends AbstractPacket {

    private final CompositeEntity entity;

    public CompositeEntityMovePacket(CompositeEntity entity) {
        this.entity = entity;
    }

    @Override
    public void send(Player recipient) {
        for (final Bone bone : this.entity.getBones()) {
            bone.getMovePacket().send(recipient);
            bone.getMetaPacket().send(recipient);
        }
    }

    @Override
    public void update() {
        for (final Bone bone : this.entity.getBones()) {
            bone.getMovePacket().update();
            bone.getMetaPacket().update();
        }
    }
}
