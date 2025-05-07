import java.util.List;
import java.util.ArrayList;
public class InvokeInterface_mutation_3 {

    public static void main(String[] args) {
        // Create an instance of ArrayList, which implements the List interface
        List<String> myList = new ArrayList<>();

        // Add elements to the list (invokeinterface)
        myList.add("Hello");
        myList.add("World");

        // Calculate the size of the list
        int size = myList.size();
        int calculatedSize = size; // Store the result in a temporary variable

        // Get the size of the list (invokeinterface)
        System.out.println("Size of the list: " + calculatedSize);

        // Print the list
        System.out.println("Elements of the list: " + myList);
    }
}