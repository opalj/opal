/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InstanceField_mutation_1 {

    private int instanceValue;

    public static void main(String[] args) {
        InstanceField_mutation_1 testInstance = new InstanceField_mutation_1();
        int value = 42;
        testInstance.instanceValue = value;
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }
}
