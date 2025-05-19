/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Negation {

    public static void main(String[] args) {
        // Integer negation
        int intValue = 42;
        int negatedInt = -intValue;
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

        // Test negation in expressions
        int exprInt = -(intValue + 10);
        long exprLong = -(longValue * 2);
        float exprFloat = -(floatValue / 2.0f);
        double exprDouble = -(doubleValue - 1.0);

        System.out.println("Negated int in expression: " + exprInt);
        System.out.println("Negated long in expression: " + exprLong);
        System.out.println("Negated float in expression: " + exprFloat);
        System.out.println("Negated double in expression: " + exprDouble);
    }
}
