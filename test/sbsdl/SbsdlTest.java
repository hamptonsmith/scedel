package sbsdl;

import com.shieldsbetter.flexcompilator.WellFormednessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import sbsdl.values.VBoolean;
import sbsdl.values.VDict;
import sbsdl.values.VNone;
import sbsdl.values.VNumber;
import sbsdl.values.VProxy;
import sbsdl.values.VSeq;
import sbsdl.values.VString;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class SbsdlTest {
    @Test
    public void unavailableLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("unavailable", VUnavailable.INSTANCE);
    }
    
    @Test
    public void noneLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("none", VNone.INSTANCE);
    }
    
    @Test
    public void trueLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("true", VBoolean.TRUE);
    }
    
    @Test
    public void falseLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("false", VBoolean.FALSE);
    }
    
    @Test
    public void andTT()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("true and true", VBoolean.TRUE);
    }
    
    @Test
    public void andTF()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("true and false", VBoolean.FALSE);
    }
    
    @Test
    public void andFT()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("false and true", VBoolean.FALSE);
    }
    
    @Test
    public void andFF()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("false and false", VBoolean.FALSE);
    }
    
    @Test
    public void orTT()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("true or true", VBoolean.TRUE);
    }
    
    @Test
    public void orTF()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("true or false", VBoolean.TRUE);
    }
    
    @Test
    public void orFT()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("false or true", VBoolean.TRUE);
    }
    
    @Test
    public void orFF()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("false or false", VBoolean.FALSE);
    }
    
    @Test
    public void notT()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("not true", VBoolean.FALSE);
    }
    
    @Test
    public void notF()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("not false", VBoolean.TRUE);
    }
    
    @Test
    public void fractionalNumber()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("123.457", VNumber.of(123457, 1000));
    }
    
    @Test
    public void wholeNumber()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("123", VNumber.of(123, 1));
    }
    
    @Test
    public void stringLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("'hello \\'quoted'", new VString("hello 'quoted"));
    }
    
    @Test
    public void emptyStringLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("''", new VString(""));
    }
    
    @Test
    public void sequenceLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("[3.1, 'foo', []]",
                new VSeq(VNumber.of(31, 10), new VString("foo"), new VSeq()));
    }
    
    @Test
    public void emptySequence()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("[]", new VSeq());
    }
    
    @Test
    public void singletonSequence()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("[1]", new VSeq(VNumber.of(1, 1)));
    }
    
    @Test
    public void functionLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("fn(x) { return x; }(123)", VNumber.of(123, 1));
    }
    
    @Test
    public void uniquePickExplicitWeight()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique from { 'foo' {1}, 'bar' {5}, 'bazz' {5} }", 
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void uniquePickImplicitWeight()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void expressionParameterizedUniqueTrue()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique(true) from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void expressionParameterizedUniqueFalse()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique(false) from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("foo")),
                true, false, false, true, false, false);
    }
    
    @Test
    public void pickExplicitWeight()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 2 from { 'foo' {1}, 'bar' {5}, 'bazz' {5} }", 
                new VSeq(new VString("foo"), new VString("foo")),
                true, false, false, true, false, false);
    }
    
    @Test
    public void pickImplicitWeight()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 2 from { 'foo', 'bar', 'bazz' }", 
                new VSeq(new VString("foo"), new VString("foo")),
                true, false, false, true, false, false);
    }
    
    @Test
    public void pickSingleYeildsElementInsteadOfSequence()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 1 from { 'foo', 'bar', 'bazz' }", 
                new VString("foo"),
                true, false, false);
    }
    
    @Test
    public void pickImplicitCount()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick from { 'foo', 'bar', 'bazz' }", 
                new VString("foo"),
                true, false, false);
    }
    
    @Test
    public void pickFromSequence()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 2 unique from [ 'foo', 'bar', 'bazz' ]",
                new VSeq(new VString("foo"), new VString("bazz")),
                true, false, false, false, true);
    }
    
    @Test
    public void pickingZeroYeildsEmptySequence()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest(
                "pick 0 from [ 'foo', 'bar', 'bazz' ]", new VSeq());
    }
    
    @Test
    public void pickingOneFromNoGoodOptionsYieldsUnavailable()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("pick 1 from {'foo' {0}, 'bar' {0}, 'bazz' {0}}",
                VUnavailable.INSTANCE);
    }
    
    @Test
    public void pickingMultilpleFromNoGoodOptionsYieldsSequenceOfUnavailable()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("pick 3 from {'foo' {0}, 'bar' {0}, 'bazz' {0}}",
                new VSeq(VUnavailable.INSTANCE, VUnavailable.INSTANCE,
                        VUnavailable.INSTANCE));
    }
    
    @Test
    public void dictionaryLiteral()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("{foo: 5, bar: 2 + 4}",
                    new VDict().put(new VString("foo"), VNumber.of(5, 1))
                            .put(new VString("bar"), VNumber.of(6, 1)));
    }
    
    @Test
    public void emptyDictionary()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("{}", new VDict());
    }
    
    @Test
    public void singletonDictionary()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("{foo: 5}",
                    new VDict().put(new VString("foo"), VNumber.of(5, 1)));
    }
    
    @Test
    public void sequenceIndexing()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("[3, 4, 5][1]", VNumber.of(4, 1));
    }
    
    @Test
    public void dictionaryAccess()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("{foo: 5, bar: 6, bazz: 9}.bar", VNumber.of(6, 1));
    }
    
    @Test
    public void dictionaryAccessExpression()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("{foo: 5, bar: 6, bazz: 9}.('bar')", VNumber.of(6, 1));
    }
    
    @Test
    public void orderOfOperations()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("1 + 3 * 5 * 2 ^ 4 * (6 + 7)", VNumber.of(3121, 1));
    }
    
    @Test
    public void stringConcatenation()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("'foo' + 'bar'", new VString("foobar"));
    }
    
    @Test
    public void stringConcatenationWithNumbers()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        evaluationTest("'foo' + 4.1", new VString("foo(41 / 10)"));
    }
    
    @Test
    public void nestedStructures()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro x;", "x", VUnavailable.INSTANCE);
    }
    
    @Test
    public void introStatementWithInitialValue()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro x = 5;", "x", VNumber.of(5, 1));
    }
    
    @Test
    public void topLevelAssign()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro x; x = 5;", "x", VNumber.of(5, 1));
    }
    
    @Test
    public void selfAssign()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro x = 'a'; x = x + 'b';", "x", new VString("ab"));
    }
    
    @Test
    public void simpleIfT()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro p = true; intro x; if p { x = 5; }", "x",
                VNumber.of(5, 1));
    }
    
    @Test
    public void simpleIfF()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro p = false; intro x; if p { x = 5; }", "x",
                VUnavailable.INSTANCE);
    }
    
    @Test
    public void ifElseTrue()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                "intro p = true; intro x; if p { x = 1; } else { x = 2; }", "x",
                VNumber.of(1, 1));
    }
    
    @Test
    public void ifElseFalse()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                "intro p = false; intro x; if p { x = 1; } else { x = 2; }",
                "x", VNumber.of(2, 1));
    }
    
    @Test
    public void ifElseIfTrue()
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = '';"
                  + "for each {'abc', 'def', 'ghi'} {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("abcdefghi"));
    }
    
    @Test
    public void forEachExplicitPoolWithExemplar()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = '';"
                  + "for each s : {'abc', 'def', 'ghi'} {"
                  + "    x = x + s;"
                  + "}",
                    "x", new VString("abcdefghi"));
    }
    
    @Test
    public void forEachSequence()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = '';"
                  + "for each ['abc', 'def', 'ghi'] {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("abcdefghi"));
    }
    
    @Test
    public void nestedForEach()
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = '';"
                  + "for each {3, 1, 4, 1, 5, 9, 2} where @ > 4 {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("59"));
    }
    
    @Test
    public void forEachWhereExemplar()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = '';"
                  + "for each s : {3, 1, 4, 1, 5, 9, 2} where s > 4 {"
                  + "    x = x + s;"
                  + "}",
                    "x", new VString("59"));
    }
    
    @Test
    public void evaluationStatement()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = fn (y) { #mem(y); };"
                  + "x(6);",
                    "#mem(unavailable)", VNumber.of(6, 1));
    }
    
    @Test
    public void pickFromExpression()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro asdf = ['a'];", "pick from asdf", new VString("a"),
                    true);
    }
    
    @Test
    public void introInitializerValueSemantics()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = [1, 2, 3];"
                  + "intro y = x;"
                  + "x[1] = 9;",
                    "[x[1], y[1]]",
                    new VSeq(VNumber.of(9, 1), VNumber.of(2, 1)));
    }
    
    @Test
    public void topLevelAssignmentValueSemantics()
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = [1, 2, 3];"
                  + "intro y = fn (xs) { xs[1] = 9; };"
                  + "y(x);",
                    "x[1]", VNumber.of(2, 1));
    }
    
    @Test
    public void pickStatementValueSemantics()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = [[1, 2, 3]];"
                  + "(pick from x)[1] = 9;",
                    "x[0][1]", VNumber.of(2, 1), true);
    }
    
    @Test
    public void proxyIntroInitializerReferenceSemantics()
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
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
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "intro y = fn (xs) { xs.foo = 'def'; };"
                  + "y(x);",
                    "x.foo", new VString("def"));
    }
    
    @Test
    public void proxyPickStatementReferenceSemantics()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = 'abc';"
                  + "(pick from [x]).foo = 'def';",
                    "x.foo", new VString("def"), true);
    }
    
    @Test
    public void proxyCannotContainProxyField()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = #proxy('y');",
                "cannot contain");
    }
    
    @Test
    public void proxyCannotContainProxyElement()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = [#proxy('y')];",
                "cannot contain");
    }
    
    @Test
    public void previouslyProxyUnsafeHierarchyOkWhenCopiedOut()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest(
                    "intro x = #proxy('x');"
                  + "x.foo = {bar: ['z']};"
                  + "intro y = x.foo;"
                  + "y.bar[0] = x;",
                    "true", VBoolean.TRUE);
    }
    
    @Test
    public void noSuchSymbol()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("x;", "no such");
    }
    
    @Test
    public void inaccessible()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro x; fn() {x;};", "accessible");
    }
    
    @Test
    public void duplicateIntroIntro()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("intro x; intro x;", "already");
    }
    
    @Test
    public void duplicateParamIntro()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("fn (x) { intro x; }", "already");
    }
    
    @Test
    public void duplicateParamParam()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("fn (x, x) { }", "already");
    }
    
    @Test
    public void duplicateForEachIntro()
            throws Sbsdl.ExecutionException, WellFormednessException {
        executionTest("for each x : [] { intro x; }", "already");
    }
    
    private static <T> List<T> list(final T ... ts) {
        return Arrays.asList(ts);
    }
    
    private void executionTest(
            String setup, String errorContains, boolean ... deciderValues)
            throws WellFormednessException {
        try {
            executionTest(setup, "true", VBoolean.TRUE, deciderValues);
            Assert.fail("Expected error containing: " + errorContains);
        }
        catch (Sbsdl.ExecutionException see) {
            Assert.assertTrue("Error supposed to contain '"
                    + errorContains + "'.  Was: " + see.getMessage(),
                    see.getMessage().toLowerCase().contains(errorContains));
            
            System.out.println("Got expected error:\n" + see.getMessage());
        }
    }
    
    private void executionTest(String setup, String expression, Value expected,
            boolean ... deciderValues)
            throws Sbsdl.ExecutionException, WellFormednessException {
        TestEnvironment h = new TestEnvironment();
        TestDecider d = new TestDecider();
        
        Sbsdl s = new Sbsdl(h, d);

        d.setResults(deciderValues);

        try {
            s.run(setup + "#out(" + expression + ");");
        }
        catch (WellFormednessException wfe) {
            System.out.println(wfe);
            throw wfe;
        }

        Assert.assertEquals(expected, h.myOut.get(0));
    }
    
    private void evaluationTest(String expression, Value expectedResult,
            boolean ... deciderResults)
            throws Sbsdl.HostEnvironmentException, WellFormednessException {
        Sbsdl.HostEnvironment h = Mockito.mock(Sbsdl.HostEnvironment.class);
        TestDecider d = new TestDecider();
        
        Sbsdl s = new Sbsdl(h, d);
        
        Mockito.reset(h);
        Mockito.when(h.evaluate(Mockito.anyString(), Mockito.anyList()))
            .thenReturn(VUnavailable.INSTANCE);

        d.setResults(deciderResults);

        try {
            s.run("#out(" + expression + ");");
        }
        catch (WellFormednessException wfe) {
            System.out.println(wfe);
            throw wfe;
        }

        Mockito.verify(h).evaluate("out", list(expectedResult));
    }
    
    private static class TestDecider implements Sbsdl.Decider {
        private Iterator<Boolean> myResults;
        
        public void setResults(boolean[] results) {
            List<Boolean> weirdness = new ArrayList<>(results.length);
            for (boolean b : results) {
                weirdness.add(b);
            }
            
            myResults = weirdness.iterator();
        }
        
        @Override
        public boolean randomize(double chance) {
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
    
    private class TestEnvironment implements Sbsdl.HostEnvironment {
        private List<Value> myOut;
        private Value myMem = VUnavailable.INSTANCE;
        
        private final Map<String, VProxy> myProxies = new HashMap<>();
        
        public void reset() {
            myOut = null;
            myMem = VUnavailable.INSTANCE;
        }
        
        @Override
        public Value evaluate(String name, List<Value> parameters)
                throws Sbsdl.HostEnvironmentException {
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
            else {
                throw new RuntimeException();
            }
            
            return result;
        }
    }
}
