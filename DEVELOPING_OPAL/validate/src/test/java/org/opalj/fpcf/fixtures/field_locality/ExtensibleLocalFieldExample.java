/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_locality;

import org.opalj.fpcf.properties.field_locality.ExtensibleLocalField;

public class ExtensibleLocalFieldExample {

    @ExtensibleLocalField("This field is local only for this precise type, a subtype coud leak it")
    private int[] extensibleLocalField;
}
