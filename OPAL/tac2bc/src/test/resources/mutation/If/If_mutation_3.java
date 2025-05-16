public class If_mutation_3 {
    public static void main(String[] args) {
        // Simple if statement
        int[] aArr = {10};
        int a = aArr[0];
        int b = 20;
        if (a < b) {
            System.out.println("a is less than b");
        }

        // If-else statement
        int[] cArr = {15};
        int c = cArr[0];
        if (a > c) {
            System.out.println("a is greater than c");
        } else {
            System.out.println("a is not greater than c");
        }

        // If-else if-else statement
        if (a < c) {
            System.out.println("a is less than c");
        } else if (b > c) {
            System.out.println("b is greater than c");
        } else {
            System.out.println("Neither condition is true");
        }

        // Nested if statement
        if (a < b) {
            if (c < b) {
                System.out.println("c is less than b");
            } else {
                System.out.println("c is not less than b");
            }
        }

        // Multiple conditions in if statement
        boolean x = true;
        boolean y = false;
        if (x && !y) {
            System.out.println("x is true and y is false");
        }

        // Object comparison using ==
        String str1 = "Hello";
        String str2 = "Hello";

        if (str1 == str2) {
            System.out.println("str1 == str2: Both references are the same");
        }

        // Object comparison using equals()
        String s1 = new String("test");
        String s2 = new String("test");

        if (s1.equals(s2)) {
            System.out.println("s1 equals s2: Both strings have the same value");
        } else {
            System.out.println("s1 does not equal s2");
        }
    }
}