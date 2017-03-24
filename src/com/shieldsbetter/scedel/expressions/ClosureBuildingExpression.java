package com.shieldsbetter.scedel.expressions;

import java.util.LinkedList;
import java.util.List;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.MultiplexingStatement;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.VFunction;
import com.shieldsbetter.scedel.values.Value;

public class ClosureBuildingExpression extends SkeletonExpression {
    private final List<Scedel.Symbol> myArgumentNames;
    private final MultiplexingStatement myCode;
    
    public ClosureBuildingExpression(ParseLocation l,
            List<Scedel.Symbol> argNames, MultiplexingStatement code) {
        super(l);
        myArgumentNames = new LinkedList<>(argNames);
        myCode = code;
    }

    @Override
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
        return new VFunction(myArgumentNames, myCode, s);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP BUILD FUNCTION CLOSURE\n");
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("arg names: ");
        b.append(myArgumentNames);
        b.append("\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "code:", myCode, b);
    }
}
