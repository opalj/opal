/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class CheckCast_mutation_5 {

    public static void main(String[] args) {
        // Test case 1: Valid cast
        Object obj = "This is a string";
        System.out.println("Successfully casted to String: " + (String) obj);

        // Test case 2: Null reference casting
        Object nullObj = null;
        String nullCast = (String) nullObj; // Valid CHECKCAST, null can be cast to any reference type
        System.out.println("Successfully casted null reference to String");

        // Test case 3: Casting within the same type
        Object anotherString = "Another string";
        String sameTypeCast = (String) anotherString; // Valid CHECKCAST
        System.out.println("Successfully casted to String: " + sameTypeCast);
    }
}