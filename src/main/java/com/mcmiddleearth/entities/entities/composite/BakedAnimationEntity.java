package com.mcmiddleearth.entities.entities.composite;

import com.google.gson.*;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.api.ActionType;
import com.mcmiddleearth.entities.api.MovementSpeed;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.composite.animation.AnimationJob;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimation;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimationTree;
import com.mcmiddleearth.entities.entities.composite.animation.BakedAnimationType;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import com.mcmiddleearth.entities.entities.composite.collision.BoneAttachedCollider;
import com.mcmiddleearth.entities.entities.composite.collision.CollisionEntity;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityAnimationChangeEvent;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityAnimationSetEvent;
import com.mcmiddleearth.entities.events.events.virtual.composite.BakedAnimationEntityStateChangedEvent;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BakedAnimationEntity extends CompositeEntity implements CollisionEntity {

    private final BakedAnimationTree animationTree = new BakedAnimationTree(null);

    private final Map<String, Integer> states = new HashMap<>();

    private AnimationJob currentAnimation, nextAnimation;

    private int currentState;

    private boolean manualAnimationControl, manualOverride;

    private MovementSpeed movementSpeedAnimation;
    private int startMovementCounter, stopMovementCounter;

    private final String animationFileName;

    protected boolean instantAnimationSwitching = true;

    protected BoundingBox colliderBoundingBox = new BoundingBox();
    protected List<BoneAttachedCollider> colliders = new ArrayList<>();

    public BakedAnimationEntity(int entityId, VirtualEntityFactory factory) throws InvalidLocationException, InvalidDataException {
        this(entityId,factory,RotationMode.YAW);
    }

    public BakedAnimationEntity(int entityId, VirtualEntityFactory factory,
                                RotationMode rotationMode) throws InvalidLocationException, InvalidDataException {
        super(entityId, factory, rotationMode);
        //Logger.getGlobal().info("Baked Animation Get location "+getLocation());
        manualAnimationControl = factory.getManualAnimationControl();
        //Logger.getGlobal().info("Manual animation: "+manualAnimationControl);
        movementSpeedAnimation = getMovementSpeed();
        animationFileName = factory.getDataFile();
        File animationFile = new File(EntitiesPlugin.getAnimationFolder(), animationFileName+".json");
        try (FileReader reader = new FileReader(animationFile)) {
            //long start = System.currentTimeMillis();
            JsonObject data = new JsonParser().parse(reader).getAsJsonObject();
            //Logger.getGlobal().info("File loading: "+(System.currentTimeMillis()-start));
            JsonObject modelData = data.get("model").getAsJsonObject();
            Material itemMaterial = Material.valueOf(modelData.get("head_item").getAsString().toUpperCase());
            JsonObject animationData = data.get("animations").getAsJsonObject();
            //start = System.currentTimeMillis();
            animationData.entrySet().forEach(entry -> {
                String[] split;
                if(entry.getKey().contains(factory.getDataFile()+".")) {
                    split = entry.getKey().split(factory.getDataFile() + "\\.");
                } else {
                    split = entry.getKey().split("animations\\.");
                    //Logger.getGlobal().info("Length: "+split.length);
                }
                String animationKey;
                if(split.length>1) {
                    animationKey = split[1];
                } else {
                    //Logger.getGlobal().info("DataFile: "+factory.getDataFile());
                    animationKey = entry.getKey();
                }
                String animationName = animationKey;
                // Ignore integers if they're the last part of the path - those are used to distinguish different animations with the same key
                int lastDot = animationName.lastIndexOf('.');
                if (lastDot > 0) {
                    String lastKeyPart = animationName.substring(lastDot + 1);
                    if (lastKeyPart.matches("^\\d+$")) {
                        animationName = animationName.substring(0, lastDot);
                    }
                }
                //Logger.getGlobal().info("AnimationKey: "+animationKey);
                animationTree.addAnimation(animationName, BakedAnimation.loadAnimation(entry.getValue().getAsJsonObject(),
                        itemMaterial, this, animationKey, animationName));
            });

            loadColliders(data);
            //Logger.getGlobal().info("Animation loading: "+(System.currentTimeMillis()-start));
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new InvalidDataException("Data file '"+factory.getDataFile()+"' doesn't exist or does not contain valid animation data.");
        }
        //animationTree.debug();
        createPackets();
    }

    public static List<String> getDataFiles() {
        return Arrays.stream(Objects.requireNonNull(EntitiesPlugin.getAnimationFolder().listFiles(
                (dir, name) -> name.endsWith(".json"))))
                               .map(file -> file.getName().substring(0,file.getName().lastIndexOf('.')))
                               .collect(Collectors.toList());
    }

    private void loadColliders(JsonObject data) {
        JsonArray collidersData = data.getAsJsonArray("colliders");
        if (collidersData == null) return;

        for (JsonElement colliderDataElement : collidersData) {
            JsonObject colliderData = colliderDataElement.getAsJsonObject();

            String boneName = colliderData.get("bone").getAsString();
            Bone bone = getBones().stream().filter(boneCandidate -> boneName.equals(boneCandidate.getName())).findFirst().orElse(null);
            Vector size = readVector(colliderData.get("size").getAsJsonArray());
            Vector offset = readVector(colliderData.get("offset").getAsJsonArray());

            this.colliders.add(new BoneAttachedCollider(bone, offset, size));
        }
    }

    private static org.bukkit.util.Vector readVector(JsonArray data) {
        return new Vector(data.get(0).getAsDouble(), data.get(1).getAsDouble(), data.get(2).getAsDouble());
    }

    @Override
    public void doTick() {
        //Logger.getGlobal().info("Movementspeed: "+getMovementSpeed());
        //Logger.getGlobal().info("Movementtype: "+getMovementType());

        /*if(movementSpeedAnimation.equals(MovementSpeed.STAND) && !getMovementSpeed().equals(MovementSpeed.STAND)) { // When beginning movement
            movementSpeedAnimation = getMovementSpeed();

        } else if(getMovementSpeed().equals(MovementSpeed.STAND) && !movementSpeedAnimation.equals(MovementSpeed.STAND)) { // When stopping movement
            stopMovementCounter++;
            if(stopMovementCounter>3) { // What is the point of having this timer??
                movementSpeedAnimation = getMovementSpeed();
                stopMovementCounter = 0;
            }
        } else {*/
        //startMovementCounter = 0;
        //stopMovementCounter = 0;

        // movementSpeedAnimation and getMovementSpeed are essentially identical? What is the difference? Why can't we get rid of movementSpeedAnimation

        if(!getMovementSpeed().equals(movementSpeedAnimation)) {
            movementSpeedAnimation = getMovementSpeed();
        }
        //}

        // Automatic animation control
        if(!manualAnimationControl) {
            // Get the expected animation based on speed and action
            AnimationJob expected = new AnimationJob(animationTree.getAnimation(this),null,0); // TODO: Why is this being created every tick???

            // If we find the expected animation
            if(expected.getAnimation() != null
                && (
                        currentAnimation == null
                        || currentAnimation.getAnimation() == null
                        // If current animation is at the last frame, allow switching to another random animation
                        || currentAnimation.getAnimation().isAtLastFrame()
                        // And always switch if we're trying to switch to a completely different animation
                        || !expected.getAnimation().getAnimationName().equals(currentAnimation.getAnimation().getAnimationName())
                )) {
                if(!manualOverride && instantAnimationSwitching && callAnimationChangeEvent(currentAnimation,expected)) {

                    currentAnimation = expected;

                    if (currentAnimation.getAnimation() != null){
                        currentAnimation.getAnimation().reset();
                    }
                } else { //if(!manualOverride){
                    nextAnimation = expected;
                }
            }
        }

        if(currentAnimation!=null) {
            if (currentAnimation.getAnimation().isFinished() || currentAnimation.getAnimation().isAtLastFrame()) {
                if (currentAnimation.getAnimation().getType().equals(BakedAnimationType.CHAIN)) {
                    AnimationJob nextAnim = new AnimationJob(animationTree.getAnimation(currentAnimation.getAnimation().getNext()),
                                                     null,0);
                    if(callAnimationChangeEvent(currentAnimation,nextAnim)) {
                        currentAnimation = nextAnim;
                        //Logger.getGlobal().info("Animation switch due to Chain: "+(currentAnimation!=null?currentAnimation.getName():"nulll"));
                        currentAnimation.getAnimation().reset();
                    }
                } else {
                    manualOverride = false;
                }
            }
            if(!manualOverride
                    && (currentAnimation.getAnimation().isAtLastFrame()
                       || currentAnimation.getAnimation().isFinished())
                    && nextAnimation != null && callAnimationChangeEvent(currentAnimation,nextAnimation)) {
                currentAnimation = nextAnimation;
                //Logger.getGlobal().info("Animation switch regular: "+(currentAnimation!=null?currentAnimation.getName():"nulll"));
                currentAnimation.getAnimation().reset();
                nextAnimation = null;
                //Logger.getGlobal().info("Next Animation switch regular: null");
            }
            //Logger.getGlobal().info("Cur: "+currentAnimation.getName()+" OR: "+manualOverride+" MC: "+manualAnimationControl);
        } else {
            manualOverride = false;
            if(nextAnimation != null
                               && callAnimationChangeEvent(null,nextAnimation)) {
                currentAnimation = nextAnimation;
                //Logger.getGlobal().info("Animation switch cause of null: "+(currentAnimation!=null?currentAnimation.getName():"nulll"));
                currentAnimation.getAnimation().reset();
                nextAnimation = null;
                //Logger.getGlobal().info("Next Animation switch cause of null: null");
            }
        }
        if(currentAnimation!=null) {
            //Logger.getGlobal().info("Current anim: "+currentAnimation.getName()+" "+currentAnimation.getCurrentFrame()
            //                        +" next: "+(nextAnimation!=null?nextAnimation.getName():"nullnext"));
            currentAnimation.doTick();
        }
        super.doTick();
    }

    public void setAnimation(String name, boolean manualOverride, Payload payload, int delay) {
        if(manualAnimationControl && !manualOverride) {
            return;
        }
        BakedAnimationEntityAnimationSetEvent event = new BakedAnimationEntityAnimationSetEvent(this, name);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        if(!event.isCancelled()) {
            this.manualOverride = manualOverride;
            AnimationJob newAnim = new AnimationJob(animationTree.getAnimation(event.getNextAnimationKey()),payload,delay);
            //newAnim.setPayload(payload, delay);
            //Logger.getGlobal().info("New Anim: "+name+" -> "+newAnim);
            if(instantAnimationSwitching || manualOverride) {
                if(callAnimationChangeEvent(currentAnimation, newAnim)) {
                    if (newAnim.getAnimation() != null) {
                        currentAnimation = newAnim;
                        //Logger.getGlobal().info("Animation switch cause of manual: "+(currentAnimation!=null?currentAnimation.getAnimation().getName():"nulll"));
                        currentAnimation.getAnimation().reset();
                    } else {
                        currentAnimation = null;
                    }
                }
            } else {
                nextAnimation = newAnim;
            }
        }
    }

    @Override
    public void playAnimation(ActionType type, boolean manualOverride, Payload payload, int delay) {
        setAnimation(this.getMovementType().name().toLowerCase()
                        +"."+this.getMovementSpeed().name().toLowerCase()
                        +"."+type.name().toLowerCase(),
                    manualOverride, payload, delay);
    }

    private boolean callAnimationChangeEvent(AnimationJob current, AnimationJob next) {
        BakedAnimationEntityAnimationChangeEvent event
                = new BakedAnimationEntityAnimationChangeEvent(this, (current==null?null:current.getAnimation()),
                                                                (next==null?null:next.getAnimation()), manualAnimationControl,
                                                                instantAnimationSwitching);
        EntitiesPlugin.getEntityServer().handleEvent(event);
        return !event.isCancelled();
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

    public void setAnimationFrame(String animation, int frameIndex) {
        //Logger.getGlobal().info("set Animation Frame "+animation + " "+ frameIndex);
        manualOverride = true;
        currentAnimation = null;
        BakedAnimation anim = animationTree.getAnimation(animation);
        if (anim != null) {
            //Logger.getGlobal().info("Apply Frame: "+ anim);
            anim.applyFrame(frameIndex);
        }
    }

    public boolean isManualAnimationControl() {
        return manualAnimationControl;
    }

    public void setManualAnimationControl(boolean manualAnimationControl) {
        this.manualAnimationControl = manualAnimationControl;
    }

    public boolean isManualOverride() {
        return manualOverride;
    }

    public boolean isInstantAnimationSwitching() {
        return instantAnimationSwitching;
    }

    public BakedAnimation getCurrentAnimation() {
        return (currentAnimation==null?null:currentAnimation.getAnimation());
    }

    public List<String> getAnimations() {
        return animationTree.getAnimationKeys();
    }

    public MovementSpeed getMovementSpeedAnimation() {
        return movementSpeedAnimation;
    }

    @Override
    public VirtualEntityFactory getFactory() {
        VirtualEntityFactory factory = super.getFactory()
                .withDataFile(animationFileName)
                .withManualAnimationControl(manualAnimationControl);
        return factory;
    }

    @Override
    public Stream<BoundingBox> getColliders() {
        return colliders.stream().map(BoneAttachedCollider::getBoundingBox);
    }

    @Override
    public BoundingBox getCollisionBoundingBox() {
        return colliderBoundingBox;
    }

    @Override
    public void updateColliders() {
        colliderBoundingBox.resize(0, 0, 0, 0, 0, 0);

        for (BoneAttachedCollider collider : colliders) {
            collider.update();

            colliderBoundingBox.union(collider.getBoundingBox());
        }
    }
}
