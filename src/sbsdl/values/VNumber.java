package sbsdl.values;

import java.math.BigInteger;
import java.util.Objects;
import sbsdl.Sbsdl;

public class VNumber extends SkeletonValue {
    public static VNumber of(long numerator, long denominator) {
        return new VNumber(numerator, denominator);
    }
    
    public static VNumber of(BigInteger numerator, BigInteger denominator) {
        return new VNumber(numerator, denominator);
    }
    
    private final BigInteger myNumerator;
    private final BigInteger myDenominator;
    
    private VNumber(long numerator, long denominator) {
        this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }
    
    private VNumber(BigInteger numerator, BigInteger denominator) {
        if (denominator.equals(BigInteger.ZERO)) {
            throw new Sbsdl.ExecutionException("Division by zero.");
        }
        
        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
        
        myNumerator = numerator;
        myDenominator = denominator;
    }
    
    public int assertNonNegativeReasonableInteger() {
        if (!myDenominator.equals(BigInteger.ONE)) {
            throw new Sbsdl.ExecutionException("Not an integer.");
        }
        
        if (myNumerator.compareTo(BigInteger.ZERO) < 0) {
            throw new Sbsdl.ExecutionException("Cannot be negative.");
        }
        
        if (myNumerator.compareTo(BigInteger.valueOf(1_000_000_000)) > 0) {
            throw new Sbsdl.ExecutionException("Bigger than 1,000,000,000.");
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
    
    public VNumber divide(VNumber o) {
        return multiply(VNumber.of(o.getDenominator(), o.getNumerator()));
    }
    
    public VNumber raiseTo(VNumber o) {
        if (!o.getDenominator().equals(BigInteger.ONE)) {
            throw new Sbsdl.ExecutionException("Non-integral exponent: " + o);
        }
        else if (o.getNumerator().compareTo(
                BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new Sbsdl.ExecutionException("Exponent overflow: " + o);
        }
        
        VNumber result;
        int intExp = o.getNumerator().intValue();
        if (intExp >= 0) {
            result = VNumber.of(myNumerator.pow(intExp), myDenominator);
        }
        else {
            result = inverse().raiseTo(o.multiply(-1));
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
    public VNumber assertIsNumber() {
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

    @Override
    public Value copy() {
        return this;
    }
}
