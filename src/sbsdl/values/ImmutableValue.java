package sbsdl.values;

public abstract class ImmutableValue<C extends Value> extends SkeletonValue {
    @Override
    public Value copy(boolean forbidProxies) {
        return this;
    }
}
