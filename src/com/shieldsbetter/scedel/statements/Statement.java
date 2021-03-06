package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;

public interface Statement {
    public static final Statement NO_OP = new Statement() {
                @Override
                public void execute(
                        Scedel.HostEnvironment h, ScriptEnvironment s) { }

                @Override
                public void prettyRender(
                        int indentUnit, int indentLevels, StringBuilder b) {
                    b.append("NO-OP");
                }

                @Override
                public ParseLocation getParseLocation() {
                    return ParseLocation.INTERNAL;
                }

                @Override
                public void accept(Visitor v) {
                    v.visitNoOpStatement(this);
                }
            };
    
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s);
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b);
    public ParseLocation getParseLocation();
    public void accept(Visitor v);
    
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
            indent(indentUnit, indentLevels + 2, b);
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
    
    public static interface Visitor {
        public void visitDecideStatement(DecideStatement s);
        public void visitEvaluateStatement(EvaluateStatement s);
        public void visitFieldAssignmentStatement(FieldAssignmentStatement s);
        public void visitForEachStatement(ForEachStatement s);
        public void visitIfStatement(IfStatement s);
        public void visitMultiplexingStatement(MultiplexingStatement s);
        public void visitReturnStatement(ReturnStatement s);
        public void visitSequenceAssignmentStatement(
                SequenceAssignmentStatement s);
        public void visitNoOpStatement(Statement s);
        public void visitTopLevelVariableAssignmentStatement(
                TopLevelVariableAssignmentStatement s);
        public void visitVariableIntroductionStatement(
                VariableIntroductionStatement s);
    }
}
