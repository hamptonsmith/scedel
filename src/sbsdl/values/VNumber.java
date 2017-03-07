package sbsdl.values;

import java.math.BigInteger;

public class VNumber implements Value {
    private BigInteger myNumerator;
    private BigInteger myDenominator;
    
    public VNumber(long numerator, long denominator) {
        myNumerator = BigInteger.valueOf(numerator);
        myDenominator = BigInteger.valueOf(denominator);
        
        reduce();
    }
    
    public void multiply(long f) {
        myNumerator = myNumerator.multiply(BigInteger.valueOf(f));
        reduce();
    }
    
    public void divide(long d) {
        myDenominator = myDenominator.multiply(BigInteger.valueOf(d));
        reduce();
    }
    
    public void add(long o) {
        BigInteger oNumerator = BigInteger.valueOf(o).multiply(myDenominator);
        myNumerator = myNumerator.add(oNumerator);
        reduce();
    }
    
    public BigInteger getDenominator() {
        return myDenominator;
    }
    
    public BigInteger getNumerator() {
        return myNumerator;
    }
    
    private void reduce() {
        BigInteger gcd = myNumerator.gcd(myDenominator);
        myNumerator = myNumerator.divide(gcd);
        myDenominator = myDenominator.divide(gcd);
    }
}
