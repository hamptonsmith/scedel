package com.shieldsbetter.scedel.expressions;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.values.Value;

public interface Expression {
    public ParseLocation getParseLocation();
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s);
    public boolean yeildsBakedLValues();
    
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b);
}
