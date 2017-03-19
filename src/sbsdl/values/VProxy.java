package sbsdl.values;

import java.util.Collections;
import java.util.Map;
import sbsdl.Sbsdl;

public class VProxy extends VDict {
    public VProxy() {
        this(Collections.EMPTY_MAP);
    }
    
    public VProxy(Map<Value, Value> value) {
        super(true, value);
    }

    @Override
    public final boolean equals(Object o) {
        return o == this;
    }

    @Override
    public final int hashCode() {
        return referenceHashCode();
    }
    
    @Override
    public final VDict copy(boolean forbidProxies) {
        if (forbidProxies) {
            throw new Sbsdl.ExecutionException(
                    "Proxy objects cannot contain other proxy objects.");
        }
        
        return this;
    }
}
