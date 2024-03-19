/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.AliasMethodID;
import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.line.MayAliasLine;
import org.opalj.fpcf.properties.alias.line.NoAliasLine;

public class ParameterAlias {

    @AliasMethodID(id = 0, clazz = ParameterAlias.class)
    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = new Object();

        noAliasWithLocal(o1);

        noAliasWithParam(o1, o2);

        mayAliasWithLocal(o1);

        mayAliasWithParam1(o1, o2);
        mayAliasWithParam1(o1, o1);

        mayAliasWithParam2(o1, o1);


        ParameterAlias pa = new ParameterAlias();
        ParameterAlias pa2 = new ParameterAlias();

        pa.mayAliasThisParam();
        pa2.noAliasThisParam();

        pa.mayAliasThisParamTwoMethods1();
        pa.mayAliasThisParamTwoMethods2();

        pa.noAliasThisParamTwoMethods1();
        pa2.noAliasThisParamTwoMethods2();
    }

    @AliasMethodID(id = 1, clazz = ParameterAlias.class)
    public static void noAliasWithLocal(@NoAliasLine(reason = "noAlias with uVar", lineNumber = 46, methodID = 1, clazz = ParameterAlias.class) Object o1) {

        Object o2 = new Object();
        o2.hashCode();
    }

    public static void noAliasWithParam(@NoAlias(reason = "noAlias with other parameter", id = 0, clazz = ParameterAlias.class) Object o1,
                                        @NoAlias(reason = "noAlias with other parameter", id = 0, clazz = ParameterAlias.class) Object o2) {

    }

    @AliasMethodID(id = 2, clazz = ParameterAlias.class)
    public static void mayAliasWithLocal(@MayAliasLine(reason = "mayAlias with uVar", lineNumber = 63, methodID = 2, clazz = ParameterAlias.class) Object o1) {

        Object o2 = new Object();

        if (Math.random() > 0.5) {
            o2 = o1;
        }

        o2.hashCode();
    }

    public static void mayAliasWithParam1(@MayAlias(reason = "mayAlias with other parameter 1", id = 1, clazz = ParameterAlias.class) Object o1,
                                          @MayAlias(reason = "mayAlias with other parameter 1", id = 1, clazz = ParameterAlias.class) Object o2) {

    }

    public static void mayAliasWithParam2(@MayAlias(reason = "mayAlias with other parameter 2", id = 2, clazz = ParameterAlias.class) Object o1,
                                          @MayAlias(reason = "mayAlias with other parameter 2", id = 2, clazz = ParameterAlias.class) Object o2) {

    }

    @MayAliasLine(reason = "may alias with this parameter and invoked uVar", thisParameter = true,
            lineNumber = 32, methodID = 0, clazz = ParameterAlias.class)
    public void mayAliasThisParam() {}

    @NoAliasLine(reason = "no alias with this parameter and invoked uVar", thisParameter = true,
            lineNumber = 32, methodID = 0, clazz = ParameterAlias.class)
    public void noAliasThisParam() {}

    @MayAlias(reason = "may alias with this parameter of two methods", thisParameter = true,
            id = 3, clazz = ParameterAlias.class)
    public void mayAliasThisParamTwoMethods1() {}

    @MayAlias(reason = "may alias with this parameter of two methods", thisParameter = true,
            id = 3, clazz = ParameterAlias.class)
    public void mayAliasThisParamTwoMethods2() {}

    @NoAlias(reason = "no alias with this parameter of two methods", thisParameter = true,
            id = 4, clazz = ParameterAlias.class)
    public void noAliasThisParamTwoMethods1() {}

    @NoAlias(reason = "no alias with this parameter of two methods", thisParameter = true,
            id = 4, clazz = ParameterAlias.class)
    public void noAliasThisParamTwoMethods2() {}

}
