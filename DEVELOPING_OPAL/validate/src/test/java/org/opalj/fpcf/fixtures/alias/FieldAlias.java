/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;
import org.opalj.tac.fpcf.analyses.alias.pointsto.AllocationSitePointsToBasedAliasAnalysis;

public class FieldAlias {

    @NoAliasLine(reason = "no alias for fields of different objects",
            lineNumber = 20, fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 21, secondFieldName = "field2", secondFieldClass = FieldClass.class, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public void differentObjectsSameFields() {
        FieldClass fc = new FieldClass();
        FieldClass fc2 = new FieldClass();

        fc.field = new Object();
        fc2.field = new Object();

        fc.field.hashCode();
        fc2.field.hashCode();
    }

    @NoAliasLine(reason = "no alias for different fields of the same object",
            lineNumber = 32, fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 33, secondFieldName = "field2", secondFieldClass = FieldClass.class, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public void sameObjectDifferentField() {
        FieldClass fc = new FieldClass();
        fc.field = new Object();
        fc.field2 = new Object();

        fc.field.hashCode();
        fc.field2.hashCode();
    }

    @NoAliasLine(reason = "no alias for different fields of different objects",
            lineNumber = 46, fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 47, secondFieldName = "field2", secondFieldClass = FieldClass.class, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public void differentObjectsDifferentFields() {
        FieldClass fc = new FieldClass();
        FieldClass fc2 = new FieldClass();

        fc.field = new Object();
        fc2.field2 = new Object();

        fc.field.hashCode();
        fc2.field2.hashCode();
    }

    @MayAliasLine(reason = "may alias for same fields of the same object",
            lineNumber = 57, fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 60, secondFieldName = "field", secondFieldClass = FieldClass.class)
    public void sameObjectSameField() {
        FieldClass fc = new FieldClass();

        fc.field = new Object();
        fc.field.hashCode();

        fc.field = new Object();
        fc.field.hashCode();
    }

    @MayAliasLine(reason = "may alias for same fields of the same object",
            lineNumber = 71, fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 72, secondFieldName = "field", secondFieldClass = FieldClass.class)
    public void sameObjectSameField2() {
        FieldClass fc = new FieldClass();

        fc.field = new Object();

        fc.field.hashCode();
        fc.field.hashCode();
    }

    public static void paramMayBeField(
            @MayAliasLine(reason = "may alias with parameter and field",
                    lineNumber = 110, methodName = "main", fieldName = "field", fieldClass = FieldClass.class)
            @NoAliasLine(reason = "no alias with parameter and field",
                    lineNumber = 111, methodName = "main", fieldName = "field2", fieldClass = FieldClass.class, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
            Object o) {
    }

    @MayAliasLine(reason = "may alias of field via parameter",
            lineNumber = 110,  methodName = "main", fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 111, secondMethodName = "main", secondFieldName = "field", secondFieldClass = FieldClass.class)
    @NoAliasLine(reason = "no alias of field via parameter",
            lineNumber = 110,  methodName = "main", fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 111, secondMethodName = "main", secondFieldName = "field2", secondFieldClass = FieldClass.class, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public static void fieldAliasViaParameter(FieldClass fc) {
        fc.field.hashCode();
        fc.field2.hashCode();
    }

    @NoAliasLine(reason = "no alias of field via parameter",
            lineNumber = 110,methodName = "main", fieldName = "field", fieldClass = FieldClass.class,
            secondLineNumber = 111, secondMethodName = "main", secondFieldName = "field2", secondFieldClass = FieldClass.class, analyses = {AllocationSitePointsToBasedAliasAnalysis.class})
    public static void noFieldAliasViaParameter(FieldClass fc) {
        fc.field.hashCode();
    }

    public static void main(String[] args) {
        FieldClass fc = new FieldClass();

        fc.field = new Object();
        fc.field2 = new Object();

        paramMayBeField(fc.field);
        fieldAliasViaParameter(fc);

        fc.field.hashCode();
        fc.field2.hashCode();

        noFieldAliasViaParameter(new FieldClass());
    }

}

class FieldClass {

    public Object field;

    public Object field2;
}