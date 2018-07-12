/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package metrics.fanin_fanout;

/**
 * @author Michael Reif
 */
public class AddExpression implements Expression {

    private final Expression left;
    private final Expression right;

    public AddExpression(Expression left, Expression right){
        this.left = left;
        this.right = right;
    }

    public int eval() {
        return left.eval() + right.eval();
    }
}
