/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Negation_mutation_3 {

    public static void main(String[] args) {
        // Integer negation
        int[] intArr = {42};
        int intValue = intArr[0];
        int negatedInt = -intValue;
        System.out.println("Negated int: " + negatedInt);

        // Long negation
        long longValue = 123456789L;
        long negatedLong = -longValue;
        System.out.println("Negated long: " + negatedLong);

        // Float negation
        float[] floatArr = {(float) 3.14};
        float floatValue = floatArr[0];
        float negatedFloat = -floatValue;
        System.out.println("Negated float: " + negatedFloat);

        // Double negation
        double[] doubleArr = {2.71828};
        double doubleValue = doubleArr[0];
        double negatedDouble = -doubleValue;
        System.out.println("Negated double: " + negatedDouble);
    }
}