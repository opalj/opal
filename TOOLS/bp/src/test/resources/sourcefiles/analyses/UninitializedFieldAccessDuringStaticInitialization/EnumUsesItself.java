/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * An enum that references itself during <clinit> (due to the implicit code generated
 * by Java, such as the values field), and should not trigger reports.
 * 
 * @author Daniel Klauer
 */
public enum EnumUsesItself {
    A("a"), B("b"), C("c");

    private String text;

    private EnumUsesItself(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
