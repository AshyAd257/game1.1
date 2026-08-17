package com.Hecate.player;

/**
 * 动画状态枚举
 */
public enum AnimationState {
    IDLE("idle"),
    WALKING("walking"),
    RUNNING("running"),
    JUMPING("jumping"),
    FALLING("falling"),
    LANDING("landing"),
    ATTACKING("attacking"),
    BLOCKING("blocking"),
    DODGING("dodging"),
    CASTING("casting"),
    HURT("hurt"),
    DEAD("dead"),
    CLIMBING("climbing"),
    SWIMMING("swimming"),
    GLIDING("gliding");

    private final String animationName;

    AnimationState(String animationName) {
        this.animationName = animationName;
    }

    public String getAnimationName() {
        return animationName;
    }

    @Override
    public String toString() {
        return animationName;
    }
}
