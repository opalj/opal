/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class StaticField_mutation_5 {

    // A static field
    private static int staticValue;

    public static void main(String[] args) {
        // Set the static field
        setStaticValue(42);

        // Get the static field value and print it
        System.out.println("Static field value: " + getStaticValue());
    }

    // Method to set the static field
    public static void setStaticValue(int value) {
        calculateStaticValue(value);
        staticValue = value;
    }

    // Method to calculate and set the static field value
    public static void calculateStaticValue(int value) {
        value = value * 2;
    }

    // Method to get the static field value
    public static int getStaticValue() {
        return staticValue;
    }
}