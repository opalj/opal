/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class If {

    public static void main(String[] args) {
        // Test simple if statement with a loop
        int a = 10;
        int b = 20;
        for (int i = 0; i < 5; i++) {
            if (a + i < b) {
                System.out.println("Iteration " + i + ": a + i is less than b");
            }
        }

        // Test if-else statement with a loop
        for (int i = 5; i > 0; i--) {
            if (a * i > b) {
                System.out.println("Iteration " + i + ": a * i is greater than b");
            } else {
                System.out.println("Iteration " + i + ": a * i is not greater than b");
            }
        }

        // Test if-else if-else statement with a loop
        int c = 15;
        for (int i = 0; i < 5; i++) {
            if (a + i > c) {
                System.out.println("Iteration " + i + ": a + i is greater than c");
            } else if (b - i > c) {
                System.out.println("Iteration " + i + ": b - i is greater than c");
            } else {
                System.out.println("Iteration " + i + ": c is greater than or equal to both a + i and b - i");
            }
        }

        // Test nested if statement with a loop
        for (int i = 0; i < 5; i++) {
            if (a < b) {
                if (c + i < b) {
                    System.out.println("Iteration " + i + ": c + i is less than b");
                } else {
                    System.out.println("Iteration " + i + ": c + i is greater than or equal to b");
                }
            }
        }

        // Test if with multiple conditions inside a loop
        boolean x = true;
        boolean y = false;
        for (int i = 0; i < 5; i++) {
            if (x && !y && i % 2 == 0) {
                System.out.println("Iteration " + i + ": x is true, y is false, and i is even");
            }
        }

        // Test if with variables being reassigned inside a loop
        int d = 5;
        for (int i = 0; i < 3; i++) {
            if (d == 5 + i) {
                d = 10 + i;
                System.out.println("Iteration " + i + ": d was " + (5 + i) + ", now it's " + d);
            }
        }
        if (d == 12) {
            System.out.println("After the loop, d is now 12");
        }

        // Object comparison using == (reference comparison)
        String str1 = "Hello";
        String str2 = "Hello";

        if (str1 == str2) {
            System.out.println("str1 == str2: Both references are the same");
        }

        String s1 = new String("test");
        String s2 = new String("test");

        if (s1.equals(s2)) {
            System.out.println("Equal strings");
        } else {
            System.out.println("Not equal strings");
        }
    }
}
