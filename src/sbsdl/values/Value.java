package sbsdl.values;

import sbsdl.expressions.Expression;

public interface Value<C extends Value> extends Expression {
    public VNumber assertIsNumber();
    public VBoolean assertIsBoolean();
    public VDict assertIsDict();
    public VSeq assertIsSeq();
    public VFunction assertIsFunction();
    public VString assertIsString();
    
    public C copy(boolean forbidProxies);
}
