package sbsdl.values;

import sbsdl.ExecutionException;
import sbsdl.ParseLocation;
import sbsdl.expressions.Expression;

public interface Value<C extends Value> extends Expression {
    public VNumber assertIsNumber(ParseLocation at);
    public VBoolean assertIsBoolean(ParseLocation at);
    public VDict assertIsDict(ParseLocation at);
    public VSeq assertIsSeq(ParseLocation at);
    public VFunction assertIsFunction(ParseLocation at);
    public VString assertIsString(ParseLocation at);
    
    public C copy(ExecutionException onProxy);
}
