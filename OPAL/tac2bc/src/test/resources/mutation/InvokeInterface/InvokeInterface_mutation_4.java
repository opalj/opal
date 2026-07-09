/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import java.util.List;
import java.util.ArrayList;

public class InvokeInterface_mutation_4 {

    public static void main(String[] args) {
        // Create an instance of ArrayList, which implements the List interface
        List<String> myList = new ArrayList<>();

        // Create an array of elements to be added to the list
        String[] elements = {"Hello", "World"};

        // Add elements to the list (invokeinterface)
        for (String element : elements) {
            myList.add(element);
        }

        // Get the size of the list (invokeinterface)
        int[] sizeArray = {myList.size()};
        System.out.println("Size of the list: " + sizeArray[0]);
    }
}