/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Jsr_mutation_3 {

    public static void main(String[] args) {
        System.out.println("Before subroutine");
        executeSubroutine();
        System.out.println("After subroutine");
    }

    private static void executeSubroutine() {
        System.out.println("In subroutine");
        for (int i = 0; i < 3; i++) {
            printIteration(i);
            System.out.println("Subroutine iteration: " + i);
        }
        // Return from the subroutine (RET equivalent)
        return; // RET is simulated by this return statement in Java
    }

    private static void printIteration(int i) {
        System.out.println("Subroutine iteration: " + i);
    }
}