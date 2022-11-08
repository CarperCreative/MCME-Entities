package com.mcmiddleearth.entities.json;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.movement.EntityBoundingBox;
import com.mcmiddleearth.entities.api.McmeEntityType;
import com.mcmiddleearth.entities.api.MovementType;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.attributes.VirtualAttributeFactory;
import com.mcmiddleearth.entities.entities.attributes.VirtualEntityAttributeInstance;
import com.mcmiddleearth.entities.entities.composite.bones.SpeechBalloonLayout;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class VirtualEntityFactoryAdapter extends TypeAdapter<VirtualEntityFactory> {

    private static final String
            TYPE = "type",
            BLACKLIST = "blacklist",
            WHITELIST = "whitelist",
            UNIQUE_ID = "unique_id",
            NAME = "name",
            DATA_FILE = "data_file",
            DISPLAY_NAME = "display_name",
            DISPLAY_NAME_POSITION = "display_name_position",
            SPAWN_LOCATION_ENTITY = "spawn_location_entity",
            SPAWN_LOCATION = "spawn_location",
            ROLL = "roll",
            HEAD_YAW = "head_yaw",
            HEAD_PITCH = "head_pitch",
            HEALTH = "health",
            MOVEMENT_TYPE = "movement_type",
            ATTRIBUTES = "attributes",
            BOUNDING_BOX = "bounding_box",
            GOAL_FACTORY = "goal_factory",
            HEAD_PITCH_CENTER = "head_pitch_center",
            SPEECH_BALLOON_LAYOUT = "speech_balloon_layout",
            MOUTH = "mouth",
            SOUND = "triggered_sound",
            SOUNDS = "triggered_sounds",
            SUBTITLES = "subtitles",
            MANUAL_ANIMATION = "manual_animation",
            HEAD_POSE_DELAY = "head_pose_delay",
            VIEW_DISTANCE = "view_distance",
            PROJECTILE_VELOCITY = "projectile_velocity",
            PROJECTILE_DAMAGE = "projectile_damage",
            MAX_ROTATION_STEP = "max_rotation_step",
            MAX_ROTATION_STEP_FLIGHT = "max_rotation_step_flight",
            UPDATE_INTERVAL = "update_interval",
            JUMP_HEIGHT = "jump_height",
            KNOCK_BACK_BASE = "knock_back_base",
            KNOCK_BACK_PER_DAMAGE = "knock_back_per_damage",
            ENEMIES = "enemies",
            SADDLE = "saddle",
            HORSE_COLOR = "horse_color",
            HORSE_STYLE = "horse_style",
            SADDLE_POINT = "saddle_point",
            ATTACK_POINT = "attack_point",
            SIT_POINT = "sit_point",
            ATTACK_DELAY = "attack_delay";

    @Override
    public void write(JsonWriter out, VirtualEntityFactory factory) throws IOException {
        boolean writeDefaults = factory.isWriteDefaultValuesToFile();
        VirtualEntityFactory defaults = VirtualEntityFactory.getDefaults();
        Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
        out.beginObject();
        out.name(TYPE).value(factory.getType().name());
        JsonUtil.writeNonDefaultBoolean(out, BLACKLIST, factory.hasBlackList(), defaults.hasBlackList(), writeDefaults);
        if (writeDefaults || factory.getWhitelist() != null) {
            out.name(WHITELIST).beginArray();
            if (factory.getWhitelist() != null) {
                for (UUID uuid : factory.getWhitelist()) out.value(uuid.toString());
            }
            out.endArray();
        }
        JsonUtil.writeNonDefaultUuid(out, UNIQUE_ID, factory.getUniqueId(), defaults.getUniqueId(), writeDefaults);
        JsonUtil.writeNonDefaultString(out, NAME, factory.getName(), defaults.getName(), writeDefaults);
        JsonUtil.writeNonDefaultString(out, DATA_FILE, factory.getDataFile(), defaults.getDataFile(), writeDefaults);
        JsonUtil.writeNonDefaultString(out, DISPLAY_NAME, factory.getDisplayName(), defaults.getDisplayName(), writeDefaults);
        JsonUtil.writeNonDefaultVector(out, DISPLAY_NAME_POSITION, factory.getDisplayNamePosition(), defaults.getDisplayNamePosition(), gson, writeDefaults);
        if (writeDefaults || factory.getSpawnLocationEntity() != null) {
            out.name(SPAWN_LOCATION_ENTITY);
            JsonUtil.writeEntityLink(factory.getSpawnLocationEntity(), true, out);
        } else {
            out.name(SPAWN_LOCATION);
            gson.toJson(factory.getLocation(), Location.class, out);
        }
        JsonUtil.writeNonDefaultFloat(out, ROLL, factory.getRoll(), defaults.getRoll(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, HEAD_YAW, factory.getHeadYaw(), defaults.getHeadYaw(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, HEAD_PITCH, factory.getHeadPitch(), defaults.getHeadPitch(), writeDefaults);
        JsonUtil.writeNonDefaultDouble(out, HEALTH, factory.getHealth(), defaults.getHealth(), writeDefaults);
        JsonUtil.writeNonDefaultString(out, MOVEMENT_TYPE, factory.getMovementType().name().toLowerCase(),
                defaults.getMovementType().name().toLowerCase(), writeDefaults);

        if (factory.getTriggeredSounds() != null) {
            out.name(SOUNDS).beginArray();
            for (final String triggeredSound : factory.getTriggeredSounds()) {
                out.value(triggeredSound);
            }
            out.endArray();
        }

        if (factory.getSubtitles() != null) {
            out.name(SUBTITLES).beginArray();
            for (final String subtitle : factory.getSubtitles()) {
                out.value(subtitle);
            }
            out.endArray();
        }

        if (writeDefaults || !factory.getAttributes().isEmpty()) {
            out.name(ATTRIBUTES).beginArray();
            for (AttributeInstance attributeInstance : factory.getAttributes().values()) {
                JsonUtil.writeNonDefaultAttribute(out, attributeInstance, factory.getType(), gson, writeDefaults);
            }
            out.endArray();
        }
        if (writeDefaults || !factory.getBoundingBox().equals(defaults.getBoundingBox())) {
            out.name(BOUNDING_BOX);
            gson.toJson(factory.getBoundingBox(), EntityBoundingBox.class, out);
        }
        if (writeDefaults || factory.getGoalFactory() != null) {
            out.name(GOAL_FACTORY);
            VirtualEntityGoalFactory goalFactory = factory.getGoalFactory();
            if (writeDefaults) goalFactory.withWriteDefaultsToFile(true);
            gson.toJson(goalFactory, VirtualEntityGoalFactory.class, out);
        }
        JsonUtil.writeNonDefaultVector(out, HEAD_PITCH_CENTER, factory.getHeadPitchCenter(),
                defaults.getHeadPitchCenter(), gson, writeDefaults);
        if (writeDefaults || !factory.getSpeechBalloonLayout().equals(defaults.getSpeechBalloonLayout())) {
            out.name(SPEECH_BALLOON_LAYOUT);
            gson.toJson(factory.getSpeechBalloonLayout(), SpeechBalloonLayout.class, out);
        }

        JsonUtil.writeNonDefaultVector(out, MOUTH, factory.getMouth(), defaults.getMouth(), gson, writeDefaults);
        JsonUtil.writeNonDefaultBoolean(out, MANUAL_ANIMATION, factory.getManualAnimationControl(),
                defaults.getManualAnimationControl(), writeDefaults);
        JsonUtil.writeNonDefaultInt(out, HEAD_POSE_DELAY, factory.getHeadPoseDelay(),
                defaults.getHeadPoseDelay(), writeDefaults);
        JsonUtil.writeNonDefaultInt(out, VIEW_DISTANCE, factory.getViewDistance(),
                defaults.getViewDistance(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, PROJECTILE_VELOCITY, factory.getProjectileVelocity(),
                defaults.getProjectileVelocity(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, PROJECTILE_DAMAGE, factory.getProjectileDamage(),
                defaults.getProjectileDamage(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, MAX_ROTATION_STEP, factory.getMaxRotationStep(),
                defaults.getMaxRotationStep(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, MAX_ROTATION_STEP_FLIGHT, factory.getMaxRotationStepFlight(),
                defaults.getMaxRotationStepFlight(), writeDefaults);
        JsonUtil.writeNonDefaultInt(out, UPDATE_INTERVAL, factory.getUpdateInterval(),
                defaults.getUpdateInterval(), writeDefaults);
        JsonUtil.writeNonDefaultInt(out, JUMP_HEIGHT, factory.getJumpHeight(),
                defaults.getJumpHeight(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, KNOCK_BACK_BASE, factory.getKnockBackBase(),
                defaults.getKnockBackBase(), writeDefaults);
        JsonUtil.writeNonDefaultFloat(out, KNOCK_BACK_PER_DAMAGE, factory.getKnockBackPerDamage(),
                defaults.getKnockBackPerDamage(), writeDefaults);
        if (writeDefaults || (factory.getEnemies() != null && !factory.getEnemies().isEmpty())) {
            out.name(ENEMIES).beginArray();
            for (McmeEntity enemy : factory.getEnemies()) {
                JsonUtil.writeEntityLink(enemy, false, out);
            }
            out.endArray();
        }
        JsonUtil.writeNonDefaultBoolean(out, SADDLE, factory.isSaddled(), defaults.isSaddled(), writeDefaults);
        JsonUtil.writeNonDefaultString(out, HORSE_COLOR, factory.getHorseColor().name().toLowerCase(),
                defaults.getHorseColor().name(), writeDefaults);
        JsonUtil.writeNonDefaultString(out, HORSE_STYLE, factory.getHorseStyle().name().toLowerCase(),
                defaults.getHorseStyle().name(), writeDefaults);
        JsonUtil.writeNonDefaultVector(out, SADDLE_POINT, factory.getSaddlePoint(), defaults.getSaddlePoint(), gson, writeDefaults);
        JsonUtil.writeNonDefaultVector(out, SIT_POINT, factory.getSitPoint(), defaults.getSitPoint(), gson, writeDefaults);
        JsonUtil.writeNonDefaultVector(out, ATTACK_POINT, factory.getAttackPoint(), defaults.getAttackPoint(), gson, writeDefaults);
        JsonUtil.writeNonDefaultInt(out, ATTACK_DELAY, factory.getAttackDelay(), defaults.getAttackDelay(), writeDefaults);

        out.endObject();
    }

    @Override
    public VirtualEntityFactory read(JsonReader in) throws IOException {
        VirtualEntityFactory factory = VirtualEntityFactory.getDefaults();
        Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
        in.beginObject();
        while (in.hasNext()) {
            String key = in.nextName();
//Logger.getGlobal().info("key: "+key);
            try {
                switch (key) {
                    case TYPE:
                        String type = in.nextString();
//Logger.getGlobal().info("type: "+type);
                        factory.withEntityType(McmeEntityType.valueOf(type.toUpperCase()));
//Logger.getGlobal().info("factory type: "+factory.getType());
                        break;
                    case BLACKLIST:
                        factory.withBlackList(in.nextBoolean());
                        break;
                    case WHITELIST:
                        Set<UUID> whitelist = new HashSet<>();
                        in.beginArray();
                        //try {
                        while (in.hasNext()) {
                            whitelist.add(UUID.fromString(in.nextString()));
                        }
                        //} finally {
                        in.endArray();//}
                        factory.withWhitelist(whitelist);
                        break;
                    case UNIQUE_ID:
                        factory.withUuid(UUID.fromString(in.nextString()));
                        break;
                    case NAME:
                        factory.withName(in.nextString());
                        break;
                    case DATA_FILE:
                        factory.withDataFile(in.nextString());
                        break;
                    case DISPLAY_NAME:
                        factory.withDisplayName(in.nextString());
                        break;
                    case DISPLAY_NAME_POSITION:
                        factory.withDisplayNamePosition(gson.fromJson(in, Vector.class));
                        break;
                    case SPAWN_LOCATION_ENTITY:
                        factory.withEntityForSpawnLocation(JsonUtil.readEntityLink(in));
                        break;
                    case SPAWN_LOCATION:
                        factory.withLocation(gson.fromJson(in, Location.class));
                        break;
                    case ROLL:
                        factory.withRoll((float) in.nextDouble());
                        break;
                    case HEAD_YAW:
                        factory.withHeadYaw((float) in.nextDouble());
                        break;
                    case HEAD_PITCH:
                        factory.withHeadPitch((float) in.nextDouble());
                        break;
                    case HEALTH:
                        factory.withHealth(in.nextInt());
                        break;
                    case SOUND:
                        factory.withTriggeredSound(in.nextString());
                        break;
                    case SOUNDS:
                        List<String> sounds = new ArrayList<>();
                        in.beginArray();
                        while (in.hasNext()) {
                            sounds.add(in.nextString());
                        }

                        in.endArray();
                        factory.withTriggeredSounds(sounds);
                        break;
                    case MOVEMENT_TYPE:
                        factory.withMovementType(MovementType.valueOf(in.nextString().toUpperCase()));
                        break;
                    case ATTRIBUTES:
                        Map<Attribute, AttributeInstance> attributes = new HashMap<>();
                        in.beginArray();
                        //try {
                        while (in.hasNext()) {
                            VirtualEntityAttributeInstance instance = gson.fromJson(in, VirtualEntityAttributeInstance.class);
                            attributes.put(instance.getAttribute(), instance);
                        }
                        //} finally {
                        in.endArray(); //}
                        factory.withAttributes(attributes);
                        break;
                    case BOUNDING_BOX:
                        factory.withBoundingBox(gson.fromJson(in, EntityBoundingBox.class));
                        break;
                    case GOAL_FACTORY:
                        factory.withGoalFactory(gson.fromJson(in, VirtualEntityGoalFactory.class));
                        break;
                    case HEAD_PITCH_CENTER:
                        factory.withHeadPitchCenter(gson.fromJson(in, Vector.class));
                        break;
                    case SUBTITLES:
                        List<String> subtitles = new ArrayList<>();
                        in.beginArray();
                        while (in.hasNext()) {
                            subtitles.add(in.nextString());
                        }

                        in.endArray();
                        factory.withSubtitles(subtitles);
                        break;
                    case SPEECH_BALLOON_LAYOUT:
                        factory.withSpeechBalloonLayout(gson.fromJson(in, SpeechBalloonLayout.class));
                        break;
                    case MOUTH:
                        factory.withMouth(gson.fromJson(in, Vector.class));
                        break;
                    case MANUAL_ANIMATION:
                        factory.withManualAnimationControl(in.nextBoolean());
                        break;
                    case HEAD_POSE_DELAY:
                        factory.withHeadPoseDelay(in.nextInt());
                        break;
                    case VIEW_DISTANCE:
                        factory.withViewDistance(in.nextInt());
                        break;
                    case PROJECTILE_VELOCITY:
                        factory.withProjectileVelocity((float) in.nextDouble());
                        break;
                    case PROJECTILE_DAMAGE:
                        factory.withProjectileDamage((float) in.nextDouble());
                        break;
                    case MAX_ROTATION_STEP:
                        factory.withMaxRotationStep((float) in.nextDouble());
                        break;
                    case MAX_ROTATION_STEP_FLIGHT:
                        factory.withMaxRotationStepFlight((float) in.nextDouble());
                        break;
                    case UPDATE_INTERVAL:
                        factory.withUpdateInterval(in.nextInt());
                        break;
                    case JUMP_HEIGHT:
                        factory.withJumpHeight(in.nextInt());
                        break;
                    case KNOCK_BACK_BASE:
                        factory.withKnockBackBase((float) in.nextDouble());
                        break;
                    case KNOCK_BACK_PER_DAMAGE:
                        factory.withKnockBackPerDamage((float) in.nextDouble());
                        break;
                    case ENEMIES:
                        Set<McmeEntity> enemies = new HashSet<>();
                        in.beginArray();
                        //try {
                        while (in.hasNext()) {
                            enemies.add(JsonUtil.readEntityLink(in));
                        }
                        //} finally {
                        in.endArray(); //}
                        factory.withEnemies(enemies);
                        break;
                    case SADDLE:
                        factory.withSaddled(in.nextBoolean());
                        break;
                    case HORSE_COLOR:
                        factory.withHorseColor(Horse.Color.valueOf(in.nextString().toUpperCase()));
                        break;
                    case HORSE_STYLE:
                        factory.withHorseStyle(Horse.Style.valueOf(in.nextString().toUpperCase()));
                        break;
                    case SADDLE_POINT:
                        factory.withSaddlePoint(gson.fromJson(in, Vector.class));
                        break;
                    case SIT_POINT:
                        factory.withSitPoint(gson.fromJson(in, Vector.class));
                        break;
                    case ATTACK_POINT:
                        factory.withAttackPoint(gson.fromJson(in, Vector.class));
                        break;
                    case ATTACK_DELAY:
                        factory.withAttackDelay(in.nextInt());
                        break;
                    default:
                        in.skipValue();
                }
            } catch (IllegalArgumentException | IllegalStateException | JsonSyntaxException ex) {
                //Logger.getLogger(VirtualEntityFactoryAdapter.class.getSimpleName()).warning("Error reading key: "+key+" -> "+ex.getMessage());
                ex.printStackTrace();
                throw new IllegalArgumentException("Error reading key: " + key + " at " + in.getPath() + " -> " + ex.getMessage());
            }
        }
        in.endObject();
        // restore default values which are not stored in file
        Map<Attribute, AttributeInstance> factoryAttributes = factory.getAttributes();
        VirtualAttributeFactory.getAttributesFor(factory.getType())
                .forEach((attribute, attributeInstance) -> {
//Logger.getGlobal().info("Attrib: "+attribute.name()+" attribs size: "+factory.getAttributes().size());
                    if (!factoryAttributes.containsKey(attribute)) {
//Logger.getGlobal().info("not found, restoring default: "+attributeInstance.getBaseValue());
                        factoryAttributes.put(attribute,
                                new VirtualEntityAttributeInstance(attribute, attributeInstance.getBaseValue(),
                                        attributeInstance.getDefaultValue()));
                    }
                });
        factory.withAttributes(factoryAttributes);
        return factory;
    }
}
