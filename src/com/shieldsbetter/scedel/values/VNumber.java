package com.shieldsbetter.scedel.values;

import java.math.BigInteger;
import java.util.Objects;
import com.shieldsbetter.scedel.InternalExecutionException;
import com.shieldsbetter.scedel.ParseLocation;

public class VNumber extends ImmutableValue<VNumber> {
    public static VNumber of(long numerator, long denominator) {
        return new VNumber(numerator, denominator);
    }
    
    private static VNumber of(BigInteger numerator, BigInteger denominator) {
        return new VNumber(numerator, denominator);
    }
    
    private final BigInteger myNumerator;
    private final BigInteger myDenominator;
    
    private VNumber(long numerator, long denominator) {
        this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }
    
    private VNumber(BigInteger numerator, BigInteger denominator) {
        if (denominator.equals(BigInteger.ZERO)) {
            throw new RuntimeException();
        }
        
        BigInteger gcd = numerator.gcd(denominator);
        
        if (denominator.compareTo(BigInteger.ZERO) < 0) {
            denominator = denominator.multiply(BigInteger.valueOf(-1));
        }
        
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
        
        myNumerator = numerator;
        myDenominator = denominator;
    }
    
    public boolean equals(int i) {
        return myDenominator.equals(BigInteger.ONE)
                && myNumerator.compareTo(BigInteger.valueOf(i)) == 0;
    }
    
    public int assertNonNegativeReasonableInteger(
            ParseLocation loc, String what) {
        if (!myDenominator.equals(BigInteger.ONE)) {
            throw InternalExecutionException.nonIntegral(what, loc, this);
        }
        
        if (myNumerator.compareTo(BigInteger.ZERO) < 0) {
            throw InternalExecutionException.negative(what, loc, this);
        }
        
        if (myNumerator.compareTo(BigInteger.valueOf(1_000_000_000)) > 0) {
            throw InternalExecutionException.tooLarge(what, loc, this);
        }
        
        return myNumerator.intValue();
    }
    
    public VNumber multiply(long f) {
        return VNumber.of(myNumerator.multiply(BigInteger.valueOf(f)),
                myDenominator);
    }
    
    public VNumber divide(long d) {
        return VNumber.of(
                myNumerator, myDenominator.multiply(BigInteger.valueOf(d)));
    }
    
    public VNumber add(long o) {
        BigInteger oNumerator = BigInteger.valueOf(o).multiply(myDenominator);
        return VNumber.of(myNumerator.add(oNumerator), myDenominator);
    }
    
    public VNumber add(VNumber o) {
        BigInteger newDenominator = myDenominator.multiply(o.getDenominator());
        
        return VNumber.of(
                myNumerator.multiply(o.getDenominator())
                        .add(o.getNumerator().multiply(myDenominator)),
                newDenominator);
    }
    
    public VNumber subtract(VNumber o) {
        BigInteger newDenominator = myDenominator.multiply(o.getDenominator());
        
        return VNumber.of(
                myNumerator.multiply(o.getDenominator())
                        .subtract(o.getNumerator().multiply(myDenominator)),
                newDenominator);
    }
    
    public VNumber multiply(VNumber o) {
        return VNumber.of(myNumerator.multiply(o.getNumerator()),
                myDenominator.multiply(o.getDenominator()));
    }
    
    public VNumber divide(VNumber o, ParseLocation oLoc) {
        if (o.equals(0)) {
            throw InternalExecutionException.divisionByZero(oLoc);
        }
        
        return multiply(VNumber.of(o.getDenominator(), o.getNumerator()));
    }
    
    public VNumber raiseTo(VNumber o, ParseLocation oLoc) {
        if (!o.getDenominator().equals(BigInteger.ONE)) {
            throw InternalExecutionException.nonIntegral("exponent", oLoc, o);
        }
        else if (o.getNumerator().compareTo(
                BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw InternalExecutionException.tooLarge("exponent", oLoc, o);
        }
        
        VNumber result;
        int intExp = o.getNumerator().intValue();
        if (intExp >= 0) {
            result = VNumber.of(myNumerator.pow(intExp), myDenominator);
        }
        else {
            result = inverse().raiseTo(o.multiply(-1), oLoc);
        }
        
        return result;
    }
    
    public int compareTo(VNumber o) {
        BigInteger thisScaledNum = myNumerator.multiply(o.getDenominator());
        BigInteger oScaledNum = o.getNumerator().multiply(myDenominator);
        
        return thisScaledNum.compareTo(oScaledNum);
    }
    
    public VNumber inverse() {
        return VNumber.of(myDenominator, myNumerator);
    }
    
    public BigInteger getDenominator() {
        return myDenominator;
    }
    
    public BigInteger getNumerator() {
        return myNumerator;
    }

    @Override
    public VNumber assertIsNumber(ParseLocation at) {
        return this;
    }
    
    @Override
    public String toString() {
        String result = "" + myNumerator;
        
        if (!myDenominator.equals(BigInteger.ONE)) {
            result += " / " + myDenominator;
            result = "(" + result + ")";
        }
        
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VNumber)) {
            return false;
        }
        
        if (o == this) {
            return true;
        }
        
        VNumber oAsVn = (VNumber) o;
        return myNumerator.equals(oAsVn.getNumerator())
                && myDenominator.equals(oAsVn.getDenominator());
    }

    @Override
    public int hashCode() {
        return Objects.hash(myNumerator, myDenominator);
    }
}
