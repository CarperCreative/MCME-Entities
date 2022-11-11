package com.mcmiddleearth.entities.entities.composite.animation;

import com.comphenix.protocol.wrappers.Pair;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcmiddleearth.entities.entities.composite.BakedAnimationEntity;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.entities.composite.bones.BoneThreeAxis;
import com.mcmiddleearth.entities.entities.composite.bones.BoneTwoAxis;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Frame implements Cloneable {

    private final List<Pair<String, BoneData>> bonesData;
    private final Map<Bone, BoneData> bones = new HashMap<>();

    public Frame(List<Pair<String, BoneData>> bonesData) {
        this.bonesData = bonesData;
    }

    public Frame() {
        this(new ArrayList<>());
    }

    public List<Pair<String, BoneData>> getBonesData() {
        return this.bonesData;
    }

    public void addBoneData(String key, BoneData boneData) {
        this.bonesData.add(new Pair<>(key, boneData));
    }

    public void apply(int state) {
        this.bones.forEach((bone, boneData) -> {
            bone.setRelativePosition(boneData.getPosition());

            bone.setHeadPose(boneData.getHeadPose());
            bone.setHeadItem(boneData.getItems()[state]);
        });
    }

    public void initFrame(BakedAnimationEntity entity) {
        for (final Pair<String, BoneData> pair : this.bonesData) {
            final String key = pair.getFirst();
            final BoneData boneData = pair.getSecond();

            Bone bone = this.getBoneFromEntity(entity, key);
            if (bone != null) {
                this.bones.put(bone, boneData);
                entity.getStates().putAll(boneData.getStates());
                continue;
            }

            final boolean headBone = key.startsWith("head");
            final int headPoseDelay = entity.getHeadPoseDelay();

            switch (entity.getRotationMode()) {
                case YAW:
                    if (headBone) {
                        bone = new BoneTwoAxis(key, entity, boneData.getHeadPose(),
                            boneData.getPosition(), boneData.getItems()[0], true, headPoseDelay
                        );
                    } else {
                        bone = new Bone(key, entity, boneData.getHeadPose(),
                            boneData.getPosition(), boneData.getItems()[0], false, headPoseDelay
                        );
                    }
                    break;
                case YAW_PITCH:
                    bone = new BoneTwoAxis(key, entity, boneData.getHeadPose(),
                        boneData.getPosition(), boneData.getItems()[0], headBone, headPoseDelay
                    );
                    break;
                case YAW_PITCH_ROLL:
                    bone = new BoneThreeAxis(key, entity, boneData.getHeadPose(),
                        boneData.getPosition(), boneData.getItems()[0], headBone, headPoseDelay
                    );
                    break;
            }

            entity.getBones().add(bone);
            entity.getStates().putAll(boneData.getStates());
            this.bones.put(bone, boneData);
        }
    }

    @Override
    public Frame clone() {
        try {
            final Frame clone = (Frame) super.clone();

            return new Frame(clone.getBonesData());
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private Bone getBoneFromEntity(BakedAnimationEntity entity, String boneName) {
        for (final Bone bone : entity.getBones()) {
            if (bone.getName().equals(boneName)) {
                return bone;
            }
        }
        return null;
    }

    public static Frame loadFrame(JsonObject data, Material itemMaterial) {
        final Set<Map.Entry<String, JsonElement>> entries = data.get("bones").getAsJsonObject().entrySet();
        final Frame frame = new Frame();

        entries.forEach(entry -> {
            final BoneData boneData = BoneData.loadBoneData(entry.getValue().getAsJsonObject(), itemMaterial);
            frame.addBoneData(entry.getKey(), boneData);
        });
        return frame;
    }
}
