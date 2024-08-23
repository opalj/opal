public class InstanceField_mutation_4 {

    private int instanceValue;

    public static void main(String[] args) {
        InstanceField testInstance = new InstanceField();
        initializeField(testInstance, 42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }

    private static void initializeField(InstanceField instance, int value) {
        instance.instanceValue = value;
    }
}
