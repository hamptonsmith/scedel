package sbsdl.statements;

import java.util.LinkedList;
import java.util.List;

public class MultiplexingStatement {
    private final List<Statement> mySubStatements;
    
    public MultiplexingStatement(List<Statement> subStatements) {
        mySubStatements = new LinkedList<>(subStatements);
    }
}
