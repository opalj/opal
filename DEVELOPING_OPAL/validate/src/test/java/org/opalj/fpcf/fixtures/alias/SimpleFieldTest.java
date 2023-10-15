package org.opalj.fpcf.fixtures.alias;

import org.opalj.fpcf.properties.alias.Alias;
import org.opalj.fpcf.properties.alias.MayAlias;
import org.opalj.fpcf.properties.alias.MustAlias;
import org.opalj.fpcf.properties.alias.NoAlias;

public class SimpleFieldTest {

    @Alias(mustAlias = {@MustAlias(reason = "field is never reassigned", id = "SF.finalField")})
    private static Object finalObject =
            new @Alias(mustAlias = {@MustAlias(reason = "field is never reassigned", id = "SF.finalField")},
                       noAlias = {@NoAlias(reason = "field is never assigned to object", id = "SF.na")}) Object();


    private static Object nonFinalObject =
            new @Alias(mayAlias = {@MayAlias(reason = "field is assigned to parameter", id = "SF.nonFinalFieldParam"),
                                    @MayAlias(reason = "field is assigned to object", id = "SF.nonFinalFieldObject")},
            noAlias = {@NoAlias(reason = "field is never assigned to object", id = "SF.na")}) Object();

    public static void main(String[] args) {

        Object o1 = new @Alias(mayAlias = {@MayAlias(reason = "field is assigned to object", id = "SF.nonFinalFieldObject")},
                                noAlias = {@NoAlias(reason = "field is never assigned to object", id = "SF.na")}) Object();

        o1.hashCode();

        nonFinalObject = o1;

        reassignField(new Object());

    }

    public static void reassignField(@Alias(mayAlias = {@MayAlias(reason = "field is assigned to parameter", id = "SF.nonFinalFieldParam")}) Object o) {
        nonFinalObject = o;
    }
}
