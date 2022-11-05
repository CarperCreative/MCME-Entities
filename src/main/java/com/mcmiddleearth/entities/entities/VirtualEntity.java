package com.mcmiddleearth.entities.entities;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.ai.goal.Goal;
import com.mcmiddleearth.entities.ai.goal.GoalDistance;
import com.mcmiddleearth.entities.ai.goal.GoalVirtualEntity;
import com.mcmiddleearth.entities.ai.movement.EntityBoundingBox;
import com.mcmiddleearth.entities.ai.movement.MovementEngine;
import com.mcmiddleearth.entities.api.ActionType;
import com.mcmiddleearth.entities.api.EntityAPI;
import com.mcmiddleearth.entities.api.McmeEntityType;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.MovementType;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.attributes.VirtualAttributeFactory;
import com.mcmiddleearth.entities.entities.composite.SpeechBalloonEntity;
import com.mcmiddleearth.entities.entities.composite.bones.SpeechBalloonLayout;
import com.mcmiddleearth.entities.events.events.McmeEntityDamagedEvent;
import com.mcmiddleearth.entities.events.events.McmeEntityDeathEvent;
import com.mcmiddleearth.entities.events.events.McmeEntityEvent;
import com.mcmiddleearth.entities.events.events.goal.GoalChangedEvent;
import com.mcmiddleearth.entities.events.events.virtual.VirtualEntityAttackEvent;
import com.mcmiddleearth.entities.events.events.virtual.VirtualEntityStopTalkEvent;
import com.mcmiddleearth.entities.events.events.virtual.VirtualEntityTalkEvent;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import com.mcmiddleearth.entities.inventory.McmeInventory;
import com.mcmiddleearth.entities.protocol.packets.AbstractPacket;
import com.mcmiddleearth.entities.util.UuidGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public abstract class VirtualEntity implements McmeEntity, Attributable {

    private int viewDistance = 320;

    private final UUID uniqueId;

    private final String name;
    private String displayName;
    private final Map<UUID, SpeechBalloonEntity> speechBallons = new HashMap<>();

    private final Set<Player> viewers = new HashSet<>();

    private final Set<UUID> whiteList;

    private boolean useWhitelistAsBlacklist = false;

    protected int tickCounter = 0;

    protected AbstractPacket spawnPacket;
    protected AbstractPacket removePacket;
    protected AbstractPacket teleportPacket;
    protected AbstractPacket movePacket;
    protected AbstractPacket statusPacket;
    private Location location;

    //private float rotation; //remove and replace with location.yaw

    private Vector velocity = new Vector(0, 0, 0);

    private float headYaw, headPitch;

    private boolean lookUpdate, rotationUpdate;

    private boolean teleported;

    private MovementType movementType;
    private MovementSpeed movementSpeed = MovementSpeed.STAND;

    //private ActionType actionType = ActionType.IDLE;

    private GoalVirtualEntity goal = null;

    private final McmeEntityType type;

    private Map<Attribute, AttributeInstance> attributes = new HashMap<>();

    private final EntityBoundingBox boundingBox;

    private final MovementEngine movementEngine;

    private int updateInterval = 10;

    private final int updateRandom;

    private int jumpHeight = 1, fallDepth = 1; //if both values differ from each other pathfinding can easily get stuck.
    private float knockBackBase = 0.04f, knockBackPerDamage = 0.002f;

    private double health;
    private boolean dead = false;
    private int deathCounter = 0;

    private boolean isTerminated = false;

    private int attackCoolDown = 40;
    private int attackDelay = 0;
    private int hurtCoolDown = 0;

    private Set<McmeEntity> enemies = new HashSet<>();

    private Map<UUID, Integer> soundsStage = new HashMap<>();
    private List<String> triggeredSounds;
    private List<String> subtitles;

    //private String[] speech;
    private boolean isTalking;
    private int speechCounter;
    private SpeechBalloonLayout defaultSpeechBalloonLayout, currentSpeechBalloonLayout;

    private Vector mouth, sitPoint, saddle;

    private org.bukkit.entity.Entity dependingEntity;

    public VirtualEntity(VirtualEntityFactory factory) throws InvalidLocationException, InvalidDataException {
        this.updateInterval = factory.getUpdateInterval();
        this.updateRandom = new Random().nextInt(updateInterval);
        this.type = factory.getType();
        this.location = factory.getLocation();
        this.headYaw = factory.getHeadYaw();
        this.headPitch = factory.getHeadPitch();
        if(factory.getUniqueId() != null) {
            this.uniqueId = factory.getUniqueId();
        } else {
            this.uniqueId = UuidGenerator.fast_random();
        }
        this.name = (factory.getName() != null ? factory.getName() : "unnamed");
        this.attributes = factory.getAttributes();
        this.displayName = factory.getDisplayName();
        this.triggeredSounds = (factory.getTriggeredSounds() != null ? factory.getTriggeredSounds() : new ArrayList<>());
        this.subtitles = (factory.getSubtitles() != null ? factory.getSubtitles() : new ArrayList<>());
        this.useWhitelistAsBlacklist = factory.hasBlackList();
        this.whiteList = (factory.getWhitelist() != null ? factory.getWhitelist() : new HashSet<>());
        this.movementType = factory.getMovementType();
        this.boundingBox = factory.getBoundingBox();
        this.boundingBox.setLocation(location);
        this.movementEngine = new MovementEngine(this);
        this.health = factory.getHealth();
        if (health < 0) {
            AttributeInstance maxHealth = getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) health = maxHealth.getValue();
        }
        this.attackDelay = factory.getAttackDelay();
        this.defaultSpeechBalloonLayout = factory.getSpeechBalloonLayout();
        this.mouth = factory.getMouth();
        this.viewDistance = factory.getViewDistance();
        this.jumpHeight = factory.getJumpHeight();
        this.fallDepth = jumpHeight;
        this.knockBackBase = factory.getKnockBackBase();
        this.knockBackPerDamage = factory.getKnockBackPerDamage();
        this.enemies = (factory.getEnemies() != null ? factory.getEnemies() : new HashSet<>());
        this.sitPoint = factory.getSitPoint();
        this.saddle = factory.getSaddlePoint();
        if (factory.getGoalFactory() != null) {
            this.goal = factory.getGoalFactory().build(this);
            if (this.goal != null) {
                this.goal.activate();
            }
        }
        dependingEntity = factory.getDependingEntity();
    }

    protected VirtualEntity(McmeEntityType type, Location location) {
        this.updateRandom = new Random().nextInt(updateInterval);
        this.type = type;
        this.name = "unnamed";
        this.location = location;
        this.uniqueId = UuidGenerator.fast_random();//UuidGenerator.getRandomV2();
        this.boundingBox = new EntityBoundingBox(0, 0, 0, 0);
        boundingBox.setLocation(location);
        this.movementEngine = null;
        this.whiteList = new HashSet<>();
        this.triggeredSounds = new ArrayList<>();
        this.subtitles = new ArrayList<>();
    }

    @Override
    public void doTick() {
        if (teleported) {
            teleport();
            if (goal != null) {
                goal.update();
            }
        } else {
            if (goal != null) {
                goal.doTick(); //tick before update to enable update do special stuff that doesn't get overridden by doTick
                if (tickCounter % goal.getUpdateInterval() == goal.getUpdateRandom()) {
                    goal.update();
                }
                setMovementSpeed(goal.getMovementSpeed());
                if (goal.isDirectMovementControl()) {
                    velocity = goal.getVelocity();
                } else {
                    movementEngine.calculateMovement(goal.getDirection());
                }
                if (goal.hasRotation()) {
                    setRotation(goal.getYaw(), goal.getPitch(), goal.getRoll());
                }
                if (goal.hasHeadRotation()) {
                    setHeadRotation(goal.getHeadYaw(), goal.getHeadPitch());
                }
                goal.resetRotationFlags();
            } else {
                movementEngine.calculateMovement(new Vector(0, 0, 0));
            }
            move();
            attackCoolDown = Math.max(0, --attackCoolDown);
            hurtCoolDown = Math.max(0, --hurtCoolDown);

        }
        tickCounter++;

        if (isDead() && !isTerminated) {
            deathCounter++;
            if (deathCounter > 20) {
                terminate();
            }
        }
        speechCounter = Math.max(-1, --speechCounter);
        if (speechCounter == 0) {
            McmeEntityEvent event = new VirtualEntityStopTalkEvent(this);
            EntitiesPlugin.getEntityServer().handleEvent(event);
            isTalking = false;
            removeSpeechBalloons();
        }
    }

    public void teleport() {
//if(!(this instanceof Projectile)) Logger.getGlobal().info("teleport");
        teleportPacket.update();
        teleportPacket.send(viewers);
        teleported = false;
        lookUpdate = false;
        rotationUpdate = false;

        spawnPacket.update();
    }

    public void move() {
//Logger.getGlobal().info("move");
//Logger.getGlobal().info("location old: "+ getLocation());
//Logger.getGlobal().info("velocity: "+ velocity+" yaw: "+getRotation()+" head: "+location.getYaw()+" "+location.getPitch());
        location = location.add(velocity);
//if(!(this instanceof Projectile)) Logger.getGlobal().info("velocity: "+ velocity);
//if(!(this instanceof Projectile)) Logger.getGlobal().info("location new: "+ getLocation().getX()+" "+getLocation().getY()+" "+getLocation().getZ());
        boundingBox.setLocation(location);

        if ((tickCounter % updateInterval == updateRandom)) {
            teleportPacket.update();
            teleportPacket.send(viewers);
        } else {
            movePacket.update();
            movePacket.send(viewers);
        }
        lookUpdate = false;
        rotationUpdate = false;

        spawnPacket.update();
    }

    @Override
    public void setLocation(Location location) {
//if(!(this instanceof Projectile)) Logger.getGlobal().info("set location!");
        this.location = location.clone();
        this.boundingBox.setLocation(location);
        teleported = true;
    }

    @Override
    public void setVelocity(Vector velocity) {
/*if (!checkFinite(velocity)) {
    Logger.getGlobal().info("set Velocity: "+velocity.getX()+" "+velocity.getY()+" "+velocity.getZ());
    throw new IllegalArgumentException("Set Velocity");
}*/
        this.velocity = velocity;
    }

    public void setHeadRotation(float yaw, float pitch) {
        // getLocation().setYaw(yaw);
        headYaw = yaw;
        headPitch = pitch;
        lookUpdate = true;
    }

    @Override
    public void setRotation(float yaw) {
        location.setYaw(yaw);
        //rotation = yaw;
        rotationUpdate = true;
    }

    @Override
    public void setRotation(float yaw, float pitch, float roll) {
        location.setPitch(pitch);
        setRotation(yaw);
    }

    @Override
    public float getYaw() {
        return location.getYaw();//rotation;
    }

    @Override
    public float getPitch() {
        return location.getPitch();
    }

    @Override
    public float getRoll() {
        return 0;
    }

    @Override
    public float getHeadPitch() {
        return headPitch;
    }

    @Override
    public float getHeadYaw() {
        return headYaw;
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    @Override
    public void setGoal(Goal goal) {
        if (goal instanceof GoalVirtualEntity && this.goal != goal) {
            GoalChangedEvent event = new GoalChangedEvent(this, this.goal, goal);
            EntitiesPlugin.getEntityServer().handleEvent(event);
            if (!event.isCancelled()) {
                this.goal.deactivate();
                this.goal = (GoalVirtualEntity) goal;
                this.goal.activate();
            }
        }
    }

    @Override
    public Vector getVelocity() {
        return velocity;
    }

    public boolean isSneaking() {
        return movementType.equals(MovementType.SNEAKING);
    }

    /*public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }*/

    @Override
    public MovementSpeed getMovementSpeed() {
        return movementSpeed;
    }

    protected void setMovementSpeed(MovementSpeed movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        if (!this.movementType.equals(MovementType.FALL)
                && movementType.equals(MovementType.FALL)) {
            movementEngine.setFallStart(boundingBox.getMin().getY());
        }
        this.movementType = movementType;
    }

    public boolean onGround() {
        return movementType.equals(MovementType.SNEAKING)
                || movementType.equals(MovementType.UPRIGHT);
    }

    /*public ActionType getActionType() {
        return actionType;
    }*/

    @Override
    public boolean hasLookUpdate() {
        return lookUpdate;
    }

    @Override
    public boolean hasRotationUpdate() {
        return rotationUpdate;
    }

    @Override
    public Location getTarget() {
        return null;
    }

    /*@Override
    public boolean onGround() {
        return true;
    }*/

    public EntityBoundingBox getEntityBoundingBox() {
        return boundingBox;
    }

    @Override
    public McmeEntityType getMcmeEntityType() {
        return type;
    }

    public boolean isViewer(Player player) {
        return viewers.contains(player);
    }

    public Set<Player> getViewers() {
        return viewers;
    }

    public synchronized void addViewer(Player player) {
        if (!player.hasPermission(Permission.VIEWER.getNode())) return;
        if (!useWhitelistAsBlacklist && !(whiteList.isEmpty() || whiteList.contains(player.getUniqueId()))
                || useWhitelistAsBlacklist && whiteList.contains(player.getUniqueId())) {
            return;
        }
        spawnPacket.send(player);
        viewers.add(player);
        if (isTalking) {
            createSpeechBalloon(player);
        }
    }

    public synchronized void removeViewer(Player player) {
        SpeechBalloonEntity balloon = speechBallons.get(player.getUniqueId());
        if (balloon != null) {
            balloon.terminate();
            speechBallons.remove(player.getUniqueId());
        }
        removePacket.send(player);
        viewers.remove(player);
        soundsStage.remove(player.getUniqueId());
    }

    public void removeAllViewers() {
        List<Player> removal = new ArrayList<>(viewers);
//Logger.getGlobal().info("REmove viewers! "+this.getClass().getSimpleName());
        removal.forEach(this::removeViewer);
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    @Override
    public AttributeInstance getAttribute(@NotNull Attribute attribute) {
        return attributes.get(attribute);
    }

    @Override
    public void registerAttribute(@NotNull Attribute attribute) {
        attributes.put(attribute, VirtualAttributeFactory.getAttributeInstance(attribute, null));
    }

    public double getJumpHeight() {
        AttributeInstance instance = getAttribute(Attribute.HORSE_JUMP_STRENGTH);
        return jumpHeight + (instance != null ? instance.getValue() : 0);
        //return jumpHeight;
    }

    public double getFlyingSpeed() {
        AttributeInstance instance = getAttribute(Attribute.GENERIC_FLYING_SPEED);
        if (instance == null) {
            return getGenericSpeed();
        }
//Logger.getGlobal().info("Flyspeed: "+instance);
        return instance.getValue();
    }

    public double getGenericSpeed() {
        AttributeInstance instance = getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
//Logger.getGlobal().info("Using generic: "+instance);
        return (instance != null ? instance.getValue() : 0.1);
    }

    public int getFallDepth() {
        return fallDepth;
    }

    public double getHealth() {
        return health;
    }

    public void damage(double damage) {
        McmeEntityDamagedEvent event = new McmeEntityDamagedEvent(this, damage);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        if (!event.isCancelled()) {
            health -= event.getDamage();
//Logger.getGlobal().info("Damage: "+damage+" Health: "+health);
            if (health <= 0) {
                EntitiesPlugin.getEntityServer().handleEvent(new McmeEntityDeathEvent(this));
                dead = true;
                playAnimation(ActionType.DEATH);
                //Logger.getGlobal().info("Dead!");
            } else {
                playAnimation(ActionType.HURT);
            }
        }
    }

    public void heal(double damage) {
        double maxHealth = 20;
        AttributeInstance attrib = getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attrib != null) maxHealth = attrib.getValue();
        health = Math.min(health + damage, maxHealth);
    }

    public boolean isDead() {
        return dead;
    }

    @Override
    public void receiveAttack(McmeEntity damager, double damage, double knockBackFactor) {
        double defense = 0;
        AttributeInstance attribute = getAttribute(Attribute.GENERIC_ARMOR);
        if (attribute != null) defense = attribute.getValue();
        double toughness = 0;
        attribute = getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
        if (attribute != null) toughness = attribute.getValue();
        damage(damage * (1 - Math.min(20, Math.max(defense / 5, defense - 4 * damage / (toughness + 8))) / 25));
        attribute = getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        double resistance = 0;
        if (attribute != null) resistance = attribute.getValue();
        double length = knockBackFactor * (knockBackBase + Math.max(0, damage - resistance) * knockBackPerDamage);
//Logger.getGlobal().info("Kb base: "+knockBackBase+" per Damage: "+knockBackPerDamage+" resistance: "+resistance+" factor: "+knockBackFactor+" length: "+length);
        Vector normal;
        if (damager != null) {
            normal = damager.getLocation().clone().subtract(location.toVector()).toVector().normalize();
        } else {
            normal = new Vector(0, 1, 0);
        }
        Vector knockBack = normal.multiply(-length).add(new Vector(0, length * 2, 0));
        if (isOnGround()) {
            setMovementType(MovementType.FALL);
        }
        //actionType = ActionType.HURT;
        hurtCoolDown = 10;
//Logger.getGlobal().info("Set Velocity: "+ knockBack.getX()+" "+knockBack.getY()+" "+knockBack.getZ());
        setVelocity(knockBack);
        if (damager != null) {
            enemies.add(damager);
        }
    }

    @Override
    public void attack(McmeEntity target) {
        attack(target, true);
    }

    public void attack(McmeEntity target, boolean animate) {
//Logger.getGlobal().info("Att cool: "+attackCoolDown);
        if (attackCoolDown == 0 && hurtCoolDown == 0) {
            VirtualEntityAttackEvent event = new VirtualEntityAttackEvent(this, target);
            EntitiesPlugin.getEntityServer().handleEvent(event);
            if (!event.isCancelled()) {
                //actionType = ActionType.ATTACK;
//Logger.getGlobal().info("Attack");
                VirtualEntity damager = this;
                Payload attack = new Payload() {
                    public void execute() {
                        if (isCloseToTarget(target, GoalDistance.ATTACK * 3)) {
                            AttributeInstance attribute = getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
                            double damage = 2;
                            if (attribute != null) damage = attribute.getValue();
                            double knockback = 0;
                            attribute = getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK);
                            if (attribute != null) knockback = attribute.getValue();
                            target.receiveAttack(damager, damage, knockback + 1);
                        }
                    }
                };
                if (animate) {
                    playAnimation(ActionType.ATTACK, attack, attackDelay);
                } else {
                    attack.execute();
                }
            }
            attackCoolDown = 40;
            AttributeInstance attribute = getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attribute != null) attackCoolDown = (int) (160 / attribute.getValue());
        }
    }

    private boolean isCloseToTarget(McmeEntity target, double distanceSquared) {
        if (target != null) {
            double distance = getLocation().toVector().distanceSquared(target.getLocation().toVector());
            return distance < distanceSquared;
        } else {
            return false;
        }
    }

    public void shoot(McmeEntity target, McmeEntityType projectile) {
        if (projectile.isProjectile()
                && target.getLocation().getWorld().equals(getLocation().getWorld())
                && attackCoolDown == 0 && hurtCoolDown == 0) {
            AttributeInstance attribute = getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            float damage = 2;
            if (attribute != null) damage = (float) attribute.getValue();
            double knockback = 1;
            attribute = getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK);
            if (attribute != null) knockback = attribute.getValue();
            VirtualEntityFactory factory = new VirtualEntityFactory(projectile, getLocation().clone().add(-0.4, 1.4, 0))
                    .withShooter(this)
                    .withProjectileDamage(damage)
                    .withKnockBackBase((float) knockback)
                    .withKnockBackPerDamage(0);
            Projectile.takeAim(factory, target.getLocation());
            try {
                EntityAPI.spawnEntity(factory);
                attackCoolDown = 40;
                attribute = getAttribute(Attribute.GENERIC_ATTACK_SPEED);
                if (attribute != null) attackCoolDown = (int) (20 / attribute.getValue());
            } catch (InvalidLocationException | InvalidDataException e) {
                e.printStackTrace();
            }
        }
    }

    public int getAttackCoolDown() {
        return attackCoolDown;
    }

    @Override
    public Set<McmeEntity> getEnemies() {
        return enemies;
    }

    public void setEnemies(Set<McmeEntity> enemies) {
        this.enemies = enemies;
    }

    @Override
    public boolean isTerminated() {
        return isTerminated;
    }

    public void terminate() {
        isTerminated = true;
    }

    @Override
    public void finalise() {
        removeSpeechBalloons();
        if (goal != null) goal.deactivate();
        if (dependingEntity != null) dependingEntity.remove();
    }

    public Entity getDependingEntity() {
        return dependingEntity;
    }

    @Override
    public void playAnimation(ActionType type) {
        this.playAnimation(type, () -> {
        }, 0);
    }

    public void playAnimation(ActionType type, Payload payload, int delay) {
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getTriggeredSounds() {
        return this.triggeredSounds;
    }

    public void addTriggeredSound(String triggeredSound) {
        this.triggeredSounds.add(triggeredSound);
    }

    public List<String> getSubtitles() {
        return this.subtitles;
    }

    public void addSubtitle(String text) {
        this.subtitles.add(text);
    }

    public void playSound(RealPlayer player) {
        final int stage = this.soundsStage.getOrDefault(player.getUniqueId(), 1);

        Bukkit.getScheduler().runTask(EntitiesPlugin.getInstance(), () -> {
            for (final String sound : this.triggeredSounds) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "stopsound " + player.getName() + " * " + sound);
            }
        });

        final String sound = this.triggeredSounds.get(stage - 1);
        Bukkit.getScheduler().runTask(EntitiesPlugin.getInstance(), () ->
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "playsound " + sound + " player " + player.getName() + " " + this.location.getBlockX() + " " + this.location.getBlockY() + " " + this.location.getBlockZ()));

        if(this.subtitles.size() >= stage) {
            this.saySubtitles(player, stage);
        }

        if(this.triggeredSounds.size() == stage) {
            this.soundsStage.remove(player.getUniqueId());
            return;
        }

        this.soundsStage.put(player.getUniqueId(), stage + 1);
    }

    private void saySubtitles(RealPlayer player, int stage) {
        final String subtitle = this.subtitles.get(stage - 1);
        for (final String s : subtitle.split("///")) {
            player.getBukkitPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', s));
        }
    }

    public void say(String message, int duration) {
        currentSpeechBalloonLayout = defaultSpeechBalloonLayout.clone().withMessage(message).withDuration(duration);
        say(currentSpeechBalloonLayout);
    }

    public void say(String[] lines, int duration) {
        currentSpeechBalloonLayout = defaultSpeechBalloonLayout.clone().withLines(lines).withDuration(duration);
        say(currentSpeechBalloonLayout);
    }

    public void sayJson(String[] jsonLines, int duration) {
        currentSpeechBalloonLayout = defaultSpeechBalloonLayout.clone().withJson(jsonLines).withDuration(duration);
        say(currentSpeechBalloonLayout);
    }

    public void say(SpeechBalloonLayout factory) {
        VirtualEntityTalkEvent event = new VirtualEntityTalkEvent(this, factory);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        if (!event.isCancelled()) {
            removeSpeechBalloons();
            isTalking = true;
            //this.speech = lines;
            speechCounter = factory.getDuration();
            currentSpeechBalloonLayout = factory;
            viewers.forEach(this::createSpeechBalloon);
        }
    }

    private void createSpeechBalloon(Player viewer) {
        try {
            SpeechBalloonEntity balloon = EntitiesPlugin.getEntityServer().spawnSpeechBalloon(this, viewer, currentSpeechBalloonLayout);
            speechBallons.put(viewer.getUniqueId(), balloon);
        } catch (InvalidLocationException e) {
            e.printStackTrace();
        }
    }

    private void removeSpeechBalloons() {
        speechBallons.forEach((uniqueId, balloon) -> balloon.terminate());
        speechBallons.clear();
    }

    public void stopTalking() {
        speechCounter = 1;
    }

    public boolean isTalking() {
        return isTalking;
    }

    @Override
    public Vector getMouth() {
        return mouth;
    }

    public Vector getSitPoint() {
        return sitPoint;
    }

    public Vector getSaddle() {
        return saddle;
    }

    public Set<UUID> getWhiteList() {
        return whiteList;
    }

    public boolean isUseWhitelistAsBlacklist() {
        return useWhitelistAsBlacklist;
    }

    @Override
    public void setInvisible(boolean visible) {
        //TODO
    }

    public boolean hasId(int entityId) {
        return this.getEntityId() == entityId;
    }

    public boolean isOnGround() {
        return movementType.equals(MovementType.SNEAKING)
                || movementType.equals(MovementType.UPRIGHT);
    }

    @Override
    public boolean isOnline() {
        return !isTerminated();
    }

    @Override
    public void addPotionEffect(PotionEffect effect) {
        //TODO
    }

    @Override
    public void removePotionEffect(PotionEffect effect) {
        //TODO
    }

    @Override
    public void addItem(ItemStack item, EquipmentSlot slot, int slotId) {
        //TODO
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        //TODO
    }

    @Override
    public void removeItem(ItemStack item) {
        //TODO
    }

    @Override
    public McmeInventory getInventory() {
        //Todo getInventory
        throw new UnsupportedOperationException();
    }

    public VirtualEntityFactory getFactory() {
        VirtualEntityFactory factory = new VirtualEntityFactory(type, location, useWhitelistAsBlacklist, uniqueId, name, attributes)
                .withBoundingBox(boundingBox)
                .withDisplayName(displayName)
                .withTriggeredSounds(triggeredSounds)
                .withMovementType(movementType)
                .withViewDistance(viewDistance)
                .withHealth(health)
                .withHeadYaw(headYaw)
                .withHeadPitch(headPitch)
                .withWhitelist(whiteList)
                .withMouth(mouth)
                .withKnockBackBase(knockBackBase)
                .withKnockBackPerDamage(knockBackPerDamage)
                .withJumpHeight(jumpHeight)
                .withAttributes(attributes)
                .withEnemies(enemies)
                .withSpeechBalloonLayout(defaultSpeechBalloonLayout)
                .withSubtitles(subtitles)
                .withSitPoint(sitPoint)
                .withSaddlePoint(saddle)
                .withAttackDelay(attackDelay);
        if (goal != null) {
            factory.withGoalFactory(goal.getFactory());
        }
        return factory;
    }

    public interface Payload {
        void execute();
    }
}
