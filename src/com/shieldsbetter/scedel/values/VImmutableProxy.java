package com.shieldsbetter.scedel.values;

import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.InternalExecutionException;

public class VImmutableProxy extends VProxy {
    @Override
    public VDict put(Value key, Value val) {
        throw new InternalExecutionException(new ExecutionException(
                ExecutionException.ErrorType.OTHER, "Cannot modify this proxy.",
                key.getParseLocation(), null));
    }
}
