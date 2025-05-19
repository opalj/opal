/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InstanceField_mutation_5 {

    private Integer instanceValue;

    public static void main(String[] args) {
        InstanceField_mutation_5 testInstance = new InstanceField_mutation_5();
        testInstance.instanceValue = Integer.valueOf(42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }
}
