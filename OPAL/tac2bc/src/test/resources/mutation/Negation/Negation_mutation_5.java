/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Negation_mutation_5 {

    public static void main(String[] args) {
        // Integer negation
        int intValue = 42;
        if (true) {
            int negatedInt = -intValue;
        }
        System.out.println("Negated int: " + intValue);

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
}