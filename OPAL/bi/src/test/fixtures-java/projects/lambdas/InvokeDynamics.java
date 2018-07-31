/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import java.util.concurrent.Callable;

/**
 * Simple lambda add test with main method
 */
public class InvokeDynamics {

    public int simpleLambdaAdd(int x, int y) throws Exception {
        Callable<Integer> c = () -> x+y;

        return c.call();
    }

    public static void main(String[] args) {
        InvokeDynamics id = new InvokeDynamics();
        int result = 0;
        try {
            result = id.simpleLambdaAdd(2, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Result is " + result);
    }
}
