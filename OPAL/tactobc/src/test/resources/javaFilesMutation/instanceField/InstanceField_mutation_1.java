public class InstanceField_mutation_1 {

    private int instanceValue;

    public static void main(String[] args) {
        InstanceField testInstance = new InstanceField();
        int value = 42;
        testInstance.instanceValue = value;
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }
}
