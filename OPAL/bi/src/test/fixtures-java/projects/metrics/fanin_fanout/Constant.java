/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package metrics.fanin_fanout;

/**
 * @author Michael Reif
 */
public class Constant implements Expression {

    private final Integer value;

    public Constant(Integer value){
        this.value = value;
    }

    @Override
    public int eval() {
        return value;
    }
}
