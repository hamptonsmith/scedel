package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.InternalExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Picker;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.Value;
import java.util.List;
import java.util.Random;

public class DecideStatement extends SkeletonStatement {
    private final Expression myDecider;
    private final MultiplexingStatement myCode;
    
    public DecideStatement(
            ParseLocation l, Expression d, MultiplexingStatement code) {
        super(l);
        myDecider = d;
        myCode = code;
    }
    
    public Expression getDecider() {
        return myDecider;
    }
    
    public MultiplexingStatement getCode() {
        return myCode;
    }

    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        final Value deciderValue = myDecider.evaluate(h, s);
        
        Picker p;
        if (deciderValue instanceof VSeq) {
            final VSeq indexes = (VSeq) deciderValue;
            p = new Picker() {
                        private int myLastIndex = -1;
                
                        @Override
                        public int pick(
                                List<Picker.Option> options, int totalWeight)
                                throws Picker.CannotPickException {
                            myLastIndex++;
                            
                            if (myLastIndex == indexes.getElementCount()) {
                                throw InternalExecutionException
                                        .deciderIndexesExhausted(
                                                myDecider.getParseLocation(),
                                                myLastIndex);
                            }
                            
                            return indexes.get(myLastIndex)
                                    .assertIsNumber(
                                            myDecider.getParseLocation())
                                    .assertNonNegativeReasonableInteger(
                                            myDecider.getParseLocation(),
                                            "decide index sequence elmeent");
                        }
                    };
        }
        else {
            p = Picker.Util.buildStandardPicker(new Scedel.Decider() {
                        private final Random myRandom =
                                new Random(deciderValue.hashCode());
                
                        @Override
                        public boolean decide(double chance) {
                            return myRandom.nextDouble() < chance;
                        }
                    });
        }
        
        s.pushPicker(p);
        
        try {
            myCode.execute(h, s);
        }
        finally {
            s.popPicker();
        }
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT DECIDE\n");
        Util.labeledChild(indentUnit, indentLevels, "decider:", myDecider, b);
        Util.labeledChild(indentUnit, indentLevels, "code:", myCode, b);
    }

    @Override
    public void accept(Visitor v) {
        v.visitDecideStatement(this);
    }
}
