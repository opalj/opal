public class InstanceField_mutation_4 {

    private int instanceValue;

    public static void main(String[] args) {
        InstanceField_mutation_4 testInstance = new InstanceField_mutation_4();
        initializeField(testInstance, 42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }

    private static void initializeField(InstanceField_mutation_4 instance, int value) {
        instance.instanceValue = value;
    }
}
