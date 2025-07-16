/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class Jsr {

    public static void main(String[] args) {
        System.out.println("Before subroutine");
        executeSubroutine();
        System.out.println("After subroutine");
    }

    private static void executeSubroutine() {
        System.out.println("In subroutine");
        // Simulating a subroutine with a loop or some operations
        for (int i = 0; i < 3; i++) {
            System.out.println("Subroutine iteration: " + i);
        }
        // Return from the subroutine (RET equivalent)
        return; // RET is simulated by this return statement in Java
    }
}
