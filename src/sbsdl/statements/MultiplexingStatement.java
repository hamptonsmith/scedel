package sbsdl.statements;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;

public class MultiplexingStatement implements Statement {
    private final List<Statement> mySubStatements;
    
    public MultiplexingStatement(List<Statement> subStatements) {
        mySubStatements = new LinkedList<>(subStatements);
    }

    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        Iterator<Statement> statementIter = mySubStatements.iterator();
        while (s.getReturn() == null && statementIter.hasNext()) {
            statementIter.next().execute(h, s);
        }
    }
}