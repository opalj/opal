/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.AliasMethodID;
import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.MustAlias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.MustAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;

public class StaticFieldAlias {

    @MayAliasLine(reason = "may alias with field and assigned uVar", lineNumber = 46, methodID = 0, clazz = StaticFieldAlias.class)
    @NoAlias(reason = "no alias with field and parameter", id = 1, clazz = StaticFieldAlias.class)
    @NoAlias(reason = "no alias with field and return value", id = 3, clazz = StaticFieldAlias.class)
    @NoAliasLine(reason = "noAlias with field and unrelated uVar", lineNumber = 60, methodID = 1, clazz = StaticFieldAlias.class)
    @MayAlias(reason = "may alias with field and return value", id = 2, clazz = StaticFieldAlias.class)
    @MayAliasLine(reason = "may alias with field and returned uVar", lineNumber = 73, methodID = 4, clazz = StaticFieldAlias.class)
    @MayAlias(reason = "may alias with field and return value via parameter", id = 5, clazz = StaticFieldAlias.class)
    @MayAliasLine(reason = "may alias with field and returned uVar via parameter", lineNumber = 90, methodID = 6, clazz = StaticFieldAlias.class)
    @MayAlias(reason = "may alias with field and parameter", id = 6, clazz = StaticFieldAlias.class)
    public static Object mayAliasField = new Object();

    @MayAliasLine(reason = "may alias with field and assigned uVar", lineNumber = 66, methodID = 3, clazz = StaticFieldAlias.class)
    public static Object nullField = null;

    @MustAlias(reason = "must alias with static final field and return value", id = 4, clazz = StaticFieldAlias.class)
    @MustAliasLine(reason = "must alias with static final field and returned uVar", lineNumber = 80, methodID = 5, clazz = StaticFieldAlias.class)
    @MustAlias(reason = "must alias with static final field and return value via parameter", id = 7, clazz = StaticFieldAlias.class)
    @MustAliasLine(reason = "must alias with static final field and returned uVar via parameter", lineNumber = 98, methodID = 7, clazz = StaticFieldAlias.class)
    @MustAlias(reason = "must alias with static final field and parameter", id = 8, clazz = StaticFieldAlias.class)
    public static Object staticFinalField = new Object();

    public static void main(String[] args) {
        noAlias(new Object());
        noAlias(staticFinalField);

        parameterIsMayAliasField(mayAliasField);
        parameterIsStaticFinalField(staticFinalField);
    }

    @AliasMethodID(id = 0, clazz = StaticFieldAlias.class)
    public static void reassignField() {
        Object o = new Object();
        mayAliasField = o;
    }

    @AliasMethodID(id = 1, clazz = StaticFieldAlias.class)
    @NoAlias(reason = "no alias with field and return value", id = 3, clazz = StaticFieldAlias.class)
    public static Object noAlias(
            @NoAlias(reason = "no alias with field and parameter", id = 1, clazz = StaticFieldAlias.class)
            Object a) {
        Object o = new Object();

        if (Math.random() > 0.5) {
            o = a;
        }

        return o;
    }

    @AliasMethodID(id = 3, clazz = StaticFieldAlias.class)
    public static void reassignNullField() {
        Object o = new Object();
        nullField = o;
    }

    @AliasMethodID(id = 4, clazz = StaticFieldAlias.class)
    @MayAlias(reason = "may alias with field and return value", id = 2, clazz = StaticFieldAlias.class)
    public static Object returnMayAliasField() {
        Object o = mayAliasField;
        return o;
    }

    @AliasMethodID(id = 5, clazz = StaticFieldAlias.class)
    @MustAlias(reason = "must alias with static final field and return value", id = 4, clazz = StaticFieldAlias.class)
    public static Object returnStaticFinalField() {
        Object o = staticFinalField;
        return o;
    }

    @AliasMethodID(id = 6, clazz = StaticFieldAlias.class)
    @MayAlias(reason = "may alias with field and return value via parameter", id = 5, clazz = StaticFieldAlias.class)
    @MayAliasLine(reason = "may alias with return value and uVar via parameter and field", lineNumber = 46, methodID = 0, clazz = StaticFieldAlias.class)
    public static Object parameterIsMayAliasField(
            @MayAlias(reason = "may alias with field and parameter", id = 6, clazz = StaticFieldAlias.class)
            @MayAliasLine(reason = "may alias with parameter and UVar via field", lineNumber = 46, methodID = 0, clazz = StaticFieldAlias.class)
            Object a) {
        return a;
    }

    @AliasMethodID(id = 7, clazz = StaticFieldAlias.class)
    @MustAlias(reason = "must alias with static final field and return value via parameter", id = 7, clazz = StaticFieldAlias.class)
    public static Object parameterIsStaticFinalField(
            @MustAlias(reason = "must alias with static final field and parameter", id = 8, clazz = StaticFieldAlias.class)
            Object a) {
        return a;
    }

}
