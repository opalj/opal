import java.util.List;
import java.util.ArrayList;

public class InvokeInterface_mutation_1 {

    public static void main(String[] args) {
        // Create an instance of ArrayList, which implements the List interface
        List<String> myList = new ArrayList<>();

        // Add elements to the list (invokeinterface)
        myList.add("Hello");
        myList.add("World");

        // Get the size of the list (invokeinterface)
        int size = getSize(myList);
        System.out.println("Size of the list: " + size);
    }

    public static int getSize(List<String> myList) {
        return myList.size();
    }
}