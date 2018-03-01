package com.shieldsbetter.scedel.visitors;

import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.expressions.BinaryExpression;
import com.shieldsbetter.scedel.expressions.ClosureBuildingExpression;
import com.shieldsbetter.scedel.expressions.DictionaryExpression;
import com.shieldsbetter.scedel.expressions.Expression;
import com.shieldsbetter.scedel.expressions.FunctionCallExpression;
import com.shieldsbetter.scedel.expressions.HostExpression;
import com.shieldsbetter.scedel.expressions.LiteralExpression;
import com.shieldsbetter.scedel.expressions.PickExpression;
import com.shieldsbetter.scedel.expressions.SequenceExpression;
import com.shieldsbetter.scedel.expressions.UnaryExpression;
import com.shieldsbetter.scedel.expressions.VariableNameExpression;
import com.shieldsbetter.scedel.statements.DecideStatement;
import com.shieldsbetter.scedel.statements.EvaluateStatement;
import com.shieldsbetter.scedel.statements.FieldAssignmentStatement;
import com.shieldsbetter.scedel.statements.ForEachStatement;
import com.shieldsbetter.scedel.statements.IfStatement;
import com.shieldsbetter.scedel.statements.MultiplexingStatement;
import com.shieldsbetter.scedel.statements.ReturnStatement;
import com.shieldsbetter.scedel.statements.SequenceAssignmentStatement;
import com.shieldsbetter.scedel.statements.TopLevelVariableAssignmentStatement;
import com.shieldsbetter.scedel.statements.VariableIntroductionStatement;
import com.shieldsbetter.scedel.values.VBoolean;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.VFunction;
import com.shieldsbetter.scedel.values.VNone;
import com.shieldsbetter.scedel.values.VNumber;
import com.shieldsbetter.scedel.values.VProxy;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.VString;
import com.shieldsbetter.scedel.values.VToken;
import com.shieldsbetter.scedel.values.VUnavailable;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * <p>A visitor to test the structural equivalency of
 * {@link com.shieldsbetter.scedel.expressions.Expression}s.</p>
 */
public class EquivalencyTester implements Expression.Visitor {

    public static boolean areEquivalent(Expression e1, Expression e2) {
        boolean result;
        try {
            checkEquivalence(e1, e2);
            result = true;
        }
        catch (NotEquivalentException nee) {
            result = false;
        }
        
        return result;
    }
    
    public static void checkEquivalence(Expression e1, Expression e2) {
        e2.accept(new EquivalencyTester(e1));
    }
    
    private final Expression myStartExpression;
    
    private EquivalencyTester(Expression e1) {
        myStartExpression = e1;
    }
    
    @Override
    public void visitBinaryExpression(final BinaryExpression e1) {
        myStartExpression.accept(
                new SubExpressionEquivalenceTester() {
                    @Override
                    public void visitBinaryExpression(BinaryExpression e2) {
                        if (e1.getOperator() != e2.getOperator()) {
                            throw new NotEquivalentException();
                        }

                        checkEquivalence(
                                e1.getOperand1(), e2.getOperand1());
                        checkEquivalence(
                                e1.getOperand2(), e2.getOperand2());
                    }
                });
    }

    @Override
    public void visitClosureBindingExpression(
            final ClosureBuildingExpression e1) {
        myStartExpression.accept(
                new SubExpressionEquivalenceTester() {
                    @Override
                    public void visitClosureBindingExpression(
                            ClosureBuildingExpression e2) {
                        Iterator<Scedel.Symbol> e1Syms =
                                e1.getArgumentNames().iterator();
                        Iterator<Scedel.Symbol> e2Syms = 
                                e2.getArgumentNames().iterator();
                        while (e1Syms.hasNext()) {
                            if (!e2Syms.hasNext()) {
                                throw new NotEquivalentException();
                            }

                            Scedel.Symbol e1Sym = e1Syms.next();
                            Scedel.Symbol e2Sym = e2Syms.next();

                            if (!e1Sym.isEquivalentTo(e2Sym)) {
                                throw new NotEquivalentException();
                            }
                        }

                        if (e2Syms.hasNext()) {
                            throw new NotEquivalentException();
                        }

                        StatementEquivalencyTester.areEquivalent(
                                e1.getCode(), e2.getCode());
                    }
                });
    }

    @Override
    public void visitDictionaryExpression(final DictionaryExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitDictionaryExpression(DictionaryExpression e2) {
                Iterator<DictionaryExpression.Mapping> e1Fields =
                        e1.entries().iterator();
                Iterator<DictionaryExpression.Mapping> e2Fields =
                        e2.entries().iterator();
                while (e1Fields.hasNext()) {
                    if (!e2Fields.hasNext()) {
                        throw new NotEquivalentException();
                    }
                    
                    DictionaryExpression.Mapping e1Mapping = e1Fields.next();
                    DictionaryExpression.Mapping e2Mapping = e2Fields.next();
                    
                    checkEquivalence(e1Mapping.getKey(), e2Mapping.getKey());
                    checkEquivalence(
                            e1Mapping.getValue(), e2Mapping.getValue());
                }
                
                if (e2Fields.hasNext()) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitFunctionCallExpression(final FunctionCallExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitFunctionCallExpression(FunctionCallExpression e2) {
                checkEquivalence(e1.getFunction(), e2.getFunction());
                
                Iterator<Expression> e1Params = e1.getParameters().iterator();
                Iterator<Expression> e2Params = e2.getParameters().iterator();
                while (e1Params.hasNext()) {
                    if (!e2Params.hasNext()) {
                        throw new NotEquivalentException();
                    }
                    
                    Expression e1Param = e1Params.next();
                    Expression e2Param = e2Params.next();
                    
                    checkEquivalence(e1Param, e2Param);
                }
                
                if (e2Params.hasNext()) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitHostExpression(HostExpression e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitLiteralExpression(LiteralExpression e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitPickExpression(PickExpression e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitSequenceExpression(SequenceExpression e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitUnaryExpression(UnaryExpression e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVariableNameExpression(VariableNameExpression e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVBoolean(VBoolean v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVDict(VDict v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVFunction(VFunction v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVNone(VNone v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVNumber(VNumber v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVProxy(VProxy v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVSeq(VSeq v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVString(VString v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVToken(VToken v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void visitVUnavailable(VUnavailable v) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private static class SubExpressionEquivalenceTester
            implements Expression.Visitor {

        @Override
        public void visitBinaryExpression(BinaryExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitClosureBindingExpression(ClosureBuildingExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitDictionaryExpression(DictionaryExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitFunctionCallExpression(FunctionCallExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitHostExpression(HostExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitLiteralExpression(LiteralExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitPickExpression(PickExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitSequenceExpression(SequenceExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitUnaryExpression(UnaryExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVariableNameExpression(VariableNameExpression e) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVBoolean(VBoolean v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVDict(VDict v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVFunction(VFunction v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVNone(VNone v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVNumber(VNumber v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVProxy(VProxy v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVSeq(VSeq v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVString(VString v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVToken(VToken v) {
            throw new NotEquivalentException();
        }

        @Override
        public void visitVUnavailable(VUnavailable v) {
            throw new NotEquivalentException();
        }
    }
    
    private static class NotEquivalentException extends RuntimeException {
        
    }
}
