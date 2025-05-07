public class Jsr_mutation_4 {

    public static void main(String[] args) {
        System.out.println("Before subroutine");
        System.out.println("After subroutine");
        executeSubroutine();
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