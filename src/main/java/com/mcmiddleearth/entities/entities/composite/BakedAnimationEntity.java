package com.mcmiddleearth.entities.entities.composite;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.entities.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimation;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimationType;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityAnimationChangedEvent;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityStateChangedEvent;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import org.bukkit.Material;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BakedAnimationEntity extends CompositeEntity {

    private final Map<String, BakedAnimation> animations = new HashMap<>();

    private final Map<String, Integer> states = new HashMap<>();

    private BakedAnimation currentAnimation;

    private int currentState;

    private final File bakedAnimationFolder = new File(EntitiesPlugin.getInstance().getDataFolder(),"animation");


    public BakedAnimationEntity(int entityId, VirtualEntityFactory factory) throws InvalidLocationException {
        super(entityId, factory);
//Logger.getGlobal().info("Baked Animation Get location "+getLocation());
        File animationFile = new File(bakedAnimationFolder, factory.getDataFile());
        try (FileReader reader = new FileReader(animationFile)) {
//long start = System.currentTimeMillis();
            JsonObject data = new JsonParser().parse(reader).getAsJsonObject();
//Logger.getGlobal().info("File loading: "+(System.currentTimeMillis()-start));
            JsonObject modelData = data.get("model").getAsJsonObject();
            Material itemMaterial = Material.valueOf(modelData.get("head_item").getAsString().toUpperCase());
            JsonObject animationData = data.get("animations").getAsJsonObject();
//start = System.currentTimeMillis();
            animationData.entrySet().forEach(entry
                    -> animations.put(entry.getKey(), BakedAnimation.loadAnimation(entry.getValue().getAsJsonObject(),
                    itemMaterial, this)));
//Logger.getGlobal().info("Animation loading: "+(System.currentTimeMillis()-start));
        } catch (IOException e) {
            e.printStackTrace();
        }
        createPackets();
    }

    @Override
    public void doTick() {
        if(currentAnimation!=null) {
            if (currentAnimation.isFinished()) {
                if (currentAnimation.getType().equals(BakedAnimationType.CHAIN)) {
                    currentAnimation = animations.get(currentAnimation.getNext());
                    currentAnimation.reset();
                }
            }
            currentAnimation.doTick();
        }
        super.doTick();
    }

    public void setAnimation(String name) {
        BakedAnimationEntityAnimationChangedEvent event = new BakedAnimationEntityAnimationChangedEvent(this, name);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        if(!event.isCancelled()) {
            BakedAnimation newAnim = animations.get(event.getNextAnimationKey());
            if (newAnim != null) {
                currentAnimation = newAnim;
                currentAnimation.reset();
            } else {
                currentAnimation = null;
            }
        }
    }

    public void setState(String state) {
        BakedAnimationEntityStateChangedEvent event = new BakedAnimationEntityStateChangedEvent(this, state);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        if(!event.isCancelled()) {
            Integer stateId = states.get(event.getNextState());
            if (stateId != null) {
                currentState = stateId;
            }
        }
    }

    public int getState() {
        return currentState;
    }

    public Map<String, Integer> getStates() {
        return states;
    }

}
