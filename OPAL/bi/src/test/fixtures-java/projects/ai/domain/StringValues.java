/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

/**
 * Methods which perform computations related to `String` values to test the handling of
 * them by the respective domain.
 *
 * @author Michael Eichberg
 */
public class StringValues {

    public String constant() {
        return "const";
    }

    public String that(String s) {
        return s;
    }

    public String aOrB(int i) {
        if (i >= 0)
            return "a";
        else
            return "b";
    }

    public String aOrBAlt(int i) {
        String s = null;
        if (i >= 0)
            s = "a";
        else
            s = "b";

        return s;
    }

    public String sb(int i, int j, int z) {
        StringBuilder sb = new StringBuilder();
        sb.append(i);
        sb.append(j).append(z);
        return sb.append("Good Bye!").toString();
    }

    public static String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello").append(" World");
        return sb.toString();
    }


    public static void main(String[] args) {
        System.out.println(build());
    }
}
