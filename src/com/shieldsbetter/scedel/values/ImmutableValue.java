package com.shieldsbetter.scedel.values;

public abstract class ImmutableValue<C extends Value> extends SkeletonValue {
    @Override
    public Value copy(boolean errorOnVProxy) {
        return this;
    }
}
