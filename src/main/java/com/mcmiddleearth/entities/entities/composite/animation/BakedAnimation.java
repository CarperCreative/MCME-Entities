package com.mcmiddleearth.entities.entities.composite.animation;

import com.mcmiddleearth.entities.entities.composite.BakedAnimationEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.SerializationUtils;

public class BakedAnimation implements Cloneable {

    private final List<Frame> frames;
    private final BakedAnimationType type;
    private final String name;
    /**
     * The non-unique name of the animation this instance represents.
     */
    private final String animationName;
    private final String next;
    private final int interval;
    private BakedAnimationEntity entity;

    private int currentFrame, ticks;
    private boolean finished;

    public BakedAnimation(BakedAnimationEntity entity, List<Frame> frames, BakedAnimationType type, String name, String animationName, String next,
                          int interval) {
        this.entity = entity;
        this.frames = frames;
        this.type = type;
        this.name = name;
        this.animationName = animationName;
        this.next = next;
        this.interval = interval;

        this.reset();
    }

    public BakedAnimation(List<Frame> frames, BakedAnimationType type, String name, String animationName, String next, int interval) {
        this(null, frames, type, name, animationName, next, interval);
    }

    public BakedAnimation(BakedAnimationType type, String name, String animationName, String next, int interval) {
        this(null, new ArrayList<>(), type, name, animationName, next, interval);
    }
    
    public void reset() {
        this.currentFrame = -1;
        this.ticks = -1;
        this.finished = false;
    }

    public void init() {
        this.frames.forEach(frame -> frame.initFrame(this.entity));
    }

    public void doTick() {
        if (this.finished) {
            return;
        }
        this.ticks++;

        if (this.ticks % this.interval == 0) {
            this.currentFrame++;
            if (this.currentFrame == this.frames.size()) {
                if (this.type.equals(BakedAnimationType.LOOP)) {
                    this.currentFrame = 0;
                } else {
                    this.finished = true;
                    return;
                }
            }

            this.frames.get(this.currentFrame).apply(this.entity.getState());
        }
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isAtLastFrame() {
        return this.currentFrame == this.frames.size() - 1;
    }

    public void addFrame(Frame frame) {
        this.frames.add(frame);
    }

    public void setEntity(BakedAnimationEntity entity) {
        this.entity = entity;
    }

    public BakedAnimationType getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getAnimationName() {
        return this.animationName;
    }

    public String getAnimationName() {
        return animationName;
    }

    public String getNext() {
        return this.next;
    }

    public List<Frame> getFrames() {
        return this.frames;
    }

    public int getInterval() {
        return this.interval;
    }

    public int getCurrentFrame() {
        return this.currentFrame;
    }

    public void applyFrame(int frameIndex) {
        final Frame frame = this.frames.get(frameIndex);

        if (frame != null) {
            frame.apply(this.entity.getState());
        }
    }    

    public BakedAnimation getReverse(String name, String animationName) {
        final BakedAnimation reverse = new BakedAnimation(this.type, name, animationName, this.next, this.interval);
        for (int i = this.frames.size() - 1; i >= 0; i--) {
            reverse.addFrame(this.frames.get(i));
        }
        return reverse;
    }

    @Override
    public BakedAnimation clone() {
        try {
            final BakedAnimation clone = (BakedAnimation) super.clone();
            final List<Frame> frameList = clone.getFrames().stream()
                .map(Frame::clone)
                .collect(Collectors.toList());

            return new BakedAnimation(frameList, clone.getType(), clone.getName(), clone.getAnimationName(), clone.getNext(), clone.getInterval());
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
