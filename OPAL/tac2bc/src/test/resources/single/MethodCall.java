/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class MethodCall {

    public static void main(String[] args) {
        // Instance method call
        MethodCall instance = new MethodCall();
        int instanceResult = instance.instanceMethod(5, 3);
        System.out.println("Instance method result: " + instanceResult);

        // Static method call
        int staticResult = staticMethod(10, 2);
        System.out.println("Static method result: " + staticResult);

        // Overloaded method call
        String overloadedResult1 = instance.overloadedMethod(5);
        String overloadedResult2 = instance.overloadedMethod(5, 3);
        System.out.println("Overloaded method result (1 parameter): " + overloadedResult1);
        System.out.println("Overloaded method result (2 parameters): " + overloadedResult2);

        // Calling a method that calls another method
        int nestedCallResult = instance.nestedMethodCall(7, 3);
        System.out.println("Nested method call result: " + nestedCallResult);

        // Method with multiple return statements
        int multipleReturnResult = instance.multipleReturnMethod(15);
        System.out.println("Multiple return method result: " + multipleReturnResult);
    }

    // Instance method
    public int instanceMethod(int a, int b) {
        return a + b;
    }

    // Static method
    public static int staticMethod(int x, int y) {
        return x * y;
    }

    // Overloaded methods
    public String overloadedMethod(int a) {
        return "Overloaded with one parameter: " + a;
    }

    public String overloadedMethod(int a, int b) {
        return "Overloaded with two parameters: " + (a + b);
    }

    // Method that calls another method
    public int nestedMethodCall(int a, int b) {
        return instanceMethod(a, b) * staticMethod(a, b);
    }

    // Method with multiple return statements
    public int multipleReturnMethod(int value) {
        if (value > 10) {
            return value * 2;
        } else if (value == 10) {
            return value * 3;
        } else {
            return value * 4;
        }
    }
}
