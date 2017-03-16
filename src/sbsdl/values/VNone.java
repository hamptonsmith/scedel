package sbsdl.values;

public class VNone extends SkeletonValue {
    public static VNone INSTANCE = new VNone();
    
    private VNone() {}

    @Override
    public Value copy() {
        return this;
    }
}
