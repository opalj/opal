/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantField;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.VariableField;

/**
 * An example to test objects created across methods.
 *
 * @author Robin Körkemeier
 */
public class CreateObjectInMethodExample {
    private int a = 42;

    private CreateObjectInMethodExample createNew1() {
        CreateObjectInMethodExample example = new CreateObjectInMethodExample();
        example.a -= 11;
        return example;
    }

    @ObjectValue(tacIndex = 0, constantValues = {@ConstantField(field = "a", value = 33)})
    private CreateObjectInMethodExample createNew2() {
        CreateObjectInMethodExample example = new CreateObjectInMethodExample();
        example.a = a + 2;
        return example;
    }

    @ObjectValue(tacIndex = 0, variableValues = {@VariableField(field = "a")})
    private CreateObjectInMethodExample createNew3() {
        CreateObjectInMethodExample example = new CreateObjectInMethodExample();
        example.a = a + 2;
        return example;
    }

    @ObjectValues({
            @ObjectValue(tacIndex = 0, constantValues = {@ConstantField(field = "a", value = 42)}),
            @ObjectValue(tacIndex = 2, constantValues = {@ConstantField(field = "a", value = 31)}),
            @ObjectValue(tacIndex = 3, constantValues = {@ConstantField(field = "a", value = 33)}),
            @ObjectValue(tacIndex = 4, variableValues = {@VariableField(field = "a")}),
            @ObjectValue(tacIndex = 5, variableValues = {@VariableField(field = "a")})
    })
    public static void main(String[] args) {
        CreateObjectInMethodExample example1 = new CreateObjectInMethodExample();
        CreateObjectInMethodExample example2 = example1.createNew1();
        CreateObjectInMethodExample example3 = example2.createNew2();
        CreateObjectInMethodExample example4 = example3.createNew3();
        CreateObjectInMethodExample example5 = example4.createNew3();

        System.out.println("e1: " + example1.a + ", e2: " + example2.a + ", e3: " + example3.a + ", e4: " +
                example4.a + ", e5: " + example5.a);
    }
}
