/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.lcp_on_fields.ObjectValues;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;

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

    @ObjectValue(variable = "lv0", constantValues = {@ConstantValue(variable = "a", value = 33)})
    private CreateObjectInMethodExample createNew2() {
        CreateObjectInMethodExample example = new CreateObjectInMethodExample();
        example.a = a + 2;
        return example;
    }

    @ObjectValue(variable = "lv0", variableValues = {@VariableValue(variable = "a")})
    private CreateObjectInMethodExample createNew3() {
        CreateObjectInMethodExample example = new CreateObjectInMethodExample();
        example.a = a + 2;
        return example;
    }

    @ObjectValues({
            @ObjectValue(variable = "lv0", constantValues = {@ConstantValue(variable = "a", value = 42)}),
            @ObjectValue(variable = "lv2", constantValues = {@ConstantValue(variable = "a", value = 31)}),
            @ObjectValue(variable = "lv3", constantValues = {@ConstantValue(variable = "a", value = 33)}),
            @ObjectValue(variable = "lv4", variableValues = {@VariableValue(variable = "a")}),
            @ObjectValue(variable = "lv5", variableValues = {@VariableValue(variable = "a")})
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
