/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class StaticField_mutation_2 {

    // A static field
    private static int staticValue;

    public static void main(String[] args) {
        // Set the static field
        StaticField_mutation_2.staticValue = 42; // replaced direct assignment with assignment through a static field

        // Get the static field value and print it
        System.out.println("Static field value: " + getStaticValue());
    }

    // Method to set the static field
    public static void setStaticValue(int value) {
        staticValue = value;
    }

    // Method to get the static field value
    public static int getStaticValue() {
        return staticValue;
    }
}