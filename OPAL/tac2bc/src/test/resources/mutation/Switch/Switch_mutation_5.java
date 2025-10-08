/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Switch_mutation_5 {

    public static void main(String[] args) {
        // Array of fruits to ensure all cases are covered
        String[] fruits = {"Banana", "Brocoli", "Wednesday"};

        // Loop through all possible fruits
        for (String fruit : fruits) {
            printFruit(fruit);
        }
    }

    public static void printFruit(String fruit) {
        if(isValidFruit(fruit)) {
            switch (fruit) {
                case "Banana":
                    System.out.println(fruit + " is a fruit");
                    break;
                case "Brocoli":
                    System.out.println(fruit + " is a vegetable.");
                    break;
                default:
                    System.out.println(fruit + " is not valid.");
                    break;
            }
        } else {
            System.out.println(fruit + " is not valid.");
        }
    }

    public static boolean isValidFruit(String fruit) {
        switch (fruit) {
            case "Banana":
            case "Brocoli":
                return true;
            default:
                return false;
        }
    }
}