/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UnusedPrivateFields;

/**
 * Class with an unused private serialVersionUID field. serialVersionUID is not special
 * here, because this class is not Serializable, thus it should be reported.
 * 
 * @author Daniel Klauer
 */
public class UnusedSerialVersionUID {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;
}
