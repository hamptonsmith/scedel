package com.shieldsbetter.scedel.values;

import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.expressions.Expression;

public interface Value<C extends Value> extends Expression {
    public VNumber assertIsNumber(ParseLocation at);
    public VBoolean assertIsBoolean(ParseLocation at);
    public VDict assertIsDict(ParseLocation at);
    public VSeq assertIsSeq(ParseLocation at);
    public VFunction assertIsFunction(ParseLocation at);
    public VString assertIsString(ParseLocation at);
    
    public C copy(ExecutionException onProxy);
}
