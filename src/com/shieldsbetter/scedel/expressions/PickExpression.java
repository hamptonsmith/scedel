package com.shieldsbetter.scedel.expressions;

import com.shieldsbetter.scedel.InternalExecutionException;
import java.util.ArrayList;
import java.util.List;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Picker;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.Scedel.HostEnvironment;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.VFunction;
import com.shieldsbetter.scedel.values.VNumber;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.VUnavailable;
import com.shieldsbetter.scedel.values.Value;
import java.util.LinkedList;

/**
 * <p>Fully explicit, a {@code pick expression} looks like this:</p>
 * 
 * <p><strong>pick</strong> <em>&lt;round count&gt;</em>
 * <strong>unique(</strong><em>&lt;unique flag&gt;</em><strong>) from</strong>
 * <em>&lt;exemplar name&gt;</em> <strong>:</strong>
 * <em>&lt;source collection&gt;</em> <strong>where</strong>
 * <em>&lt;selection predicate&gt;</em> <strong>weighted by</strong>
 * <em>&lt;weighter&gt;</em></p>
 * 
 * <p>Of these, only the <em>source collection</em> is required.  With all
 * optional parts removed, the shortest form of the expression looks like
 * this:</p>
 * 
 * <p><strong>pick from</strong> <em>&lt;source collection&gt;</em></p>
 * 
 * <p>The default values of the optional clauses are as follows:</p>
 * 
 * <ul>
 *     <li><strong>round count</strong>: {@code 1}</li>
 *     <li><strong>unique flag</strong>: The <strong>unique</strong> keyword may
 *         be specified omitting the subsequent parenthetical
 *         <em>unique flag</em> expression, in which case the default value of
 *         the unique flag expression is {@code true}.  If the
 *         <strong>unique</strong> keyword itself is also omitted, the default
 *         value of the unique flag expression is {@code false}.</li>
 *     <li><strong>examplar</strong>: &#64;</li>
 *     <li><strong>selection predicate</strong>: {@code true}</li>
 *     <li><strong>weighted by</strong>: {@code fn(index, value) { return 1; }}
 * </ul>
 * 
 * <p>The semantics of a pick expression are then as follows:</p>
 * 
 * <ol>
 *     <li>Evaluate the <em>round count expression</em> into a
 *         <em>round count value</em>.  If this value is not a positive integral
 *         number of reasonable size, this is a runtime error.</li>
 *     <li>Evaluate the <em>unique flag expression</em> into a
 *         <em>unique flag value</em>.  If this value is not a a boolean, this
 *         is a runtime error.</li>
 *     <li>If the <em>source collection</em> is a <em>pick collection
 *         literal</em>, evaluate the value portion of each entry in order
 *         and concatenate them into a <em>source collection value</em>, then
 *         evaluate the weight portion of each entry in order and construct an
 *         appropriate <em>weighter</em>.  Otherwise, the <em>source
 *         collection</em> is an expression; evaluate it into a <em>source
 *         collection value</em>.  In the first case, the source collection
 *         value will be a sequence by construction.  In the second case, it is
 *         a runtime error for the source collection value not to be a
 *         sequence.</li>
 *     <li>If the <em>source collection</em> is a <em>pick collection
 *         literal</em>, and the <strong>weighted by</strong> clause has not
 *         been omitted, this is a syntax error.</li>
 *     <li>Evaluate the <em>weighter</em> into a <em>weighter value</em>.  If
 *         this value is not a sequence with length at least as large as the
 *         number of elements in the source collection value, a dictionary, or
 *         a function taking two arguments, this is a runtime error.</li>
 *     <li>For each non-{@code unavailable} element of the source collection
 *         value, the <em>selection predicate</em> is evaluated in a context
 *         where the exemplar symbol is mapped to that element's value.  It is a
 *         runtime error for this predicate to evaluate to a non-boolean value.
 *         If this evaluates to true:
 *         <ol>
 *             <li><em>Weight</em> (defined below) the candidate value using the
 *                 <em>weighter value</em>. It is a runtime error if the
 *                 resulting value is not a number, is a non-integral number, is
 *                 a negative number, or is a number of unreasonable size.</li>
 *             <li>Add the selected value and its weight as a pair to the
 *                 <em>selected list</em>, a meta-construct.</li>
 *         </ol>
 *     </li>
 *     <li>Now do the following a number of times equal to the <em>round count
 *         value</em>:
 *         <ol>
 *             <li>Defer to the current execution environment's <em>picker</em>
 *                 (a meta-construct) to pick a value/weight pair from the
 *                 <em>selected list</em>.</li>
 *             <li>If the picker throws a {@code Picker#CannotPickException},
 *                 this pick expression evaluates to {@code unavailable}.</li>
 *             <li>Otherwise, the picked value (i.e., the first part of the
 *                 picked value/weight pair) is added to the <em>picked
 *                 sequence</em>.</li>
 *             <li>If the <em>unique flag</em> is {@code true}, the weight of
 *                 the picked value/weight entry in the <em>selected list</em>
 *                 is changed to {@code 0}.</li>
 *         </ol>
 *     </li>
 *     <li>If the <em>round count value</em> is {@code 1}, this pick expression
 *         evaluates to the sole entry in the <em>picked sequence</em>.</li>
 *     <li>Otherwise, this pick expression evaluates to the <em>picked
 *         sequence</em> itself.  Note that this means that this pick expression
 *         evaluates to the empty sequence in cases where the <em>round count
 *         value</em> is {@code 0}.</li>
 * </ol>
 * 
 * <p>The process of <em>weighting</em> a candidate value is performed as
 * follows:</p>
 * 
 * <ul>
 *     <li>If the <em>weighter value</em> is a function, the candidate value's
 *         weight is the result of calling that function with a first parameter
 *         equal to the index of the candidate value in the source collection
 *         (not the selected list!) and its second parameter equal to the
 *         candidate value itself.</li>
 *     <li>If the <em>weighter value</em> is a dictionary, the candidate value's
 *         weight is determined as though the weighter value was
 *         {@code fn(index, value) { return d.(value); }}, where {@code d} is
 *         the original weighter value dictionary.</li>
 *     <li>If the <em>weighter value</em> is a sequence, the candidate value's
 *         weight is determined as though the weighter value was
 *         {@code fn(index, value) { return s[index]; }}, where {@code s} is the
 *         original weighter value sequence.</li>
 *     <li>In any case, if the resulting weight is not a number, or is a
 *         negative number, or a non-integral number, or is an unreasonably
 *         large number, this is a runtime error.</li>
 * </ul>
 */
