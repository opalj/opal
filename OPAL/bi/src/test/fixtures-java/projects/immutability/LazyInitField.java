/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Immutable;

/**
 * An immutable class which has a private field which is lazily initialized.
 * 
 * @author Andre Pacak
 */
@Immutable("the fields is lazily initialized")
public class LazyInitField {

    private ImmutableClass lazyInitField = null;

    public String getXAsString() {
        if (this.lazyInitField == null) {
            this.lazyInitField = new ImmutableClass();
        }
        return lazyInitField.toString();
    }
}
