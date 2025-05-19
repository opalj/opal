/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class StaticField_mutation_4 {

    // A static field
    private static int staticValue;

    public static void main(String[] args) {
        // Set the static field
        setStaticValue(42);

        // Print the static field value
        printStaticFieldValue();
    }

    // Method to set the static field
    public static void setStaticValue(int value) {
        staticValue = value;
    }

    // Method to get the static field value
    public static int getStaticValue() {
        return staticValue;
    }

    // Method to print the static field value
    public static void printStaticFieldValue() {
        System.out.println("Static field value: " + getStaticValue());
    }
}