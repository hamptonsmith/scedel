package com.shieldsbetter.scedel.visitors;

import com.shieldsbetter.scedel.statements.DecideStatement;
import com.shieldsbetter.scedel.statements.EvaluateStatement;
import com.shieldsbetter.scedel.statements.FieldAssignmentStatement;
import com.shieldsbetter.scedel.statements.ForEachStatement;
import com.shieldsbetter.scedel.statements.IfStatement;
import com.shieldsbetter.scedel.statements.MultiplexingStatement;
import com.shieldsbetter.scedel.statements.ReturnStatement;
import com.shieldsbetter.scedel.statements.SequenceAssignmentStatement;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.statements.TopLevelVariableAssignmentStatement;
import com.shieldsbetter.scedel.statements.VariableIntroductionStatement;
import java.util.Iterator;

class StatementEquivalencyTester implements Statement.Visitor {
    
    public static boolean areEquivalent(Statement s1, Statement s2) {
        boolean result;
        try {
            checkEquivalence(s1, s2);
            result = true;
        }
        catch (NotEquivalentException nee) {
            result = false;
        }
        
        return result;
    }
    
    public static void checkEquivalence(Statement s1, Statement s2) {
        s2.accept(new StatementEquivalencyTester(s1));
    }
    
    private final Statement myStartStatement;
    
    private StatementEquivalencyTester(Statement s1) {
        myStartStatement = s1;
    }
    
    @Override
    public void visitDecideStatement(final DecideStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitDecideStatement(DecideStatement s2) {
                EquivalencyTester.checkEquivalence(
                        s1.getDecider(), s2.getDecider());
                checkEquivalence(s1.getCode(), s2.getCode());
            }
        });
    }

    @Override
    public void visitEvaluateStatement(final EvaluateStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitEvaluateStatement(EvaluateStatement s2) {
                EquivalencyTester.checkEquivalence(
                        s1.getExpression(), s2.getExpression());
            }
        });
    }

    @Override
    public void visitFieldAssignmentStatement(
            final FieldAssignmentStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitFieldAssignmentStatement(
                    FieldAssignmentStatement s2) {
                EquivalencyTester.checkEquivalence(s1.getBase(), s2.getBase());
                EquivalencyTester.checkEquivalence(
                        s1.getField(), s2.getField());
                EquivalencyTester.checkEquivalence(
                        s1.getAssigned(), s2.getAssigned());
            }
        });
    }

    @Override
    public void visitForEachStatement(final ForEachStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitForEachStatement(ForEachStatement s2) {
                if (!s1.getExemplar().isEquivalentTo(s2.getExemplar())) {
                    throw new NotEquivalentException();
                }
                
                EquivalencyTester.checkEquivalence(
                        s1.getCollection(), s2.getCollection());
                EquivalencyTester.checkEquivalence(
                        s1.getWhere(), s2.getWhere());
                checkEquivalence(s1.getCode(), s2.getCode());
            }
        });
    }

    @Override
    public void visitIfStatement(final IfStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitIfStatement(IfStatement s2) {
                EquivalencyTester.checkEquivalence(
                        s1.getCondition(), s2.getCondition());
                checkEquivalence(s1.getOnTrue(), s2.getOnTrue());
                checkEquivalence(s1.getOnFalse(), s2.getOnFalse());
            }
        });
    }

    @Override
    public void visitMultiplexingStatement(final MultiplexingStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitMultiplexingStatement(MultiplexingStatement s2) {
                Iterator<Statement> s1Stmts = s1.getSubStatements().iterator();
                Iterator<Statement> s2Stmts = s2.getSubStatements().iterator();
                while (s1Stmts.hasNext()) {
                    if (!s2Stmts.hasNext()) {
                        throw new NotEquivalentException();
                    }
                    
                    Statement s1Stmt = s1Stmts.next();
                    Statement s2Stmt = s2Stmts.next();
                    
                    checkEquivalence(s1Stmt, s2Stmt);
                }
                
                if (s2Stmts.hasNext()) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitReturnStatement(final ReturnStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitReturnStatement(ReturnStatement s2) {
                EquivalencyTester.checkEquivalence(
                        s1.getExpression(), s2.getExpression());
            }
        });
    }

    @Override
    public void visitSequenceAssignmentStatement(
            final SequenceAssignmentStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitSequenceAssignmentStatement(
                    SequenceAssignmentStatement s2) {
                EquivalencyTester.checkEquivalence(s1.getBase(), s2.getBase());
                EquivalencyTester.checkEquivalence(
                        s1.getIndex(), s2.getIndex());
                EquivalencyTester.checkEquivalence(
                        s1.getAssigned(), s2.getAssigned());
            }
        });
    }

    @Override
    public void visitNoOpStatement(final Statement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitNoOpStatement(Statement s2) { }
        });
    }

    @Override
    public void visitTopLevelVariableAssignmentStatement(
            final TopLevelVariableAssignmentStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitTopLevelVariableAssignmentStatement(
                    TopLevelVariableAssignmentStatement s2) {
                if (!s1.getSymbol().isEquivalentTo(s2.getSymbol())) {
                    throw new NotEquivalentException();
                }
                
                EquivalencyTester.checkEquivalence(
                        s1.getAssigned(), s2.getAssigned());
            }
        });
    }

    @Override
    public void visitVariableIntroductionStatement(
            final VariableIntroductionStatement s1) {
        myStartStatement.accept(new SubStatementEquivalencyTester() {
            @Override
            public void visitVariableIntroductionStatement(
                    VariableIntroductionStatement s2) {
                if (!s1.getSymbol().isEquivalentTo(s2.getSymbol())) {
                    throw new NotEquivalentException();
                }
                
                EquivalencyTester.checkEquivalence(
                        s1.getInitialValue(), s2.getInitialValue());
            }
        });
    }
    
    private static class SubStatementEquivalencyTester
            implements Statement.Visitor {
        @Override
        public void visitDecideStatement(DecideStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitEvaluateStatement(EvaluateStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitFieldAssignmentStatement(FieldAssignmentStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitForEachStatement(ForEachStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitIfStatement(IfStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitMultiplexingStatement(MultiplexingStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitReturnStatement(ReturnStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitSequenceAssignmentStatement(
                SequenceAssignmentStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitNoOpStatement(Statement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitTopLevelVariableAssignmentStatement(
                TopLevelVariableAssignmentStatement s) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVariableIntroductionStatement(
                VariableIntroductionStatement s) {
            throw new NotEquivalentException();
        }
    }
    
    public static class NotEquivalentException extends RuntimeException { }
}
