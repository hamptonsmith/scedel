package sbsdl.values;

import sbsdl.ExecutionException;

public abstract class ImmutableValue<C extends Value> extends SkeletonValue {
    @Override
    public Value copy(ExecutionException onProxy) {
        return this;
    }
}
