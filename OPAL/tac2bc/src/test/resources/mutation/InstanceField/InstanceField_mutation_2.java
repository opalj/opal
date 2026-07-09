/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InstanceField_mutation_2 {

    private int instanceValue;

    public InstanceField_mutation_2(int value) {
        this.instanceValue = value;
    }

    public static void main(String[] args) {
        InstanceField_mutation_2 testInstance = new InstanceField_mutation_2(42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }
}
