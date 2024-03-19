/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.AliasMethodID;
import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;

public class ReturnValueAlias {

    @AliasMethodID(id = 0, clazz = ReturnValueAlias.class)
    @NoAliasLine(reason = "no Alias with local variable", lineNumber = 17, methodID = 0, clazz = ReturnValueAlias.class)
    public static Object noAliasWithLocal() {
        Object o1 = new Object();

        o1.hashCode();

        return new Object();
    }

    @AliasMethodID(id = 1, clazz = ReturnValueAlias.class)
    @MayAliasLine(reason = "mayAlias with local variable", lineNumber = 32, methodID = 1, clazz = ReturnValueAlias.class)
    public static Object mayAliasWithLocal1() {
        Object o1 = new Object();
        Object o2 = new Object();

        if (Math.random() > 0.5) {
            o2 = o1;
        }

        o2.hashCode();

        return o2;
    }

    @AliasMethodID(id = 2, clazz = ReturnValueAlias.class)
    @MayAliasLine(reason = "mayAlias with local variable", lineNumber = 42, methodID = 2, clazz = ReturnValueAlias.class)
    public static Object mayAliasWithLocal2() {
        Object o1 = new Object();

        o1.hashCode();

        return o1;
    }

    @NoAlias(reason = "noAlias with parameter", id = 0, clazz = ReturnValueAlias.class)
    public static Object noAliasWithParam(
            @NoAlias(reason = "noAlias with parameter", id = 0, clazz = ReturnValueAlias.class)
            Object a) {
        Object o1 = new Object();
        return o1;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 1, clazz = ReturnValueAlias.class)
    public static Object mayAliasWithParam1(
            @MayAlias(reason = "mayAlias with parameter", id = 1, clazz = ReturnValueAlias.class)
            Object a) {
        return a;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 2, clazz = ReturnValueAlias.class)
    public static Object mayAliasWithParam2(
            @MayAlias(reason = "mayAlias with parameter", id = 2, clazz = ReturnValueAlias.class)
            Object a) {
        return a;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 3, clazz = ReturnValueAlias.class)
    public static Object mayAliasWithParam3(
            @MayAlias(reason = "mayAlias with parameter", id = 3, clazz = ReturnValueAlias.class)
            Object a) {

        Object o1 = new Object();

        if (Math.random() > 0.5) {
            o1 = a;
        }

        return o1;
    }

    @MayAlias(reason = "mayAlias with parameter", id = 4, clazz = ReturnValueAlias.class)
    public static Object mayAliasWithParam4(
            @MayAlias(reason = "mayAlias with parameter", id = 4, clazz = ReturnValueAlias.class)
            Object a) {

        Object o1 = new Object();

        if (Math.random() > 0.5) {
            return o1;
        }

        return a;
    }

    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = new Object();

        mayAliasWithParam1(o1);

        mayAliasWithParam2(o1);
        mayAliasWithParam2(o2);

        mayAliasWithParam3(o1);

        mayAliasWithParam4(o1);
    }

}
