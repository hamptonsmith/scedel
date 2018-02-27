package com.shieldsbetter.scedel.values;

import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.expressions.Expression;

public interface Value<C extends Value> extends Expression {
    public VNumber assertIsNumber(ParseLocation at);
    public VBoolean assertIsBoolean(ParseLocation at);
    public VDict assertIsDict(ParseLocation at);
    public VSeq assertIsSeq(ParseLocation at);
    public VFunction assertIsFunction(ParseLocation at);
    public VString assertIsString(ParseLocation at);
    
    /**
     * <p>Creates a deep copy of this {@code Value} with the exception of any
     * {@link VProxy} nodes, which cannot be deep copied, and will instead be
     * preserved as references to their corresponding game objects.</p>
     * 
     * <p>If and only if {@code errorOnProxy} is {@code true}, will throw a
     * {@link CannotCopyVProxyException} upon encountering a {@code VProxy}
     * node.</p>
     * 
     * @param errorOnProxy Whether or not to throw a
     *              {@code CannotCopyVProxyException} upon encountering a
     *              {@code VProxy}.
     * 
     * @return A deep copy of this {@code Value}.
     */
    public C copy(boolean errorOnProxy);
    
    public String getValueString();
    
    public static class CannotCopyVProxyException extends RuntimeException {
    }
}
