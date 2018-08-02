/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_locality.subpackage;

public class UsesFields {

    public int getValue(PackagePrivateFields ppf){
        return ppf.localField[0];
    }

    public void assignField(PackagePrivateFields ppf, Object o){
        ppf.assigned = o;
    }

    public void replaceField(PackagePrivateFields ppf){
        ppf.localField = new int[10];
    }

    public char[] getField(PackagePrivateFields ppf){
        return ppf.escaped;
    }
}
