import java.util.List;
import java.util.ArrayList;
public class InvokeInterface_mutation_5 {

    public static void main(String[] args) {
        // Create an instance of ArrayList, which implements the List interface
        List<String> myList = new ArrayList<>();

        // Create a class to hold the list size
        class SizeHolder {
            static int listSize = 0;
        }

        // Add elements to the list
        myList.add("Hello");
        myList.add("World");
        SizeHolder.listSize = myList.size();

        // Get the size of the list from the SizeHolder class
        int size = SizeHolder.listSize;
        System.out.println("Size of the list: " + size);
    }
}