public class StaticField {

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
        staticValue = value;
    }

    // Method to get the static field value
    public static int getStaticValue() {
        return staticValue;
    }
}
