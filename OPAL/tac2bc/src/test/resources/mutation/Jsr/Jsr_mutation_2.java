public class Jsr_mutation_2 {

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
        if (true) {
            return; // RET is simulated by this return statement in Java
        }
    }
}