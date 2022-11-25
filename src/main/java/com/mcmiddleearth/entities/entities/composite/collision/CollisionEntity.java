package com.mcmiddleearth.entities.entities.composite.collision;

import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.util.stream.Stream;

public interface CollisionEntity extends Entity {
    /**
     * @return Stream of bounding boxes in this entity's local space. The instances must be cloned before modification.
     */
    public Stream<BoundingBox> getColliders();

    /**
     * @return The axis-aligned bounding box surrounding all of this entity's colliders.
     */
    public BoundingBox getCollisionBoundingBox();

    /**
     * Recalculates the positions and sizes of all of this entity's colliders.
     */
    public void updateColliders();
}
