/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import java.util.List;
import java.util.ArrayList;

public class InvokeInterface_mutation_2 {

    public static void main(String[] args) {
        // Create an instance of ArrayList, which implements the List interface
        List<String> myList = new ArrayList<>();

        // Add elements to the list
        String[] arr = {"Hello"};
        myList.add(arr[0]);

        // Add elements to the list
        String[] arr2 = {"World"};
        myList.add(arr2[0]);

        // Get the size of the list
        int[] sizeArr = {myList.size()};
        System.out.println("Size of the list: " + sizeArr[0]);
    }
}