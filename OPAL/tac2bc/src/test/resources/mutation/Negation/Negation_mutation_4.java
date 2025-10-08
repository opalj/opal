/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Negation_mutation_4 {

    private static int INT_FIELD = 42;
    private static long LONG_FIELD = 123456789L;
    private static float FLOAT_FIELD = 3.14f;
    private static double DOUBLE_FIELD = 2.71828;

    public static void main(String[] args) {
        // Integer negation
        int intValue = INT_FIELD;
        int negatedInt = -intValue;
        System.out.println("Negated int: " + negatedInt);

        // Long negation
        long longValue = LONG_FIELD;
        long negatedLong = -longValue;
        System.out.println("Negated long: " + negatedLong);

        // Float negation
        float floatValue = FLOAT_FIELD;
        float negatedFloat = -floatValue;
        System.out.println("Negated float: " + negatedFloat);

        // Double negation
        double doubleValue = DOUBLE_FIELD;
        double negatedDouble = -doubleValue;
        System.out.println("Negated double: " + negatedDouble);
    }
}