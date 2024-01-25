/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UnusedPrivateFields;

/**
 * A Serializable class with a private serialVersionUID field. The serialVersionUID field
 * is used by Serializable, and thus it should not be reported.
 * 
 * @author Daniel Klauer
 */
class UsedSerialVersionUID implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
}
