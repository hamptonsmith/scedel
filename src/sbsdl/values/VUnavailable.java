package sbsdl.values;

public class VUnavailable extends SkeletonValue {
    public static VUnavailable INSTANCE = new VUnavailable();
    
    private VUnavailable() { }

    @Override
    public Value copy() {
        return this;
    }
}
