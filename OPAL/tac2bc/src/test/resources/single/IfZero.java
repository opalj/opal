/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class IfZero {

    public static void main(String[] args) {
        IfZero test = new IfZero();

        // Test with a positive number, zero, and a negative number
        int[] testValues = {10, 0, -5};

        for (int value : testValues) {
            System.out.println("Testing with k = " + value);
            test.compareWithZero(value);
            System.out.println();
        }
    }

    public void compareWithZero(int k) {
        // Equal to zero
        if (k == 0) {
            System.out.println("k is equal to zero.");
        } else {
            System.out.println("k is not equal to zero.");
        }

        // Not equal to zero
        if (k != 0) {
            System.out.println("k is not equal to zero.");
        } else {
            System.out.println("k is equal to zero.");
        }

        // Less than zero
        if (k < 0) {
            System.out.println("k is less than zero.");
        } else {
            System.out.println("k is not less than zero.");
        }

        // Greater than zero
        if (k > 0) {
            System.out.println("k is greater than zero.");
        } else {
            System.out.println("k is not greater than zero.");
        }

        // Less than or equal to zero
        if (k <= 0) {
            System.out.println("k is less than or equal to zero.");
        } else {
            System.out.println("k is greater than zero.");
        }

        // Greater than or equal to zero
        if (k >= 0) {
            System.out.println("k is greater than or equal to zero.");
        } else {
            System.out.println("k is less than zero.");
        }
    }
}
