package com.shieldsbetter.scedel;

import java.util.Iterator;

public class Utils {
    public static <T> boolean elementsConformant(Iterable<? extends T> i1,
            Iterable<? extends T> i2, BinaryPredicate<T> c) {
        Iterator<? extends T> i1Iter = i1.iterator();
        Iterator<? extends T> i2Iter = i2.iterator();
        
        boolean result = true;
        while (result && i1Iter.hasNext()) {
            result = i2Iter.hasNext();
            if (result) {
                T t1 = i1Iter.next();
                T t2 = i2Iter.next();
                
                result = c.satisfy(t1, t2);
            }
        }
        
        if (result) {
            result = !i2Iter.hasNext();
        }
        
        return result;
    }
    
    public static interface BinaryPredicate<T> {
        public boolean satisfy(T t1, T t2);
    }
}
