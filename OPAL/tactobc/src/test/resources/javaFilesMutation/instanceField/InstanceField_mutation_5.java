public class InstanceField_mutation_5 {

    private Integer instanceValue;

    public static void main(String[] args) {
        InstanceField testInstance = new InstanceField();
        testInstance.instanceValue = Integer.valueOf(42);
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }
}
