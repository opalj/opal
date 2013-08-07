package apps;

import java.util.Stack;
import java.util.StringTokenizer;

public class RPNCalculator {

    // Things that we should be able to determine (at some point...)
    // 1.1) If expression is null, we will get a NullPointerException
    // 1.2) If expression is not null, we will not get a NullPointerException
    // 2) This method does not have any observable side effect (it is thread safe)
    // 3) The returned value is in the range Int.MinValue..Int.MaxValue
    // 4) We will never get an EmptyStackException
    
    @SuppressWarnings("boxing")
    public static int evaluate(String expression) {
        Stack<Integer> stack = new Stack<Integer>();

        StringTokenizer stringTokenizer = new StringTokenizer(expression);
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();

            if (token.matches("\\d+")) {
                stack.push(new Integer(token));
            } else {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Missing operands for operator: " + token);
                }
                int r = stack.pop();
                int l = stack.pop();
                int t;
                if (token.equals("+")) {
                    t = l + r;
                } else if (token.equals("-")) {
                    t = l - r;
                } else if (token.equals("*")) {
                    t = l * r;
                } else if (token.equals("/")) {
                    t = l / r;
                } else {
                    throw new IllegalArgumentException("Unknown operator: " + token);
                }
                stack.push(t);
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Too many operands.");
        }

        // the result is on top of the stack
        return stack.pop();
    }

    public static void main(String[] args) {
        String expression = "";
        for (String arg : args) {
            expression = expression + " " + arg;
        }
        System.out.println(evaluate(expression.trim()));
    }
}
