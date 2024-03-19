/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.AliasFieldID;
import org.opalj.fpcf.properties.alias.AliasMethodID;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;

public class FieldAlias {

    @AliasMethodID(id = 0, clazz = FieldAlias.class)
    @NoAliasLine(reason = "no alias for fields of different objects",
            lineNumber = 23, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 0,
            secondLineNumber = 24, secondFieldReference = true, secondFieldID = 1, secondFieldClass = FieldClass.class, secondMethodID = 0,
            clazz = FieldAlias.class)
    public void differentObjectsSameFields() {
        FieldClass fc = new FieldClass();
        FieldClass fc2 = new FieldClass();

        fc.field = new Object();
        fc2.field = new Object();

        fc.field.hashCode();
        fc2.field.hashCode();
    }

    @AliasMethodID(id = 1, clazz = FieldAlias.class)
    @NoAliasLine(reason = "no alias for different fields of the same object",
            lineNumber = 37, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 1,
            secondLineNumber = 38, secondFieldReference = true, secondFieldID = 1, secondFieldClass = FieldClass.class, secondMethodID = 1,
            clazz = FieldAlias.class)
    public void sameObjectDifferentField() {
        FieldClass fc = new FieldClass();
        fc.field = new Object();
        fc.field2 = new Object();

        fc.field.hashCode();
        fc.field2.hashCode();
    }

    @AliasMethodID(id = 2, clazz = FieldAlias.class)
    @NoAliasLine(reason = "no alias for different fields of different objects",
            lineNumber = 53, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 2,
            secondLineNumber = 54, secondFieldReference = true, secondFieldID = 1, secondFieldClass = FieldClass.class, secondMethodID = 2,
            clazz = FieldAlias.class)
    public void differentObjectsDifferentFields() {
        FieldClass fc = new FieldClass();
        FieldClass fc2 = new FieldClass();

        fc.field = new Object();
        fc2.field2 = new Object();

        fc.field.hashCode();
        fc2.field2.hashCode();
    }

    @AliasMethodID(id = 3, clazz = FieldAlias.class)
    @MayAliasLine(reason = "may alias for same fields of the same object",
            lineNumber = 66, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 3,
            secondLineNumber = 69, secondFieldReference = true, secondFieldID = 0, secondFieldClass = FieldClass.class, secondMethodID = 3,
            clazz = FieldAlias.class)
    public void sameObjectSameField() {
        FieldClass fc = new FieldClass();

        fc.field = new Object();
        fc.field.hashCode();

        fc.field = new Object();
        fc.field.hashCode();
    }

    @AliasMethodID(id = 4, clazz = FieldAlias.class)
    @MayAliasLine(reason = "may alias for same fields of the same object",
            lineNumber = 82, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 4,
            secondLineNumber = 83, secondFieldReference = true, secondFieldID = 0, secondFieldClass = FieldClass.class, secondMethodID = 4,
            clazz = FieldAlias.class)
    public void sameObjectSameField2() {
        FieldClass fc = new FieldClass();

        fc.field = new Object();

        fc.field.hashCode();
        fc.field.hashCode();
    }

    public static void paramMayBeField(
            @MayAliasLine(reason = "may alias with parameter and field",
                    lineNumber = 126, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 7,
                    clazz = FieldAlias.class)
            @NoAliasLine(reason = "no alias with parameter and field",
                    lineNumber = 129, fieldReference = true, fieldID = 1, fieldClass = FieldClass.class, methodID = 7,
                    clazz = FieldAlias.class)
            Object o) {
    }

    @AliasMethodID(id = 5, clazz = FieldAlias.class)
    @MayAliasLine(reason = "may alias of field via parameter",
            lineNumber = 126, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 7,
            secondLineNumber = 106, secondFieldReference = true, secondFieldID = 0, secondFieldClass = FieldClass.class, secondMethodID = 5,
            clazz = FieldAlias.class)
    @NoAliasLine(reason = "may alias of field via parameter",
            lineNumber = 129, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 7,
            secondLineNumber = 107, secondFieldReference = true, secondFieldID = 1, secondFieldClass = FieldClass.class, secondMethodID = 5,
            clazz = FieldAlias.class)
    public static void fieldAliasViaParameter(FieldClass fc) {
        fc.field.hashCode();
        fc.field2.hashCode();
    }

    @AliasMethodID(id = 6, clazz = FieldAlias.class)
    @NoAliasLine(reason = "no alias of field via parameter",
            lineNumber = 126, fieldReference = true, fieldID = 0, fieldClass = FieldClass.class, methodID = 7,
            secondLineNumber = 116, secondFieldReference = true, secondFieldID = 1, secondFieldClass = FieldClass.class, secondMethodID = 6,
            clazz = FieldAlias.class)
    public static void noFieldAliasViaParameter(FieldClass fc) {
        fc.field.hashCode();
    }

    @AliasMethodID(id = 7, clazz = FieldAlias.class)
    public static void main(String[] args) {
        FieldClass fc = new FieldClass();

        fc.field = new Object();
        fc.field2 = new Object();

        paramMayBeField(fc.field);
        fieldAliasViaParameter(fc);

        fc.field2.hashCode();

        noFieldAliasViaParameter(new FieldClass());
    }

}

class FieldClass {

    @AliasFieldID(id = 0, clazz = FieldClass.class)
    public Object field;

    @AliasFieldID(id = 1, clazz = FieldClass.class)
    public Object field2;
}