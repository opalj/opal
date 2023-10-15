package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.Alias;
import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.NoAlias;

public class SimpleParameters {

    static int a = 0;

    public static void main(String[] args) {
        noAlias1(new Object());
        noAlias2(new Object(), new Object());
        mayAlias1(new Object());
        mayAlias2(new Object(), new Object());

        Object o1 = new Object();
        mustAlias(o1, o1);
    }

    public static void noAlias1(@Alias(noAlias = @NoAlias(reason = "noAlias", id = "SP.na1")) Object o1) {

        Object o2 = new @Alias(noAlias = @NoAlias(reason = "noAlias", id = "SP.na1")) Object();
        o1.hashCode();
        o2.hashCode();
    }

    public static void noAlias2(@Alias(noAlias = @NoAlias(reason = "noAlias", id = "SP.na2")) Object o1,
                                @Alias(noAlias = @NoAlias(reason = "noAlias", id = "SP.na2")) Object o2) {

        o1.hashCode();
        o2.hashCode();
    }

    public static void mayAlias1(@Alias(mayAlias = @MayAlias(reason = "mayAlias", id = "SP.ma1")) Object o1) {

        Object o2 = new @Alias(mayAlias = @MayAlias(reason = "mayAlias", id = "SP.ma1")) Object();
        o2.hashCode();

        if (a == 1) {
            o2 = o1;
        }

        o2.hashCode();
    }

    public static void mayAlias2(@Alias(mayAlias = @MayAlias(reason = "mayAlias", id = "SP.ma2")) Object o1,
                                 @Alias(mayAlias = @MayAlias(reason = "mayAlias", id = "SP.ma2")) Object o2) {

        o1.hashCode();
        o2.hashCode();

        if (a == 1) {
            o2 = o1;
        }

        o2.hashCode();
    }

    public static void mustAlias(@Alias(mayAlias = @MayAlias(reason = "mayAlias", id = "SP.mua")) Object o1,
                                 @Alias(mayAlias = @MayAlias(reason = "mayAlias", id = "SP.mua")) Object o2) {

        o1.hashCode();
        o2.hashCode();
    }

}
