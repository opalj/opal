/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Negation_mutation_2 {

    public static void main(String[] args) {
        // Integer negation
        int intValue = 42;
        int negatedInt = negate(intValue);
        System.out.println("Negated int: " + negatedInt);

        // Long negation
        long longValue = 123456789L;
        long negatedLong = -longValue;
        System.out.println("Negated long: " + negatedLong);

        // Float negation
        float floatValue = 3.14f;
        float negatedFloat = -floatValue;
        System.out.println("Negated float: " + negatedFloat);

        // Double negation
        double doubleValue = 2.71828;
        double negatedDouble = -doubleValue;
        System.out.println("Negated double: " + negatedDouble);
    }

    public static int negate(int value) {
        return -value;
    }
}