package sbsdl.values;

import sbsdl.ExecutionException;
import sbsdl.InternalExecutionException;

public class VProxy extends VDict {
    public VProxy() {
        super(true);
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
    public final VDict copy(ExecutionException onProxy) {
        if (onProxy != null) {
            throw new InternalExecutionException(onProxy);
        }
        
        return this;
    }
}