public class PickExpression extends SkeletonExpression {
    private static final VFunction DEFAULT_WEIGHTER =
            VFunction.buildConstantFunction(2, VNumber.of(1, 1));
    
    private final Scedel.Symbol myExemplar;
    private final Expression myPool;
    private final Expression myCount;
    private final Expression myUniqueFlag;
    private final Expression myWeighter;
    private final Expression myWhere;
    
    public PickExpression(ParseLocation l, Scedel.Symbol exemplar,
            Expression pool, Expression count, Expression unique,
            Expression weighter, Expression where) {
        super(l);
        myExemplar = exemplar;
        myPool = pool;
        myCount = count;
        myUniqueFlag = unique;
        myWhere = where;
        
        if (weighter == null) {
            myWeighter = DEFAULT_WEIGHTER;
        }
        else {
            myWeighter = weighter;
        }
    }
    
    public Scedel.Symbol getExamplar() {
        return myExemplar;
    }
    
    public Expression getCollection() {
        return myPool;
    }
    
    public Expression getCount() {
        return myCount;
    }
    
    public Expression getUnique() {
        return myUniqueFlag;
    }
    
    public Expression getWeighExpression() {
        return myWeighter;
    }
    
    public Expression getWhere() {
        return myWhere;
    }
    
    @Override
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
        int requestedCt = myCount.evaluate(h, s).assertIsNumber(
                    myCount.getParseLocation())
                .assertNonNegativeReasonableInteger(
                    myCount.getParseLocation(), "pick round count");
        
