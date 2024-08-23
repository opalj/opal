public class InstanceField_mutation_3 {

    private int instanceValue;

    public static void main(String[] args) {
        InstanceField testInstance = new InstanceField();
        testInstance.assignValue(42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }

    public void assignValue(int value) {
        this.instanceValue = value;
    }
}
