package com.shieldsbetter.scedel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import com.shieldsbetter.scedel.values.VBoolean;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.VNone;
import com.shieldsbetter.scedel.values.VNumber;
import com.shieldsbetter.scedel.values.VProxy;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.VString;
import com.shieldsbetter.scedel.values.VToken;
import com.shieldsbetter.scedel.values.VUnavailable;
import com.shieldsbetter.scedel.values.Value;

public class SbsdlTest {
    @Test
    public void unavailableLiteral() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("unavailable", VUnavailable.INSTANCE);
    }
    
    @Test
    public void noneLiteral() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("none", VNone.INSTANCE);
    }
    
    @Test
    public void trueLiteral() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("true", VBoolean.TRUE);
    }
    
    @Test
    public void falseLiteral() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("false", VBoolean.FALSE);
    }
    
    @Test
    public void andTT() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("true and true", VBoolean.TRUE);
    }
    
    @Test
    public void andTF() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("true and false", VBoolean.FALSE);
    }
    
    @Test
    public void andFT() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("false and true", VBoolean.FALSE);
    }
    
    @Test
    public void andFF() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("false and false", VBoolean.FALSE);
    }
    
    @Test
    public void andShortCircuit() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("false and 5", VBoolean.FALSE);
    }
    
    @Test
    public void orTT() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("true or true", VBoolean.TRUE);
    }
    
    @Test
    public void orTF() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("true or false", VBoolean.TRUE);
    }
    
    @Test
    public void orFT() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("false or true", VBoolean.TRUE);
    }
    
    @Test
    public void orFF() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("false or false", VBoolean.FALSE);
    }
    
    @Test
    public void orShortCircuit() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("true or 5", VBoolean.TRUE);
    }
    
    @Test
    public void notT() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("not true", VBoolean.FALSE);
    }
    
    @Test
    public void notF() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("not false", VBoolean.TRUE);
    }
    
    @Test
    public void otherwiseNotUnavailable() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("none otherwise 6", VNone.INSTANCE);
    }
    
    @Test
    public void otherwiseUnavailable() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("unavailable otherwise 6", VNumber.of(6, 1));
    }
    
    @Test
    public void fractionalNumber() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("123.457", VNumber.of(123457, 1000));
    }
    
    @Test
    public void wholeNumber() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("123", VNumber.of(123, 1));
    }
    
    @Test
    public void stringLiteral() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("'hello \\'quoted'", new VString("hello 'quoted"));
    }
    
    @Test
    public void emptyStringLiteral() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("''", new VString(""));
    }
    
    @Test
    public void sequenceLiteral() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("[3.1, 'foo', []]",
                new VSeq(VNumber.of(31, 10), new VString("foo"), new VSeq()));
    }
    
    @Test
    public void emptySequence() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("[]", new VSeq());
    }
    
    @Test
    public void singletonSequence() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("[1]", new VSeq(VNumber.of(1, 1)));
    }
    
    @Test
    public void functionLiteral() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("fn(x) { return x; }(123)", VNumber.of(123, 1));
    }
    
    @Test
    public void uniquePickExplicitWeight() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique from { 'foo' {1}, 'bar' {5}, 'bazz' {5} }", 
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void uniquePickImplicitWeight() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void expressionParameterizedUniqueTrue() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique(true) from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void expressionParameterizedUniqueFalse() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique(false) from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("foo")),
                true, false, false, true, false, false);
    }
    
    @Test
    public void pickExplicitWeight() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 2 from { 'foo' {1}, 'bar' {5}, 'bazz' {5} }", 
                new VSeq(new VString("foo"), new VString("foo")),
                true, false, false, true, false, false);
    }
    
    @Test
    public void pickImplicitWeight() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 2 from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("foo")),
                true, false, false, true, false, false);
    }
    
    @Test
    public void pickSingleYeildsElementInsteadOfSequence()
            throws ExecutionException, StaticCodeException,
                Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 1 from { 'foo', 'bar', 'bazz' }", 
                new VString("foo"),
                true, false, false);
    }
    
    @Test
    public void pickImplicitCount() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick from { 'foo', 'bar', 'bazz' }", 
                new VString("foo"),
                true, false, false);
    }
    
    @Test
    public void pickFromSequence() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique from [ 'foo', 'bar', 'bazz' ]",
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void pickingZeroYeildsEmptySequence() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest(
                "pick 0 from [ 'foo', 'bar', 'bazz' ]", new VSeq());
    }
    
    @Test
    public void pickingOneFromNoGoodOptionsYieldsUnavailable()
            throws ExecutionException, StaticCodeException,
                Scedel.HostEnvironmentException {
        evaluationTest("pick 1 from {'foo' {0}, 'bar' {0}, 'bazz' {0}}",
                VUnavailable.INSTANCE);
    }
    
    @Test
    public void pickingMultilpleFromNoGoodOptionsYieldsUnavailable()
            throws ExecutionException, StaticCodeException,
                Scedel.HostEnvironmentException {
        evaluationTest("pick 3 from {'foo' {0}, 'bar' {0}, 'bazz' {0}}",
                VUnavailable.INSTANCE);
    }
    
    @Test
    public void pickWithOtherwise() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("pick from [] otherwise pick from {true}",
                VBoolean.TRUE, true);
    }
    
    @Test
    public void dictionaryLiteral() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("{foo: 5, bar: 2 + 4}",
                    new VDict().put(new VString("foo"), VNumber.of(5, 1))
                            .put(new VString("bar"), VNumber.of(6, 1)));
    }
    
    @Test
    public void emptyDictionary() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("{}", new VDict());
    }
    
    @Test
    public void singletonDictionary() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("{foo: 5}",
                    new VDict().put(new VString("foo"), VNumber.of(5, 1)));
    }
    
    @Test
    public void sequenceIndexing() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("[3, 4, 5][1]", VNumber.of(4, 1));
    }
    
    @Test
    public void dictionaryAccess() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("{foo: 5, bar: 6, bazz: 9}.bar", VNumber.of(6, 1));
    }
    
    @Test
    public void dictionaryAccessExpression() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("{foo: 5, bar: 6, bazz: 9}.('bar')", VNumber.of(6, 1));
    }
    
    @Test
    public void orderOfOperations() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("1 + 3 * 5 * 2 ^ 4 * (6 + 7)", VNumber.of(3121, 1));
    }
    
    @Test
    public void stringConcatenation() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("'foo' + 'bar'", new VString("foobar"));
    }
    
    @Test
    public void stringConcatenationWithNumbers() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("'foo' + 4.1", new VString("foo(41 / 10)"));
    }
    
    @Test
    public void nestedStructures() throws ExecutionException,
            StaticCodeException, Scedel.HostEnvironmentException {
        evaluationTest("{"
                        + "foo: ["
                            + "[1, 2, {}],"
                            + "{"
                                + "tom: [],"
                                + "dick: ['a'],"
                                +" harry: [4, 5, 6]"
                            + "}],"
                        + "bar: [[[[[]]]]]"
                    + "}",
                    new VDict()
                        .put(new VString("foo"),
                                new VSeq(
                                        new VSeq(VNumber.of(1, 1),
                                                VNumber.of(2, 1),
                                                new VDict()),
                                        new VDict()
                                            .put(new VString("tom"),
                                                    new VSeq())
                                            .put(new VString("dick"),
                                                    new VSeq(new VString("a")))
                                            .put(new VString("harry"),
                                                    new VSeq(VNumber.of(4, 1),
                                                            VNumber.of(5, 1),
                                                            VNumber.of(6, 1)))))
                        .put(new VString("bar"),
                                new VSeq(new VSeq(new VSeq(new VSeq(
                                        new VSeq()))))));
    }
    
    @Test
    public void introStatement()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x;", "x", VUnavailable.INSTANCE);
    }
    
    @Test
    public void introStatementWithInitialValue()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 5;", "x", VNumber.of(5, 1));
    }
    
    @Test
    public void topLevelAssign()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x; x = 5;", "x", VNumber.of(5, 1));
    }
    
    @Test
    public void multiLevelAssignment()
            throws ExecutionException, StaticCodeException {
        executionTest(
                  "intro x = {foo: [1, {bar: {bazz: [9, 8, 7]}}, 3]};"
                + "x.foo[1].bar.bazz[2] = 6;",
                "x.foo[1].bar.bazz[2]", VNumber.of(6, 1));
    }
    
    @Test
    public void selfAssign()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 'a'; x = x + 'b';", "x", new VString("ab"));
    }
    
    @Test
    public void simpleIfT()
            throws ExecutionException, StaticCodeException {
        executionTest("intro p = true; intro x; if p { x = 5; }", "x",
                VNumber.of(5, 1));
    }
    
    @Test
    public void simpleIfF()
            throws ExecutionException, StaticCodeException {
        executionTest("intro p = false; intro x; if p { x = 5; }", "x",
                VUnavailable.INSTANCE);
    }
    
    @Test
    public void ifElseTrue()
            throws ExecutionException, StaticCodeException {
        executionTest(
                "intro p = true; intro x; if p { x = 1; } else { x = 2; }", "x",
                VNumber.of(1, 1));
    }
    
    @Test
    public void ifElseFalse()
            throws ExecutionException, StaticCodeException {
        executionTest(
                "intro p = false; intro x; if p { x = 1; } else { x = 2; }",
                "x", VNumber.of(2, 1));
    }
    
    @Test
    public void ifElseIfTrue()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro y = 1;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if';"
                  + "}",
                    "x", new VString("if"));
    }
    
    @Test
    public void ifElseIfTrue2()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro y = 2;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if';"
                  + "}",
                    "x", new VString("else if"));
    }
    
    @Test
    public void ifElseIfFalse()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro y = 3;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if';"
                  + "}",
                    "x", VUnavailable.INSTANCE);
    }
    
    @Test
    public void fullIfTrue1()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro y = 1;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("if"));
    }
    
    @Test
    public void fullIfTrue2()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro y = 2;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("else if 1"));
    }
    
    @Test
    public void fullIfTrue3()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro y = 3;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("else if 2"));
    }
    
    @Test
    public void fullIfFalse()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro y = 4;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("else"));
    }
    
    @Test
    public void forEachExplicitPool()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = '';"
                  + "for each {'abc', 'def', 'ghi'} {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("abcdefghi"));
    }
    
    @Test
    public void forEachExplicitPoolWithExemplar()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = '';"
                  + "for each s : {'abc', 'def', 'ghi'} {"
                  + "    x = x + s;"
                  + "}",
                    "x", new VString("abcdefghi"));
    }
    
    @Test
    public void forEachSequence()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = '';"
                  + "for each ['abc', 'def', 'ghi'] {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("abcdefghi"));
    }
    
    @Test
    public void nestedForEach()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = '';"
                  + "for each s : ['a', 'b', 'c'] {"
                  + "    for each {'d', 'e'} {"
                  + "        x = x + s + @;"
                  + "    }"
                  + "}",
                    "x", new VString("adaebdbecdce"));
    }
    
    @Test
    public void forEachWhere()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = '';"
                  + "for each {3, 1, 4, 1, 5, 9, 2} where @ > 4 {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("59"));
    }
    
    @Test
    public void forEachWhereExemplar()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = '';"
                  + "for each s : {3, 1, 4, 1, 5, 9, 2} where s > 4 {"
                  + "    x = x + s;"
                  + "}",
                    "x", new VString("59"));
    }
    
    @Test
    public void forEachNoValues()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "for each s : [] {"
                  + "    #mem(6);"
                  + "}",
                    "#mem(none)", VUnavailable.INSTANCE);
    }
    
    @Test
    public void evaluationStatement()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = fn (y) { #mem(y); };"
                  + "x(6);",
                    "#mem(unavailable)", VNumber.of(6, 1));
    }
    
    @Test
    public void pickFromExpression()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro asdf = ['a'];", "pick from asdf", new VString("a"),
                    true);
    }
    
    @Test
    public void introInitializerValueSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = [1, 2, 3];"
                  + "intro y = x;"
                  + "x[1] = 9;",
                    "[x[1], y[1]]",
                    new VSeq(VNumber.of(9, 1), VNumber.of(2, 1)));
    }
    
    @Test
    public void topLevelAssignmentValueSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = [1, 2, 3];"
                  + "intro y;"
                  + "y = x;"
                  + "x[1] = 9;",
                    "[x[1], y[1]]",
                    new VSeq(VNumber.of(9, 1), VNumber.of(2, 1)));
    }
    
    @Test
    public void dictionaryFieldValueSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = [1, 2, 3];"
                  + "intro y = {};"
                  + "y.foo = x;"
                  + "x[1] = 9;",
                    "[x[1], y.foo[1]]",
                    new VSeq(VNumber.of(9, 1), VNumber.of(2, 1)));
    }
    
    @Test
    public void sequenceElementValueSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = [1, 2, 3];"
                  + "intro y = ['z'];"
                  + "y[0] = x;"
                  + "x[1] = 9;",
                    "[x[1], y[0][1]]",
                    new VSeq(VNumber.of(9, 1), VNumber.of(2, 1)));
    }
    
    @Test
    public void functionParameterValueSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = [1, 2, 3];"
                  + "intro y = fn (xs) { xs[1] = 9; };"
                  + "y(x);",
                    "x[1]", VNumber.of(2, 1));
    }
    
    @Test
    public void pickExpressionValueSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = [[1, 2, 3]];"
                  + "(pick from x)[1] = 9;",
                    "x[0][1]", VNumber.of(2, 1), true);
    }
    
    @Test
    public void forEachValueSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = [[1, 2, 3]];"
                  + "for each e : x {"
                  + "   e[1] = 9;"
                  + "}",
                    "x[0][1]", VNumber.of(2, 1), true);
    }
    
    @Test
    public void proxyIntroInitializerReferenceSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "intro y = x;"
                  + "x.foo = 'def';",
                    "[x.foo, y.foo]",
                    new VSeq(new VString("def"), new VString("def")));
    }
    
    @Test
    public void proxyTopLevelAssignmentReferenceSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "intro y;"
                  + "y = x;"
                  + "x.foo = 'def';",
                    "[x.foo, y.foo]",
                    new VSeq(new VString("def"), new VString("def")));
    }
    
    @Test
    public void proxyDictionaryFieldReferenceSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "intro y = {};"
                  + "y.bar = x;"
                  + "x.foo = 'def';",
                    "[x.foo, y.bar.foo]",
                    new VSeq(new VString("def"), new VString("def")));
    }
    
    @Test
    public void proxySequenceElementReferenceSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "intro y = ['z'];"
                  + "y[0] = x;"
                  + "x.foo = 'def';",
                    "[x.foo, y[0].foo]",
                    new VSeq(new VString("def"), new VString("def")));
    }
    
    @Test
    public void proxyFunctionParameterReferenceSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "intro y = fn (xs) { xs.foo = 'def'; };"
                  + "y(x);",
                    "x.foo", new VString("def"));
    }
    
    @Test
    public void proxyPickStatementReferenceSemantics()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "(pick from [x]).foo = 'def';",
                    "x.foo", new VString("def"), true);
    }
    
    @Test
    public void proxyCannotContainProxyField()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = #proxy('y');",
                ExecutionException.ErrorType.ILLEGAL_PROXY_CONTAINMENT);
    }
    
    @Test
    public void proxyCannotContainProxyElement()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = [#proxy('y')];",
                ExecutionException.ErrorType.ILLEGAL_PROXY_CONTAINMENT);
    }
    
    @Test
    public void previouslyValidProxieForbiddenByBake()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = {foo: #proxy('x')};"
                  + "bake y = x;", 
                ExecutionException.ErrorType.CANNOT_BAKE_PROXY);
    }
    
    @Test
    public void previouslyProxyUnsafeHierarchyOkWhenCopiedOut()
            throws ExecutionException, StaticCodeException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = {bar: ['z']};"
                  + "intro y = x.foo;"
                  + "y.bar[0] = x;",
                    "true", VBoolean.TRUE);
    }
    
    @Test
    public void noSuchSymbol()
            throws ExecutionException, StaticCodeException {
        executionTest("x;", StaticCodeException.ErrorType.NO_SUCH_SYMBOL);
    }
    
    @Test
    public void accessible()
            throws ExecutionException, StaticCodeException {
        executionTest("bake x;", "fn() { return x; }()", VUnavailable.INSTANCE);
    }
    
    @Test
    public void inaccessible()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x; fn() { return x; };",
                StaticCodeException.ErrorType.INACCESSIBLE_SYMBOL);
    }
    
    @Test
    public void duplicateIntroIntro()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x; intro x;",
                StaticCodeException.ErrorType.DUPLICATE_SYMBOL);
    }
    
    @Test
    public void duplicateParamIntro()
            throws ExecutionException, StaticCodeException {
        executionTest("fn (x) { intro x; }",
                StaticCodeException.ErrorType.DUPLICATE_SYMBOL);
    }
    
    @Test
    public void duplicateParamParam()
            throws ExecutionException, StaticCodeException {
        executionTest("fn (x, x) { }",
                StaticCodeException.ErrorType.DUPLICATE_SYMBOL);
    }
    
    @Test
    public void duplicateForEachIntro()
            throws ExecutionException, StaticCodeException {
        executionTest("for each x : [] { intro x; }",
                StaticCodeException.ErrorType.DUPLICATE_SYMBOL);
    }
    
    @Test
    public void pickExemplarEvaporates()
            throws ExecutionException, StaticCodeException {
        executionTest("(pick from x : [fn(){}])(); intro x;", "true",
                VBoolean.TRUE, true);
    }
    
    @Test
    public void forEachExemplarEvaporates()
            throws ExecutionException, StaticCodeException {
        executionTest("for each x : []{} intro x;", "true", VBoolean.TRUE);
    }
    
    @Test
    public void pickStatementDisallowed() 
            throws ExecutionException, StaticCodeException {
        executionTest("pick from {}();",
                StaticCodeException.ErrorType.GENERIC_SYNTAX_ERROR);
    }
    
    @Test
    public void topLevelAssignmentToBakedVarForbidden()
            throws ExecutionException, StaticCodeException {
        executionTest("bake x = 5; x = 6;",
                StaticCodeException.ErrorType.BAKED_MODIFICATION_FORBIDDEN);
    }
    
    @Test
    public void fieldAssignmentToBakedVarForbidden()
            throws ExecutionException, StaticCodeException {
        executionTest("bake x = {}; x.foo = 6;",
                StaticCodeException.ErrorType.BAKED_MODIFICATION_FORBIDDEN);
    }
    
    @Test
    public void seqAssignmentToBakedVarForbidden()
            throws ExecutionException, StaticCodeException {
        executionTest("bake x = ['x']; x[0]= 6;",
                StaticCodeException.ErrorType.BAKED_MODIFICATION_FORBIDDEN);
    }
    
    @Test
    public void multiLevelAssignmentToBakedVarForbidden()
            throws ExecutionException, StaticCodeException {
        executionTest(
                  "bake x = {foo: [1, {bar: {bazz: [9, 8, 7]}}, 3]};"
                + "x.foo[1].bar.bazz[2] = 6;",
                StaticCodeException.ErrorType.BAKED_MODIFICATION_FORBIDDEN);
    }
    
    @Test
    public void upvalues()
            throws ExecutionException, StaticCodeException {
        executionTest(
                "intro foo = fn() {"
              + "  bake x = 6;"
              + "  return fn() { return x; };"
              + "};",
                "foo()()", VNumber.of(6, 1));
    }
    
    @Test
    public void tokenEquality() throws ExecutionException, StaticCodeException,
            Scedel.HostEnvironmentException {
        evaluationTest("#token('abc') = #token('abc')", VBoolean.TRUE);
    }
    
    @Test
    public void tokenCanBeBaked()
            throws ExecutionException, StaticCodeException {
        executionTest("bake x = {foo: #token('abc')};", "true", VBoolean.TRUE);
    }
    
    @Test
    public void divisionByZero()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 5 / 0;",
                ExecutionException.ErrorType.DIVISION_BY_ZERO);
    }
    
    @Test
    public void incorrectParameterCount()
            throws ExecutionException, StaticCodeException {
        executionTest("intro f = fn(x, y) { return x; }; f(1, 2, 3);",
                ExecutionException.ErrorType.INCORRECT_NUMBER_OF_PARAMETERS);
    }
    
    @Test
    public void negativeIndex()
            throws ExecutionException, StaticCodeException {
        executionTest("intro s = []; s = s[-1];",
                ExecutionException.ErrorType.NEGATIVE);
    }
    
    @Test
    public void negativePickCount()
            throws ExecutionException, StaticCodeException {
        executionTest("(pick -1 from [])();",
                ExecutionException.ErrorType.NEGATIVE);
    }
    
    @Test
    public void negativeWeight()
            throws ExecutionException, StaticCodeException {
        executionTest("(pick 1 from {'abc' {-1}})();",
                ExecutionException.ErrorType.NEGATIVE);
    }
    
    @Test
    public void nonIntegralIndex()
            throws ExecutionException, StaticCodeException {
        executionTest("intro s = []; s = s[1/2];",
                ExecutionException.ErrorType.NON_INTEGRAL);
    }
    
    @Test
    public void nonIntegralPickCount()
            throws ExecutionException, StaticCodeException {
        executionTest("(pick 1/2 from [])();",
                ExecutionException.ErrorType.NON_INTEGRAL);
    }
    
    @Test
    public void nonIntegralWeight()
            throws ExecutionException, StaticCodeException {
        executionTest("(pick 1 from {'abc' {1/2}})();",
                ExecutionException.ErrorType.NON_INTEGRAL);
    }
    
    @Test
    public void nonIntegralExponent()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 2 ^ (1/2);",
                ExecutionException.ErrorType.NON_INTEGRAL);
    }
    
    @Test
    public void andNotABoolean1()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 2 and false;",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void andNotABoolean2()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = true and 2;",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void orNotABoolean1()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 2 or false;",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void orNotABoolean2()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = false or 2;",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void notNotABoolean()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = not 2;",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void uniqueNotABoolean()
            throws ExecutionException, StaticCodeException {
        executionTest("(pick 1 unique(3) from [])();",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void ifNotABoolean()
            throws ExecutionException, StaticCodeException {
        executionTest("if 2 {}",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void elseIfNotABoolean()
            throws ExecutionException, StaticCodeException {
        executionTest("if false {} else if 2 {}",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void forEachWhereNotABoolean()
            throws ExecutionException, StaticCodeException {
        executionTest("for each ['abc'] where 2 {}",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void pickWhereNotABoolean()
            throws ExecutionException, StaticCodeException {
        executionTest("(pick from ['abc'] where 2)();",
                ExecutionException.ErrorType.NOT_A_BOOLEAN);
    }
    
    @Test
    public void notADictionary()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = [].foo;",
                ExecutionException.ErrorType.NOT_A_DICTIONARY);
    }
    
    @Test
    public void notAFunction()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = []();",
                ExecutionException.ErrorType.NOT_A_FUNCTION);
    }
    
    @Test
    public void notANumber()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 2 + 'abc';",
                ExecutionException.ErrorType.NOT_A_NUMBER);
    }
    
    @Test
    public void notASequence()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = 'abc'[2];",
                ExecutionException.ErrorType.NOT_A_SEQUENCE);
    }
    
    @Test
    public void wrongPlusFirstParam()
            throws ExecutionException, StaticCodeException {
        executionTest("intro x = [] + 3;",
                ExecutionException.ErrorType
                        .PLUS_FIRST_PARAMETER_MUST_BE_NUMBER_OR_STRING);
    }
    
    @Test
    public void stackTrace() throws ExecutionException, StaticCodeException {
        List<ParseLocation> st = executionTest(
                "intro f = fn(g) {\n"
              + "  return g(0);\n"
              + "};\n"
              + "intro h = fn(x) {\n"
              + "  return 5 / x;\n"
              + "};\n"
              + "intro y = f(h);",
                ExecutionException.ErrorType.DIVISION_BY_ZERO);
        
        Assert.assertEquals(2, st.get(0).getLineNumber());
        Assert.assertEquals(7, st.get(1).getLineNumber());
        Assert.assertEquals(2, st.size());
    }
    
    private static <T> List<T> list(final T ... ts) {
        return Arrays.asList(ts);
    }
    
    private List<ParseLocation> executionTest(String setup,
            ExecutionException.ErrorType type, boolean ... deciderValues) {
        
        List<ParseLocation> stackTrace = null;
        try {
            executionTest(setup, "true", VBoolean.TRUE, deciderValues);
            Assert.fail("Expected error: " + type);
        }
        catch (ExecutionException ee) {
            Assert.assertEquals(
                    "Expected error: " + type, type, ee.getErrorType());
            
            stackTrace = ee.getScriptStackTrace();
        }
        catch (StaticCodeException sce) {
            Assert.fail("Expected execution exception " + type
                    + ". Got static code exception: " + sce.getErrorType());
        }
        
        return stackTrace;
    }
    
    private void executionTest(String setup, StaticCodeException.ErrorType type,
            boolean ... deciderValues) {
        try {
            executionTest(setup, "true", VBoolean.TRUE, deciderValues);
            Assert.fail("Expected error: " + type);
        }
        catch (ExecutionException see) {
            Assert.fail("Expected static code exception " + type
                    + ". Got execution exception: " + see.getErrorType());
        }
        catch (StaticCodeException see) {
            Assert.assertEquals(
                    "Expected error: " + type, type, see.getErrorType());
        }
    }
    
    private void executionTest(String setup, String expression, Value expected,
            boolean ... deciderValues)
            throws ExecutionException, StaticCodeException {
        TestEnvironment h = new TestEnvironment();
        TestDecider d = new TestDecider();
        
        Scedel s = new Scedel(h, d);

        d.setResults(deciderValues);

        try {
            s.run(setup + "#out(" + expression + ");");
        }
        catch (ExecutionException ee) {
            ee.print(System.out);
            throw ee;
        }
        catch (StaticCodeException sce) {
            sce.print(System.out);
            throw sce;
        }

        Assert.assertEquals(expected, h.myOut.get(0));
    }
    
    private void evaluationTest(String expression, Value expectedResult,
            boolean ... deciderResults)
            throws ExecutionException, StaticCodeException,
                Scedel.HostEnvironmentException {
        Scedel.HostEnvironment h = Mockito.mock(Scedel.HostEnvironment.class);
        TestDecider d = new TestDecider();
        
        Scedel s = new Scedel(h, d);
        
        Mockito.reset(h);
        Mockito.when(h.evaluate(Mockito.anyString(), Mockito.anyList()))
            .thenReturn(VUnavailable.INSTANCE);

        d.setResults(deciderResults);

        try {
            s.run("#out(" + expression + ");");
        }
        catch (ExecutionException ee) {
            ee.print(System.out);
            throw ee;
        }
        catch (StaticCodeException sce) {
            sce.print(System.out);
            throw sce;
        }

        Mockito.verify(h).evaluate("out", list(expectedResult));
    }
    
    private static class TestDecider implements Scedel.Decider {
        private Iterator<Boolean> myResults;
        
        public void setResults(boolean[] results) {
            List<Boolean> weirdness = new ArrayList<>(results.length);
            for (boolean b : results) {
                weirdness.add(b);
            }
            
            myResults = weirdness.iterator();
        }
        
        @Override
        public boolean decide(double chance) {
            return myResults.next();
        }
    }
    
    private static class ExecutionTest extends EvaluationTest {
        private final String mySetup;
        
        public ExecutionTest(String name, String setup, String expression,
                Value expected, boolean... deciderResults) {
            super(name, expression, expected, deciderResults);
            
            mySetup = setup;
        }
        
        public String getSetup() {
            return mySetup;
        }
    }
    
    private static class EvaluationTest {
        private final String myName;
        private final String myExpression;
        private final Value myExpectedResult;
        private final boolean[] myDeciderResults;
        
        public EvaluationTest(String name, String expression, Value expected,
                boolean ... deciderResults) {
            myName = name;
            myExpression = expression;
            myExpectedResult = expected;
            myDeciderResults = deciderResults;
        }
        
        public String getName() {
            return myName;
        }
        
        public String getExpression() {
            return myExpression;
        }
        
        public Value getExpected() {
            return myExpectedResult;
        }
        
        public boolean[] getDeciderValues() {
            return myDeciderResults;
        }
    }
    
    private class TestEnvironment implements Scedel.HostEnvironment {
        private List<Value> myOut;
        private Value myMem = VUnavailable.INSTANCE;
        
        private final Map<String, VProxy> myProxies = new HashMap<>();
        
        public void reset() {
            myOut = null;
            myMem = VUnavailable.INSTANCE;
        }
        
        @Override
        public Value evaluate(String name, List<Value> parameters)
                throws Scedel.HostEnvironmentException {
            Value result;
            
            if (name.equals("out")) {
                myOut = parameters;
                result = VUnavailable.INSTANCE;
            }
            else if (name.equals("mem")) {
                result = myMem;
                myMem = parameters.get(0);
            }
            else if (name.equals("proxy")) {
                String proxyName = ((VString) parameters.get(0)).getValue();
                result = myProxies.get(proxyName);
                
                if (result == null) {
                    result = new VProxy();
                }
                
                myProxies.put(proxyName, (VProxy) result);
            }
            else if (name.equals("token")) {
                result = new VToken(
                        parameters.get(0).assertIsString(
                                ParseLocation.INTERNAL).getValue());
            }
            else {
                throw new RuntimeException();
            }
            
            return result;
        }
    }
}
