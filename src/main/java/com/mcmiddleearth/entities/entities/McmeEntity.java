package com.mcmiddleearth.entities.entities;

import com.mcmiddleearth.entities.ai.goal.Goal;
import com.mcmiddleearth.entities.ai.movement.EntityBoundingBox;
import com.mcmiddleearth.entities.api.ActionType;
import com.mcmiddleearth.entities.api.McmeEntityType;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.MovementType;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface McmeEntity extends Entity, InventoryHolder {

    UUID getUniqueId();

    String getName();

    Location getLocation();

    void setLocation(Location location);

    McmeEntityType getMcmeEntityType();

    Vector getVelocity();

    void setVelocity(Vector velocity);

    Location getTarget();

    Goal getGoal();

    void setGoal(Goal goal);

    void doTick();

    int getEntityId();

    int getEntityQuantity();

    boolean hasLookUpdate();

    boolean hasRotationUpdate();

    public Set<String> getTagList();

    public default boolean hasTag(String tag) {
        for(String search: getTagList()) {
            if(search.equals(tag)) return true;
        }
        return false;
    }

    public default void addTag(String tag) {
        getTagList().add(tag);
    }

    public default void removeTag(String tag) {
        getTagList().remove(tag);
    }
    //public boolean onGround();

    float getYaw();
    float getPitch();
    float getRoll();

    float getHeadYaw();
    float getHeadPitch();

    Vector getHeadPosition();

    void setRotation(float yaw);

    void setRotation(float yaw, float pitch, float roll);

    EntityBoundingBox getEntityBoundingBox();

    double getHealth();
    void damage(double damage);
    void heal(double damage);
    boolean isDead();

    boolean isTerminated();

    void playAnimation(ActionType type);

    void receiveAttack(McmeEntity damager, double damage, double knockDownFactor);
    void attack(McmeEntity target);

    Set<McmeEntity> getEnemies();

    void finalise();

    Vector getMouth();

    boolean onGround();

    MovementType getMovementType();

    MovementSpeed getMovementSpeed();

    //public ActionType getActionType();

    boolean hasId(int entityId);

    void setInvisible(boolean visible);

    void setEquipment(EquipmentSlot slot, ItemStack item);

    boolean isOnline();

    void addPotionEffect(PotionEffect effect);

    void removePotionEffect(PotionEffect effect);

    void addItem(ItemStack item, EquipmentSlot slot, int slotId);

    void removeItem(ItemStack item);

    Inventory getInventory();

    //default methods for interfaces InventoryHolder and Entity
    @Override
    default @Nullable Location getLocation(@Nullable Location loc) {
        return null;
    }

    @Override
    default double getHeight() {
        return 0;
    }

    @Override
    default double getWidth() {
        return 0;
    }

    @Override
    default @NotNull BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    default boolean isInWater() {
        return false;
    }

    @Override
    default @NotNull World getWorld() {
        return null;
    }

    @Override
    default void setRotation(float yaw, float pitch) {

    }

    @Override
    default boolean teleport(@NotNull Location location) {
        return false;
    }

    @Override
    default boolean teleport(@NotNull Location location, PlayerTeleportEvent.@NotNull TeleportCause cause) {
        return false;
    }

    @Override
    default boolean teleport(@NotNull Entity destination) {
        return false;
    }

    @Override
    default boolean teleport(@NotNull Entity destination, PlayerTeleportEvent.@NotNull TeleportCause cause) {
        return false;
    }

    @Override
    default @NotNull List<Entity> getNearbyEntities(double x, double y, double z) {
        return null;
    }

    @Override
    default int getFireTicks() {
        return 0;
    }

    @Override
    default int getMaxFireTicks() {
        return 0;
    }

    @Override
    default void setFireTicks(int ticks) {

    }

    @Override
    default void remove() {

    }

    @Override
    default boolean isValid() {
        return false;
    }

    @Override
    default void sendMessage(@NotNull String message) {

    }

    @Override
    default void sendMessage(@NotNull String[] messages) {

    }

    @Override
    default void sendMessage(@Nullable UUID sender, @NotNull String message) {

    }

    @Override
    default void sendMessage(@Nullable UUID sender, @NotNull String[] messages) {

    }

    @Override
    default @NotNull Server getServer() {
        return null;
    }

    @Override
    default boolean isPersistent() {
        return false;
    }

    @Override
    default void setPersistent(boolean persistent) {

    }

    @Override
    default @Nullable Entity getPassenger() {
        return null;
    }

    @Override
    default boolean setPassenger(@NotNull Entity passenger) {
        return false;
    }

    @Override
    default @NotNull List<Entity> getPassengers() {
        return null;
    }

    @Override
    default boolean addPassenger(@NotNull Entity passenger) {
        return false;
    }

    @Override
    default boolean removePassenger(@NotNull Entity passenger) {
        return false;
    }

    @Override
    default boolean isEmpty() {
        return false;
    }

    @Override
    default boolean eject() {
        return false;
    }

    @Override
    default float getFallDistance() {
        return 0;
    }

    @Override
    default void setFallDistance(float distance) {

    }

    @Override
    default void setLastDamageCause(@Nullable EntityDamageEvent event) {

    }

    @Override
    default @Nullable EntityDamageEvent getLastDamageCause() {
        return null;
    }

    @Override
    default int getTicksLived() {
        return 0;
    }

    @Override
    default void setTicksLived(int value) {

    }

    @Override
    default void playEffect(@NotNull EntityEffect type) {

    }

    @Override
    default @NotNull EntityType getType() {
        return null;
    }

    @Override
    default boolean isInsideVehicle() {
        return false;
    }

    @Override
    default boolean leaveVehicle() {
        return false;
    }

    @Override
    default @Nullable Entity getVehicle() {
        return null;
    }

    @Override
    default void setCustomNameVisible(boolean flag) {

    }

    @Override
    default boolean isCustomNameVisible() {
        return false;
    }

    @Override
    default void setGlowing(boolean flag) {

    }

    @Override
    default boolean isGlowing() {
        return false;
    }

    @Override
    default void setInvulnerable(boolean flag) {

    }

    @Override
    default boolean isInvulnerable() {
        return false;
    }

    @Override
    default boolean isSilent() {
        return false;
    }

    @Override
    default void setSilent(boolean flag) {

    }

    @Override
    default boolean hasGravity() {
        return false;
    }

    @Override
    default void setGravity(boolean gravity) {

    }

    @Override
    default int getPortalCooldown() {
        return 0;
    }

    @Override
    default void setPortalCooldown(int cooldown) {

    }

    @Override
    default @NotNull Set<String> getScoreboardTags() {
        return null;
    }

    @Override
    default boolean addScoreboardTag(@NotNull String tag) {
        return false;
    }

    @Override
    default boolean removeScoreboardTag(@NotNull String tag) {
        return false;
    }

    @Override
    default @NotNull PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    @Override
    default @NotNull BlockFace getFacing() {
        return null;
    }

    @Override
    default @NotNull Pose getPose() {
        return null;
    }

    @Override
    default @NotNull Spigot spigot() {
        return null;
    }

    @Override
    default @Nullable Location getOrigin() {
        return null;
    }

    @Override
    default boolean fromMobSpawner() {
        return false;
    }

    @Override
    default @NotNull Chunk getChunk() {
        return null;
    }

    @Override
    default CreatureSpawnEvent.@NotNull SpawnReason getEntitySpawnReason() {
        return null;
    }

    @Override
    default boolean isInRain() {
        return false;
    }

    @Override
    default boolean isInBubbleColumn() {
        return false;
    }

    @Override
    default boolean isInWaterOrRain() {
        return false;
    }

    @Override
    default boolean isInWaterOrBubbleColumn() {
        return false;
    }

    @Override
    default boolean isInWaterOrRainOrBubbleColumn() {
        return false;
    }

    @Override
    default boolean isInLava() {
        return false;
    }

    @Override
    default boolean isTicking() {
        return false;
    }

    @Override
    default @Nullable Component customName() {
        return null;
    }

    @Override
    default void customName(@Nullable Component customName) {

    }

    @Override
    default @Nullable String getCustomName() {
        return null;
    }

    @Override
    default void setCustomName(@Nullable String name) {

    }

    @Override
    default void setMetadata(@NotNull String metadataKey, @NotNull MetadataValue newMetadataValue) {

    }

    @Override
    default @NotNull List<MetadataValue> getMetadata(@NotNull String metadataKey) {
        return null;
    }

    @Override
    default boolean hasMetadata(@NotNull String metadataKey) {
        return false;
    }

    @Override
    default void removeMetadata(@NotNull String metadataKey, @NotNull Plugin owningPlugin) {

    }

    @Override
    default boolean isPermissionSet(@NotNull String name) {
        return false;
    }

    @Override
    default boolean isPermissionSet(@NotNull Permission perm) {
        return false;
    }

    @Override
    default boolean hasPermission(@NotNull String name) {
        return false;
    }

    @Override
    default boolean hasPermission(@NotNull Permission perm) {
        return false;
    }

    @Override
    default @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        return null;
    }

    @Override
    default @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return null;
    }

    @Override
    default @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        return null;
    }

    @Override
    default @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return null;
    }

    @Override
    default void removeAttachment(@NotNull PermissionAttachment attachment) {

    }

    @Override
    default void recalculatePermissions() {

    }

    @Override
    default @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return null;
    }

    @Override
    default boolean isOp() {
        return false;
    }

    @Override
    default void setOp(boolean value) {

    }

    @Override
    default @NotNull PersistentDataContainer getPersistentDataContainer() {
        return null;
    }
}
