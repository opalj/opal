/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class Array {

    public static void main(String[] args) {
        // Single-Dimensional Array
        int[] singleArray = new int[5];

        // Assign values to the single-dimensional array
        singleArray[0] = 10;
        singleArray[1] = 20;
        singleArray[2] = 30;
        singleArray[3] = 40;
        singleArray[4] = 50;

        // Access and print the values in the single-dimensional array
        System.out.println("Single Array Elements:");
        for (int i = 0; i < singleArray.length; i++) {
            System.out.println("Element at index " + i + ": " + singleArray[i]);
        }

        // Multi-Dimensional Array
        int[][] multiArray = new int[2][3];

        // Assign values to the multi-dimensional array
        multiArray[0][0] = 1;
        multiArray[0][1] = 2;
        multiArray[0][2] = 3;
        multiArray[1][0] = 4;
        multiArray[1][1] = 5;
        multiArray[1][2] = 6;

        // Access and print the values in the multi-dimensional array
        System.out.println("\nMulti-Dimensional Array Elements:");
        for (int i = 0; i < multiArray.length; i++) {
            for (int j = 0; j < multiArray[i].length; j++) {
                System.out.println("Element at index [" + i + "][" + j + "]: " + multiArray[i][j]);
            }
        }

        // Array of Strings
        String[] stringArray = {"Apple", "Banana", "Cherry"};

        // Access and print the values in the string array
        System.out.println("\nString Array Elements:");
        for (int i = 0; i < stringArray.length; i++) {
            System.out.println("Element at index " + i + ": " + stringArray[i]);
        }

        // Array Length Test
        System.out.println("\nArray Lengths:");
        System.out.println("Length of singleArray: " + singleArray.length);
        System.out.println("Length of multiArray: " + multiArray.length);
        System.out.println("Length of stringArray: " + stringArray.length);

        // Modify and reprint the single-dimensional array
        singleArray[2] = 100;
        System.out.println("\nModified Single Array Elements:");
        for (int i = 0; i < singleArray.length; i++) {
            System.out.println("Element at index " + i + ": " + singleArray[i]);
        }
    }
}
