package sbsdl.statements;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public interface Statement {
    public static final Statement NO_OP = new Statement() {
                @Override
                public void execute(
                        Sbsdl.HostEnvironment h, ScriptEnvironment s) { }

                @Override
                public void prettyRender(
                        int indentUnit, int indentLevels, StringBuilder b) {
                    b.append("NO-OP");
                }

                @Override
                public ParseLocation getParseLocation() {
                    return ParseLocation.INTERNAL;
                }
            };
    
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s);
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b);
    public ParseLocation getParseLocation();
    
    public static class Util {
        public static void indent(
                int indentUnit, int indentLevels, StringBuilder b) {
            for (int i = 0; i < indentLevels; i++) {
                for (int j = 0; j < indentUnit; j++) {
                    b.append(' ');
                }
            }
        }
        
        public static void labeledChild(int indentUnit, int indentLevels,
                String label, Statement child, StringBuilder b) {
            indent(indentUnit, indentLevels + 1, b);
            b.append(label);
            b.append("\n");
            child.prettyRender(indentUnit, indentLevels + 2, b);
            b.append("\n");
        }
        
        public static void labeledChild(int indentUnit, int indentLevels,
                String label, Expression child, StringBuilder b) {
            indent(indentUnit, indentLevels + 1, b);
            b.append(label);
            b.append("\n");
            indent(indentUnit, indentLevels + 2, b);
            child.prettyRender(indentUnit, indentLevels + 2, b);
            b.append("\n");
        }
    }
}
