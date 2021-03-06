package com.shieldsbetter.scedel.values;

import com.shieldsbetter.scedel.InternalExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;

public abstract class SkeletonValue<C extends Value> implements Value<C> {
    @Override
    public final ParseLocation getParseLocation() {
        return ParseLocation.INTERNAL;
    }
    
    @Override
    public VNumber assertIsNumber(ParseLocation at) {
        throw InternalExecutionException.notANumber(at, this);
    }
    
    @Override
    public VBoolean assertIsBoolean(ParseLocation at) {
        throw InternalExecutionException.notABoolean(at, this);
    }

    @Override
    public VDict assertIsDict(ParseLocation at) {
        throw InternalExecutionException.notADictionary(at, this);
    }
    
    @Override
    public VSeq assertIsSeq(ParseLocation at) {
        throw InternalExecutionException.notASequence(at, this);
    }

    @Override
    public VFunction assertIsFunction(ParseLocation at) {
        throw InternalExecutionException.notAFunction(at, this);
    }

    @Override
    public VString assertIsString(ParseLocation at) {
        throw InternalExecutionException.notAString(at, this);
    }

    @Override
    public final Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
        return this;
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append(this);
    }
}
