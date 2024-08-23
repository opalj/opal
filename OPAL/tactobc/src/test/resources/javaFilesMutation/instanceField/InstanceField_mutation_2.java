public class InstanceField_mutation_2 {

    private int instanceValue;

    public InstanceField(int value) {
        this.instanceValue = value;
    }

    public static void main(String[] args) {
        InstanceField testInstance = new InstanceField(42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }
}
