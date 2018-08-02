/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_locality.subpackage;

import org.opalj.fpcf.properties.field_locality.LocalField;
import org.opalj.fpcf.properties.field_locality.NoLocalField;

public class PackagePrivateFields {

    @LocalField("This field is local although it is used in other classes in this package")
    int[] localField;

    @NoLocalField("This field is not local because it is assigned in UsesFields.assignField")
    Object assigned;

    @NoLocalField("This field is not local as it escapes UsesFields.getField")
    char[] escaped;

    public PackagePrivateFields clone() throws CloneNotSupportedException {
        PackagePrivateFields copy = (PackagePrivateFields) super.clone();
        copy.localField = new int[1];
        copy.assigned = new Object();
        copy.escaped = new char[1];
        return copy;
    }
}
