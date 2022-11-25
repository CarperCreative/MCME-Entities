package com.mcmiddleearth.entities.entities.composite.collision;

import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class BoneAttachedCollider {
    /**
     * Offset relative to bone origin to shift the bounding box by. Rotated with the bone.
     *
     * May be null, indicating that no shift should be performed.
     */
    private final Vector offset;
    private final Vector halfSize;
    private final BoundingBox boundingBox = new BoundingBox();
    private final Bone boundBone;

    public BoneAttachedCollider(Bone bindToBone, Vector boneOffset, Vector size) {
        this.offset = boneOffset == null || boneOffset.lengthSquared() < 0.0001d ? null : boneOffset.clone();
        this.halfSize = size.clone().multiply(0.5f);
        this.boundBone = bindToBone;
    }

    /**
     * @return A mutable instance of the bounding box. A copy must be made if modification is necessary.
     */
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void update() {
        // Reset to center
        boundingBox.resize(-halfSize.getX(), -halfSize.getY(), -halfSize.getZ(), halfSize.getX(), halfSize.getY(), halfSize.getZ());

        if (offset != null) {
            EulerAngle rotatedHeadPose = boundBone.getRotatedHeadPose();

            Vector offsetFromBone = offset.clone()
                    // TODO: dunno if armor stands use XYZ order
                    .rotateAroundX(rotatedHeadPose.getX())
                    .rotateAroundY(rotatedHeadPose.getY())
                    .rotateAroundZ(rotatedHeadPose.getZ());

            boundingBox.shift(offsetFromBone);
        }

        // Offset to the head pivot point
        boundingBox.shift(0f, 22f / 16f, 0f);

        // Move to current bone position
        boundingBox.shift(boundBone.getRelativePositionRotated());
    }
}
