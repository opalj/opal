/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;
//import org.opalj.tac.fpcf.analyses.alias.AllocationSiteBasedAliasAnalysis;

public class ArrayAlias {

    @NoAliasLine(reason = "no alias with array and uVar that is not stored to array",
            lineNumber = 20,
            secondLineNumber = 22,
            analyses = {/*AllocationSiteBasedAliasAnalysis.class*/})
    public static void notStoredToArray() {

        Object[] arr = new Object[10];

        Object o1 = new Object();

        o1.hashCode();
        Object arrObject = arr[0];
        arrObject.hashCode();
    }

    @MayAliasLine(reason = "may alias with array and uVar that is stored to array",
            lineNumber = 41,
            secondLineNumber = 43)
    @MayAliasLine(reason = "may alias with array and uVar that is stored to array",
            lineNumber = 45,
            secondLineNumber = 47)
    public static void storedToArrayDifferentIndices() {

        Object[] arr = new Object[10];

        Object o1 = new Object();
        Object o2 = new Object();

        arr[0] = o1;
        arr[1] = o2;

        o1.hashCode();
        Object arrObject1 = arr[0];
        arrObject1.hashCode();

        o2.hashCode();
        Object arrObject2 = arr[1];
        arrObject2.hashCode();
    }

    @MayAliasLine(reason = "may alias with array and uVar that is stored to array",
            lineNumber = 65,
            secondLineNumber = 67)
    @MayAliasLine(reason = "may alias with array and uVar that is stored to array",
            lineNumber = 69,
            secondLineNumber = 71)
    public static void storedToArraySameIndex() {
        Object[] arr = new Object[10];

        Object o1 = new Object();
        Object o2 = new Object();

        arr[0] = o1;
        arr[0] = o2;

        o1.hashCode();
        Object arrObject1 = arr[0];
        arrObject1.hashCode();

        o2.hashCode();
        Object arrObject2 = arr[0];
        arrObject2.hashCode();
    }

    @MayAliasLine(reason = "may alias with Object[] and Object[]",
            lineNumber = 113,
            secondLineNumber = 114)
    @MayAliasLine(reason = "may alias with Object[] and Object[][]",
            lineNumber = 113,
            secondLineNumber = 115)
    @MayAliasLine(reason = "may alias with Object[] and Object[][0]",
            lineNumber = 113,
            secondLineNumber = 117)
    @MayAliasLine(reason = "may alias with Object[] and Integer[]",
            lineNumber = 113,
            secondLineNumber = 118)
    @NoAliasLine(reason = "may alias with Integer[] and String[]",
            lineNumber = 118,
            secondLineNumber = 119)
    public static void arraySubTypes() {

        Object[] objectArray = new Object[10];
        Object[] objectArray2 = new Object[10];
        Object[][] objectArray2D = new Object[10][10];
        Integer[] integerArray = new Integer[10];
        String[] stringArray = new String[10];

        if (Math.random() < 0.5) {
            objectArray = objectArray2;
        }

        if (Math.random() < 0.5) {
            objectArray = objectArray2D;
        }

        if (Math.random() < 0.5) {
            objectArray = objectArray2D[0];
        }

        if (Math.random() < 0.5) {
            objectArray = integerArray;
        }

        objectArray.hashCode();
        objectArray2.hashCode();
        objectArray2D.hashCode();
        Object[] innerObjectArray = objectArray2D[0];
        innerObjectArray.hashCode();
        integerArray.hashCode();
        stringArray.hashCode();
    }

}
