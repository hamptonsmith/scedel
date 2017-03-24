package com.shieldsbetter.scedel.statements;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;

public class MultiplexingStatement extends SkeletonStatement {
    private final List<Statement> mySubStatements;
    
    public MultiplexingStatement(
            ParseLocation l, List<Statement> subStatements) {
        super(l);
        mySubStatements = new LinkedList<>(subStatements);
    }

    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        Iterator<Statement> statementIter = mySubStatements.iterator();
        while (s.getReturn() == null && statementIter.hasNext()) {
            statementIter.next().execute(h, s);
        }
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT MUX\n");
        for (Statement s : mySubStatements) {
            s.prettyRender(indentUnit, indentLevels + 1, b);
        }
    }
}
