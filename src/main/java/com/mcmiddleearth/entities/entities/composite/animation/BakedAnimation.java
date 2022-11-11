package com.mcmiddleearth.entities.entities.composite.animation;

import com.mcmiddleearth.entities.entities.composite.BakedAnimationEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BakedAnimation implements Cloneable {

    private final List<Frame> frames;

    private int currentFrame, ticks;

    private final BakedAnimationType type;

    private final String next;

    private final int interval;

    private boolean finished;

    private BakedAnimationEntity entity;

    private final String name;

    /**
     * The non-unique name of the animation this instance represents.
     */
    private final String animationName;

    public BakedAnimation(BakedAnimationEntity entity, List<Frame> frames, BakedAnimationType type, String name, String animationName, String next, int interval) {
        this.entity = entity;
        this.frames = frames;
        this.name = name;
        this.animationName = animationName;
        this.type = type;
        this.next = next;
        this.interval = interval;
        /*for (int i = 0; i < states.length; i++) {
            this.states.put(states[i], i);
        }*/
        reset();
    }

    public BakedAnimation(List<Frame> frames, BakedAnimationType type, String name, String animationName, String next, int interval) {
        this(null, frames, type, name, animationName, next, interval);
    }

    public BakedAnimation(BakedAnimationType type, String name, String animationName, String next, int interval) {
        this(null, new ArrayList<>(), type, name, animationName, next, interval);
    }

    public void reset() {
        currentFrame = -1;
        ticks = -1;
        finished = false;
    }

    public void init() {
        this.frames.forEach(frame -> frame.initFrame(this.entity));
    }

    public void doTick() {
        if(finished) {
            return;
        }
        ticks++;
        if(ticks%interval==0) {
            currentFrame++;
            if(currentFrame == frames.size()) {
                if(type.equals(BakedAnimationType.LOOP)) {
                    currentFrame = 0;
                } else {
                    finished = true;
                    return;
                }
            }
            frames.get(currentFrame).apply(entity.getState());
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isAtLastFrame() {
        return currentFrame == frames.size()-1;
    }

    public void addFrame(Frame frame) {
        frames.add(frame);
    }

    public void setEntity(BakedAnimationEntity entity) {
        this.entity = entity;
    }

    public String getName() {
        return name;
    }

    public String getAnimationName() {
        return animationName;
    }

    public String getNext() {
        return next;
    }

    public BakedAnimationType getType() {
        return type;
    }

    public int getInterval() {
        return interval;
    }

    public void applyFrame(int frameIndex) {
//Logger.getGlobal().info("1");
        Frame frame = frames.get(frameIndex);
//Logger.getGlobal().info("apply frame: "+frameIndex +" -> "+frame);
        if(frame!=null) {
            frame.apply(entity.getState());
        }
    }

    public BakedAnimation getReverse(String name, String animationName) {
        BakedAnimation reverse = new BakedAnimation(type, name, animationName, next, interval);
        for(int i = frames.size()-1; i >= 0; i--) {
            reverse.addFrame(frames.get(i));
        }
        return reverse;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public BakedAnimation clone() {
        try {
            final BakedAnimation clone = (BakedAnimation) super.clone();
            final List<Frame> frameList = clone.getFrames().stream()
                .map(Frame::clone)
                .collect(Collectors.toList());

            return new BakedAnimation(frameList, clone.getType(), clone.getName(), clone.getNext(), clone.getAnimationName(), clone.getInterval());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
