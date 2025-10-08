/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

import java.util.List;
import java.util.ArrayList;

public class InvokeInterface {

    public static void main(String[] args) {
        // Create an instance of ArrayList, which implements the List interface
        List<String> myList = new ArrayList<>();

        // Add elements to the list (invokeinterface)
        myList.add("Hello");
        myList.add("World");

        // Get the size of the list (invokeinterface)
        int size = myList.size();
        System.out.println("Size of the list: " + size);
    }
}