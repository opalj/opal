public class Jsr_mutation_1 {

    public static void main(String[] args) {
        System.out.println("Before subroutine");
        int[] arr = {3, 6};
        executeSubroutine(arr);
        System.out.println("After subroutine");
    }

    private static void executeSubroutine(int[] arr) {
        System.out.println("In subroutine");
        // Simulating a subroutine with a loop or some operations
        for (int i = arr[0]; i < arr[1]; i++) {
            System.out.println("Subroutine iteration: " + i);
        }
        // Return from the subroutine (RET equivalent)
        return; // RET is simulated by this return statement in Java
    }
}