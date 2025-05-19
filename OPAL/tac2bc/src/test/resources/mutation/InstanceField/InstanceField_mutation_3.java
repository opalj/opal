/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InstanceField_mutation_3 {

    private int instanceValue;

    public static void main(String[] args) {
        InstanceField_mutation_3 testInstance = new InstanceField_mutation_3();
        testInstance.assignValue(42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }

    public void assignValue(int value) {
        this.instanceValue = value;
    }
}
