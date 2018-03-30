package com.shieldsbetter.scedel.visitors;

import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.Utils;
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
import com.shieldsbetter.scedel.statements.Statement;
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
import com.shieldsbetter.scedel.values.Value;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

                        checkStatementEquivalece(e1.getCode(), e2.getCode());
                    }
                });
    }

    private static void checkStatementEquivalece(
            Statement s1, Statement s2) {
        try {
            StatementEquivalencyTester.checkEquivalence(s1, s2);
        }
        catch (StatementEquivalencyTester.NotEquivalentException nee) {
            throw new NotEquivalentException(nee);
        }
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
    public void visitHostExpression(final HostExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitHostExpression(HostExpression e2) {
                if (!e1.getId().equals(e2.getId())) {
                    throw new NotEquivalentException();
                }
                
                Utils.elementsConformant(
                        e1.getParameters(), e2.getParameters(),
                        new Utils.BinaryPredicate<Expression>() {
                            @Override
                            public boolean satisfy(
                                    Expression t1, Expression t2) {
                                checkEquivalence(t1, t2);
                                return true;
                            }
                        });
            }
        });
    }

    @Override
    public void visitLiteralExpression(final LiteralExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitLiteralExpression(LiteralExpression e2) {
                checkEquivalence(e1.getValue(), e2.getValue());
            }
        });
    }

    @Override
    public void visitPickExpression(final PickExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitPickExpression(PickExpression e2) {
                if (!e1.getExamplar().isEquivalentTo(e2.getExamplar())) {
                    throw new NotEquivalentException();
                }
                
                checkEquivalence(e1.getCollection(), e2.getCollection());
                checkEquivalence(e1.getCount(), e2.getCount());
                checkEquivalence(e1.getUnique(), e2.getUnique());
                checkEquivalence(
                        e1.getWeighExpression(), e2.getWeighExpression());
                checkEquivalence(e1.getWhere(), e2.getWhere());
            }
        });
    }

    @Override
    public void visitSequenceExpression(final SequenceExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitSequenceExpression(SequenceExpression e2) {
                if (!Utils.elementsConformant(
                        e1.getElements(), e2.getElements(),
                        new Utils.BinaryPredicate<Expression>() {
                            @Override
                            public boolean satisfy(
                                    Expression t1, Expression t2) {
                                return areEquivalent(t1, t2);
                            }
                        })) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitUnaryExpression(final UnaryExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitUnaryExpression(UnaryExpression e2) {
                if (!e1.getOperator().equals(e2.getOperator())) {
                    throw new NotEquivalentException();
                }
                
                checkEquivalence(e1.getOperand(), e2.getOperand());
            }
        });
    }

    @Override
    public void visitVariableNameExpression(final VariableNameExpression e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVariableNameExpression(VariableNameExpression e2) {
                if (!e1.getSymbol().isEquivalentTo(e2.getSymbol())) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitVBoolean(final VBoolean e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVBoolean(VBoolean e2) {
                if (e1.getValue() != e2.getValue()) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitVDict(final VDict e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVDict(VDict e2) {
                Map<Value, Value> e1Copy = dictCopy(e1);
                Map<Value, Value> e2Copy = dictCopy(e2);
                
                for (Map.Entry<Value, Value> e1Entry : e1Copy.entrySet()) {
                    Value e2Value = removeEquivalent(e2Copy, e1Entry.getKey());
                    
                    if (e2Value == null
                            || !areEquivalent(e1Entry.getValue(), e2Value)) {
                        throw new NotEquivalentException();
                    }
                }
                
                if (!e2Copy.isEmpty()) {
                    throw new NotEquivalentException();
                }
            }
        });
    }
    
    private static Map<Value, Value> dictCopy(VDict d) {
        Map<Value, Value> result = new HashMap<>();
        for (Map.Entry<Value, Value> e : d.entries()) {
            result.put(e.getKey(), e.getValue());
        }
        return result;
    }
    
    private static Value removeEquivalent(Map<Value, Value> m, Value v) {
        Iterator<Map.Entry<Value, Value>> iIter = m.entrySet().iterator();
        Value result = null;
        while (result == null && iIter.hasNext()) {
            Map.Entry<Value, Value> entry = iIter.next();
            if (areEquivalent(entry.getKey(), v)) {
                iIter.remove();
                result = entry.getValue();
            }
        }
        return result;
    }

    @Override
    public void visitVFunction(final VFunction e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVFunction(VFunction e2) {
                if (e1.getArgumentCount() != e2.getArgumentCount()) {
                    throw new NotEquivalentException();
                }
                
                Iterator<Scedel.Symbol> e1Args =
                        e1.getArgumentNames().iterator();
                Iterator<Scedel.Symbol> e2Args =
                        e2.getArgumentNames().iterator();
                while (e1Args.hasNext()) {
                    if (!e1Args.next().isEquivalentTo(e2Args.next())) {
                        throw new NotEquivalentException();
                    }
                }
                
                checkStatementEquivalece(e1.getCode(), e2.getCode());
            }
        });
    }

    @Override
    public void visitVNone(VNone v) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVNone(VNone e2) { }
        });
    }

    @Override
    public void visitVNumber(final VNumber e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVNumber(VNumber e2) {
                if (!e1.equals(e2)) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitVProxy(final VProxy e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVProxy(VProxy e2) {
                if (!e1.equals(e2)) {
                    throw new NotEquivalentException();
                }
            }
        });
    }

    @Override
    public void visitVSeq(final VSeq e1) {
        myStartExpression.accept(new SubExpressionEquivalenceTester() {
            @Override
            public void visitVSeq(VSeq e2) {
                if (e1.getElementCount() != e2.getElementCount()) {
                    throw new NotEquivalentException();
                }
                
                Iterator<Value> e1Iter = e1.iterator();
                Iterator<Value> e2Iter = e2.iterator();
                while (e1Iter.hasNext()) {
                    if (areEquivalent(e1Iter.next(), e2Iter.next())) {
                        throw new NotEquivalentException();
                    }
                }
            }
        });
    }

    @Override
    public void visitVString(VString v) {
        if (!v.equals(myStartExpression)) {
            throw new NotEquivalentException();
        }
    }

    @Override
    public void visitVToken(VToken v) {
        if (!v.equals(myStartExpression)) {
            throw new NotEquivalentException();
        }
    }

    @Override
    public void visitVUnavailable(VUnavailable v) {
        if (myStartExpression != VUnavailable.INSTANCE) {
            throw new NotEquivalentException();
        }
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
        public NotEquivalentException() {}
        public NotEquivalentException(Exception cause) {
            super(cause);
        }
    }
}
