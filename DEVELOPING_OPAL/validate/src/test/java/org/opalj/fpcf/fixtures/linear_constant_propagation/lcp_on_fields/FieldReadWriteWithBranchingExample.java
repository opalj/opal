/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantField;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.VariableField;

/**
 * An example to test reading and writing object fields in presence of if-then-else constructs.
 *
 * @author Robin Körkemeier
 */
public class FieldReadWriteWithBranchingExample {
    private int a = -1;

    @ObjectValue(tacIndex = 0, variableValues = {@VariableField(field = "a")})
    public static FieldReadWriteWithBranchingExample multipleReturns(int y) {
        FieldReadWriteWithBranchingExample e = new FieldReadWriteWithBranchingExample();
        if (y > 0) {
            e.a = 42;
            return e;
        } else {
            e.a = 23;
            return e;
        }
    }

    @ObjectValues({
            @ObjectValue(tacIndex = 0, constantValues = {@ConstantField(field = "a", value = 42)}),
            @ObjectValue(tacIndex = 2, variableValues = {@VariableField(field = "a")}),
            @ObjectValue(tacIndex = 4, variableValues = {@VariableField(field = "a")})
    })
    public static void main(String[] args) {
        FieldReadWriteWithBranchingExample example1 = new FieldReadWriteWithBranchingExample();
        FieldReadWriteWithBranchingExample example2 = new FieldReadWriteWithBranchingExample();
        FieldReadWriteWithBranchingExample example3 = new FieldReadWriteWithBranchingExample();

        if (args.length == 0) {
            example1.a = 42;
            example2.a = 23;
            example3.a = example2.a;
        } else {
            example1.a = 40;
            example1.a += 2;
            example3.a = example1.a;
        }

        System.out.println("e1: " + example1.a + ", e2: " + example2.a + ", e3: " + example3.a);
    }
}
