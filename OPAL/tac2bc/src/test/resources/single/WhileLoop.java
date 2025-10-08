/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class WhileLoop {

    public static void main(String[] args) {
        // Simple while loop
        int counter = 0;
        while (counter < 5) {
            System.out.println("Simple while loop: counter = " + counter);
            counter++;
        }

        // While loop with multiple conditions
        int a = 10;
        int b = 20;
        while (a < b && a % 2 == 0) {
            System.out.println("While loop with multiple conditions: a = " + a);
            a += 2;
        }

        // Nested while loop
        int outer = 1;
        while (outer <= 3) {
            int inner = 1;
            while (inner <= 2) {
                System.out.println("Nested while loop: outer = " + outer + ", inner = " + inner);
                inner++;
            }
            outer++;
        }

        // While loop with break
        int x = 0;
        while (true) {
            System.out.println("While loop with break: x = " + x);
            if (x >= 3) {
                break;
            }
            x++;
        }

        // While loop with continue
        int y = 0;
        while (y < 5) {
            y++;
            if (y % 2 == 0) {
                continue;
            }
            System.out.println("While loop with continue: y = " + y);
        }

        // Do-while loop
        int z = 0;
        do {
            System.out.println("Do-while loop: z = " + z);
            z++;
        } while (z < 3);

        // Do-while loop with complex condition
        int m = 10;
        do {
            System.out.println("Do-while loop with complex condition: m = " + m);
            m--;
        } while (m > 0 && m % 3 != 0);
    }
}
