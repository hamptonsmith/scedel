package sbsdl.values;

public abstract class ContainerValue<C extends Value> extends SkeletonValue<C> {
    private final boolean myForbidsProxiesFlag;
    
    public ContainerValue(boolean proxiesForbidden) {
        myForbidsProxiesFlag = proxiesForbidden;
    }
    
    public boolean forbidsProxies() {
        return myForbidsProxiesFlag;
    }
}
