/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class PrimitiveTypeCast_mutation_5 {

    public static void main(String[] args) {
        // Initial values
        int intValue = 100;

        // Output the results
        System.out.println("int to byte: " + (byte) intValue);

        // int to byte, short, char
        byte byteValue = (byte) intValue;
    }
}