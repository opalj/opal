/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package strings;

/**
 * Represents a collection of java.lang.StringBuilder usages that focus on populating/manipulating
 * strings. It contains methods with rather simple usages as well as more complex usages that can
 * be found in real code (examples are minimized). Those methods can be used to evaluate string
 * tracking.
 *
 * Methods that can mutate the StringBuilder's value:
 *  - append
 *  - appendCodePoint
 *  - insert
 *  - delete
 *  - deleteCharAt
 *  - replace
 *  - reverse
 *  - constructors
 *
 * @author Michael Reif
 */
public class StringBuilderUsages {

    // Requires
    //  - requires an understanding of ".toCharArray" - a precise modeling of (constant) arrays
    //  - we need to know that INIT_CHAR_ARRAY is effectively not mutated/constant
    //  - (for some cases) we need to model simple alternatives/repetitions etc. =>
    //    *lightweight matchers*: "adsfasdf(XYZ|ZDY)" where () models an alternative.
    //  - we need to be able to "unroll loops" => loop detection and loop variables detection
    //    required
    //

    final public static String INIT_STR = "0123456789";
    final public static char[] INIT_CHAR_ARRAY = INIT_STR.toCharArray();

    /*
     * ############ CONSTRUCTORS ############
     */

    public static String stringConstructor(){
        StringBuilder sb = new StringBuilder(INIT_STR);
        return sb.toString();
    }

    public static String charSequenceConstructor(){
        StringBuilder sb = new StringBuilder(INIT_CHAR_ARRAY.toString());
        return sb.toString();
    }

    /*
     * ############ .append() - append is overloaded ############
     */

    /* A very common use case of a StringBuilder. */
    public static String sequentialAppend(){
        final StringBuilder sb = new StringBuilder();

        sb.append("List(");
        sb.append("1,2,3");
        sb.append(")");

        return sb.toString();
    }

    public static String alternatingAppend(){
        final StringBuilder sb = new StringBuilder();

        if(Math.random() > 0.5)
            sb.append("yes");
        else
            sb.append("no");

        return sb.toString();
    }

    public static String appendInLoop(){
        final StringBuilder sb = new StringBuilder();

        sb.append("List(");

        for(int i = 0; i < 3; i++){
            sb.append(i);
            sb.append(",");
        }

        return sb.toString();
    }

    public static String appendInLoopWithConditional(){
        final StringBuilder sb = new StringBuilder();

        sb.append("List(");

        for(int i = 0; i < 3; i++){
            sb.append(i);
            if(i < 2) sb.append(",");
        }

        return sb.toString();
    }

    /*
     * ############ DELETE / DELETE_CHAR_AT ############
     */

    public static String deleteAtIndex(){
        final StringBuilder sb = new StringBuilder("0123456789");

        sb.delete(8,9);
        sb.delete(3,4);

        return sb.toString();
    }

    public static String deleteWithMethodCalls(){
        final String s = ",";
        final StringBuilder sb = new StringBuilder("java/lang/Class ");
        sb.delete(sb.length() - s.length(), sb.length());

        return sb.toString();
    }

    public static String clearBufferWithDelete(){
        final StringBuilder sb = new StringBuilder("This is some String without any value.");
        sb.delete(0, sb.length());

        return sb.toString();
    }

    /*
     * ############ INSERTS (overloaded) ############
     */

    public static String prependString(){
        final StringBuilder sb = new StringBuilder("(1,2,3,4,5)");
        sb.insert(0, "List");
        sb.toString();

        return sb.toString();
    }

    public static String prependParamString(String prefix){
        final StringBuilder sb = new StringBuilder("(1,2,3,4,5)");
        sb.insert(0, prefix);
        sb.toString();

        return sb.toString();
    }

    public static String insertObject(){
        class Value {
            private int value;

            Value(int value){
                this.value = value;
            }

            public String toString(){
                return "Value("+value+")";
            }
        }

        final StringBuilder sb = new StringBuilder();

        sb.insert(0, new Value(100));

        return sb.toString();
    }


}