        boolean unique =
                myUniqueFlag.evaluate(h, s).assertIsBoolean(
                        myUniqueFlag.getParseLocation()).getValue();
        
        VSeq poolSeq =
                myPool.evaluate(h, s).assertIsSeq(myPool.getParseLocation());
        
        Value weighter = myWeighter.evaluate(h, s);
        validateWeighter(weighter, poolSeq);
        
        int totalWeight = 0;
        List<Picker.Option> selectedList = new ArrayList<>(poolSeq.length());
        int index = 0;
        for (Value e : poolSeq.elements()) {
            s.pushScope(false);
            s.introduceSymbol(myExemplar, e);
            
            if (myWhere.evaluate(h, s).assertIsBoolean(
                    myWhere.getParseLocation()).getValue()) {
                int weight = weight(index, e, weighter, h, s);
                totalWeight += weight;
                
                selectedList.add(new Picker.Option(e, weight));
            }
            
            index++;
        }
        
        Value result;
        try {
            Picker picker = s.getPicker();
            VSeq picked = new VSeq();
            for (int i = 0; i < requestedCt; i++) {
                int pickedIndex = picker.pick(selectedList, totalWeight);

                Picker.Option pickedOption = selectedList.get(pickedIndex);
                picked.enqueue(pickedOption.getValue().copy(null));

                if (unique) {
                    totalWeight -= pickedOption.getWeight();
                    pickedOption.zeroWeight();
                }
            }
            
            if (requestedCt == 1) {
                result = picked.get(0);
            }
            else {
                result = picked;
            }
        }
        catch (Picker.CannotPickException cpe) {
            result = VUnavailable.INSTANCE;
        }
        
        return result;
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }
    
    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP PICK\n");
        
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("exemplar: ");
        b.append(myExemplar);
        b.append("\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "count:", myCount, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "unique:", myUniqueFlag, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "collection:", myPool, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "where:", myWhere, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "weight:", myWeighter, b);
    }

    @Override
    public void accept(Visitor v) {
        v.visitPickExpression(this);
    }

    private void validateWeighter(Value weighter, VSeq sourceCollection) {
        if (weighter instanceof VFunction) {
            if (((VFunction) weighter).getArgumentCount() != 2) {
                throw InternalExecutionException.invalidWeighter(
                        weighter.getParseLocation(),
                        "Weighter function does not take 2 arguments: "
                                + weighter, weighter);
            }
        }
        else if (weighter instanceof VSeq) {
            if (((VSeq) weighter).getElementCount()
                    < sourceCollection.getElementCount()) {
                throw InternalExecutionException.invalidWeighter(
                        weighter.getParseLocation(),
                        "Weighter sequence has too few elements: " + weighter,
                        weighter);
            }
        }
        else if (!(weighter instanceof VDict)) {
            throw InternalExecutionException.invalidWeighter(
                    weighter.getParseLocation(),
                    "Weighter is not a function, sequence, or dictionary: "
                            + weighter, weighter);
        }
    }
    
    private int weight(int index, Value v, Value weighter, HostEnvironment h,
            ScriptEnvironment s) {
        Value resultVal;
        
        if (weighter instanceof VFunction) {
            List<Value> parameters = new LinkedList<>();
            parameters.add(VNumber.of(index, 1));
            parameters.add(v.copy(null));
            resultVal = ((VFunction) weighter).call(
                    weighter.getParseLocation(), h, s, parameters);
        }
        else if (weighter instanceof VDict) {
            resultVal = ((VDict) weighter).get(v);
        }
        else if (weighter instanceof VSeq) {
            resultVal = ((VSeq) weighter).get(index);
        }
        else {
            throw new RuntimeException();
        }
        
        return resultVal.assertIsNumber(weighter.getParseLocation())
                .assertNonNegativeReasonableInteger(weighter.getParseLocation(),
                        "calculated weight for element " + index + " (" + v
                                + ")");
    }
}
