package com.mcmiddleearth.entities.protocol.packets.composite;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.protocol.packets.AbstractPacket;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BoneMetaPacket extends AbstractPacket {

    protected PacketContainer posePacket;
    protected PacketContainer equipPacket;

    protected final Bone bone;

    private boolean hasPoseUpdate, hasItemUpdate;

    protected final List<WrappedWatchableObject> storedPoseObjects = new ArrayList<>();
    private final List<WrappedDataWatcher> headPoseQueue = new ArrayList<>();

    private WrappedWatchableObject headRotationObject = null;

    public BoneMetaPacket(Bone bone, int headPoseDelay) {
        this.bone = bone;
        posePacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        posePacket.getIntegers().write(0,bone.getEntityId());

        equipPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        equipPacket.getIntegers().write(0,bone.getEntityId());

        for(int i = 0; i < headPoseDelay; i++) {
            headPoseQueue.add(null);
        }
        //headPoseQueue.add(null);
        update();
    }

    @Override
    public void update() {
        if(bone.isHasHeadPoseUpdate()) {
            WrappedDataWatcher watcher = new WrappedDataWatcher();
            headPoseQueue.add(watcher);
        } else {
            headPoseQueue.add(null);
        }
        if(headPoseQueue.get(0)!=null) {
            posePacket.getWatchableCollectionModifier().write(0,headPoseQueue.get(0).getWatchableObjects());
            hasPoseUpdate = true;
        } else {
            hasPoseUpdate = false;
        }
        headPoseQueue.remove(0);
        if(bone.isHasItemUpdate()) {
            writeHeadItem();
            hasItemUpdate = true;
        } else {
            hasItemUpdate = false;
        }
    }

    protected void writeHeadPose(WrappedDataWatcher watcher) {
        this.writeHeadPose(watcher, false);
    }

    protected void writeHeadPose(WrappedDataWatcher watcher, boolean store) {
        WrappedDataWatcher.WrappedDataWatcherObject state = new WrappedDataWatcher
            .WrappedDataWatcherObject(15, WrappedDataWatcher.Registry.getVectorSerializer());
        final Vector3F vector3F = new Vector3F(
            (float)this.bone.getRotatedHeadPose().getX(),
            (float)this.bone.getRotatedHeadPose().getY(),
            (float)this.bone.getRotatedHeadPose().getZ()
        );

        if(!store) {
            watcher.setObject(state, vector3F, false);
            return;
        }

        if(this.headRotationObject == null) {
            final Optional<WrappedWatchableObject> watchableObjectOptional = this.storedPoseObjects.stream()
                .filter(object -> object.getIndex() == state.getIndex())
                .findFirst();

            if(!watchableObjectOptional.isPresent()) {
                return;
            }

            this.headRotationObject = watchableObjectOptional.get();
        }

        this.headRotationObject.setValue(vector3F);
    }

    protected void writeHeadItem() {
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = new ArrayList<>();
        equipment.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, bone.getHeadItem()));
        equipPacket.getSlotStackPairLists().write(0, equipment);
    }

    @Override
    public void send(Player recipient) {
        if(hasPoseUpdate) {
            send(posePacket, recipient);
//Logger.getGlobal().info("send bone pose");
        }
        if(hasItemUpdate) {
            send(equipPacket, recipient);
//Logger.getGlobal().info("send bone item");
        }
    }
}
