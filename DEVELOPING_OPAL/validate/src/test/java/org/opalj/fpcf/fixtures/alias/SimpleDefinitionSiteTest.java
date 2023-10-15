package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.Alias;
import org.opalj.fpcf.properties.alias.NoAlias;
import org.opalj.fpcf.properties.alias.MayAlias;

public class SimpleDefinitionSiteTest {

    static int a = 0;

    public static void main(String[] args) {
        noAlias();
        mayAlias();
    }

    public static void noAlias() {

        Object o1 = new @Alias(noAlias = @NoAlias(reason = "o1 and o2 do not alias", id = "SDS.na")) Object();
        Object o2 = new @Alias(noAlias = @NoAlias(reason = "o1 and o2 do not alias", id = "SDS.na")) Object();

        Object o3 = new Object();

        o1.hashCode();
        o2.hashCode();
        o3.hashCode();

        if (a == 1) {
            o3 = o1;
        }

        o3.hashCode();

    }

    public static void mayAlias() {

        Object o1 = new @Alias(mayAlias = @MayAlias(reason = "o1 and o2 may alias", id = "SDS.ma")) Object();
        Object o2 = new @Alias(mayAlias = @MayAlias(reason = "o1 and o2 may alias", id = "SDS.ma")) Object();

        o1.hashCode();
        o2.hashCode();

        if (a == 0) {
            o2 = o1;
        }

        o2.hashCode();
    }

    // a simple must alias test would be optimized away by the compiler

}
