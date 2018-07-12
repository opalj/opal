/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package metrics.fanin_fanout;

/**
 * @author Michael Reif
 */
public class ExpressionInterpreter {

    public static void main(String[] args){
        Expression expr = new AddExpression(new Constant(8), new Constant(34));
        System.out.println(expr.eval());
    }
}
