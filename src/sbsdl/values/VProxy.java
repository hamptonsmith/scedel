package sbsdl.values;

import java.util.Collections;
import java.util.Map;
import sbsdl.Sbsdl;

public class VProxy extends VDict {
    public static String cannotContainProxyMessage(boolean forbidden) {
        String result;
        if (forbidden) {
            result = "Proxy objects cannot contain other proxy objects.";
        }
        else {
            result = null;
        }
        return result;
    }
    
    public VProxy() {
        this(Collections.EMPTY_MAP);
    }
    
    public VProxy(Map<Value, Value> value) {
        super(cannotContainProxyMessage(true), value);
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
    public final VDict copy(String proxiesForbiddedError) {
        if (proxiesForbiddedError != null) {
            throw new Sbsdl.ExecutionException(proxiesForbiddedError);
        }
        
        return this;
    }
}
