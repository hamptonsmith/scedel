package com.shieldsbetter.scedel;

import com.shieldsbetter.scedel.values.Value;
import java.util.List;

public interface Picker {
    
    /**
     * <p>Returns an index into {@code options} indicating the picked
     * element.</p>
     * 
     * @param options A list of {@link Option}s.  The structure of this list and
     *              the value of its elements must not be changed.
     * @param totalWeight The sum of all weights in {@code options}, as a
     *              convenience.
     * @return An index into {@code options}.
     * 
     * @throws com.shieldsbetter.scedel.Picker.CannotPickException If for any
     *               reason, this picker can't or won't pick an element.
     */
    public int pick(List<Option> options, int totalWeight)
            throws CannotPickException;
    
    public static class Util {
        public static Picker buildStandardPicker(final Scedel.Decider d) {
            return new Picker() {
                        @Override
                        public int pick(List<Option> options, int totalWeight)
                                throws CannotPickException {
                            if (totalWeight == 0) {
                                throw CannotPickException.INSTANCE;
                            }
                            
                            int chosenIndex = -1;
                            double remainingChance = 1.0;

                            int i = 0;
                            for (Option o : options) {
                                if (o.getWeight() > 0
                                        && d.decide(remainingChance)) {
                                    chosenIndex = i;
                                }

                                remainingChance -=
                                        o.getWeight() / (double) totalWeight;
                            
                                i++;
                            }

                            if (chosenIndex == -1) {
                                throw new RuntimeException();
                            }
                            
                            return chosenIndex;
                        }
                    };
        }
    }
    
    public static final class CannotPickException extends Exception {
        public static final CannotPickException INSTANCE =
                new CannotPickException();
        private CannotPickException() {}
    }
    
    public static final class Option {
        private final Value myValue;
        private int myWeight;
        
        public Option(Value v, int w) {
            myValue = v;
            myWeight = w;
        }
        
        public Value getValue() {
            return myValue;
        }
        
        public int getWeight() {
            return myWeight;
        }
        
        public void zeroWeight() {
            myWeight = 0;
        }
    }
}
