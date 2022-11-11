package com.mcmiddleearth.entities.protocol.packets.composite;

import com.mcmiddleearth.entities.entities.composite.CompositeEntity;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.protocol.packets.AbstractPacket;
import org.bukkit.entity.Player;

public class CompositeEntitySpawnPacket extends AbstractPacket {

    private final CompositeEntity entity;

    public CompositeEntitySpawnPacket(CompositeEntity entity) {
        this.entity = entity;
    }

    @Override
    public void send(Player recipient) {
        for (final Bone bone : this.entity.getBones()) {
            bone.getSpawnPacket().send(recipient);

            if (bone.getDisplayName() != null) {
                bone.getNamePacket().send(recipient);
            }

            bone.getInitPacket().send(recipient);
        }
    }

    public void update() {
        for (final Bone bone : this.entity.getBones()) {
            bone.getSpawnPacket().update();
            bone.getInitPacket().update();
        }
    }

}
