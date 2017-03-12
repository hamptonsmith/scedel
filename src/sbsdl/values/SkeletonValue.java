package sbsdl.values;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;

public abstract class SkeletonValue implements Value {
    @Override
    public VNumber assertIsNumber() {
        throw new Sbsdl.ExecutionException("Value is not a number: " + this);
    }
    
    @Override
    public VBoolean assertIsBoolean() {
        throw new Sbsdl.ExecutionException("Value is not a boolean: " + this);
    }

    @Override
    public VDict assertIsDict() {
        throw new Sbsdl.ExecutionException(
                "Value is not a dictionary: " + this);
    }
    
    @Override
    public VSeq assertIsSeq() {
        throw new Sbsdl.ExecutionException("Value is not a sequence: " + this);
    }

    @Override
    public VFunction assertIsFunction() {
        throw new Sbsdl.ExecutionException("Value is not a function: " + this);
    }

    @Override
    public final Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        return this;
    }
}
