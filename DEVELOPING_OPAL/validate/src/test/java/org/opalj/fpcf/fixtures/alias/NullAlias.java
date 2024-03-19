/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.AliasMethodID;
import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.MustAlias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.MustAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;

public class NullAlias {

    @MustAlias(reason = "Field is always null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
    public static Object nullField = null;

    @MayAlias(reason = "Field may be null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
    public static Object mayBeNullField = null;

    @NoAlias(reason = "Field is never null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
    public static Object neverNullField = new Object();

    public static void reassignField() {
        Object o = new Object();
        mayBeNullField = o;
    }

    @MustAlias(reason = "always returns null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
    public static Object mustReturnNull() {
        return null;
    }

    @MayAlias(reason = "may return null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
    public static Object mayReturnNull() {
        if (Math.random() > 0.5) {
            return new Object();
        }
        return null;
    }

    @NoAlias(reason = "never returns null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
    public static Object neverReturnNull() {
        return new Object();
    }

    public static void main(String[] args) {
        paramIsAlwaysNull(null);
        paramMayBeNull(null);
        paramMayBeNull(new Object());
        paramIsNeverNull(new Object());
    }

    @AliasMethodID(id = 0, clazz = NullAlias.class)
    @MustAliasLine(reason = "uVar is always null via parameter", lineNumber = 58, methodID = 0, aliasWithNull = true, clazz = NullAlias.class)
    public static void paramIsAlwaysNull(
            @MustAlias(reason = "parmeter is always null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
            Object o) {
        o.hashCode();
    }

    @AliasMethodID(id = 1, clazz = NullAlias.class)
    @MayAliasLine(reason = "uVar may be null via parameter", lineNumber = 66, methodID = 1, aliasWithNull = true, clazz = NullAlias.class)
    public static void paramMayBeNull(
            @MayAlias(reason = "parameter may be null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
            Object o) {
        o.hashCode();
    }

    @AliasMethodID(id = 2, clazz = NullAlias.class)
    @NoAliasLine(reason = "uVar is never null via parameter", lineNumber = 74, methodID = 2, aliasWithNull = true, clazz = NullAlias.class)
    public static void paramIsNeverNull(
            @NoAlias(reason = "parameter is never null", id = -1, aliasWithNull = true, clazz = NullAlias.class)
            Object o) {
        o.hashCode();
    }

    @AliasMethodID(id = 3, clazz = NullAlias.class)
    @MustAliasLine(reason = "uVar is always null", lineNumber = 81, methodID = 3, aliasWithNull = true, clazz = NullAlias.class)
    public static void UVarIsAlwaysNull() {
        Object o = null;
        o.hashCode();
    }

    @AliasMethodID(id = 4, clazz = NullAlias.class)
    @MayAliasLine(reason = "uVar may be null", lineNumber = 91, methodID = 4, aliasWithNull = true, clazz = NullAlias.class)
    public static void UVarMayBeNull() {
        Object o = null;
        if (Math.random() > 0.5) {
            o = new Object();
        }
        o.hashCode();
    }

    @AliasMethodID(id = 5, clazz = NullAlias.class)
    @NoAliasLine(reason = "uVar is never null", lineNumber = 98, methodID = 5, aliasWithNull = true, clazz = NullAlias.class)
    public static void UVarIsNeverNull() {
        Object o = new Object();
        o.hashCode();
    }

}
