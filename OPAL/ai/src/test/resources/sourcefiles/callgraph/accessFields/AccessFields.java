/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.accessFields;

import callgraph.base.AlternateBase;
import callgraph.base.ConcreteBase;
import org.opalj.ai.test.invokedynamic.annotations.AccessedField;

/**
 * This class was used to create a class file with some well defined attributes. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * <!--
 * 
 * 
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * 
 * 
 * -->
 * 
 * @author Marco Jacobasch
 */
public class AccessFields {

    ConcreteBase concreteBase = new ConcreteBase();
    AlternateBase alternerateBase = new AlternateBase();

    @AccessedField(declaringType = ConcreteBase.class, fieldType = Integer.class, name = "integer", line = 65)
    public int accessFieldInClass() {
        return concreteBase.integer;
    }

    @AccessedField(declaringType = ConcreteBase.class, fieldType = String.class, name = "string", line = 70)
    public String accessFieldInSuperClass() {
        return concreteBase.string;
    }

    @AccessedField(declaringType = AlternateBase.class, fieldType = String.class, name = "string", line = 75)
    public String accessFieldInClassSameFieldNameInSuperClass() {
        return alternerateBase.string;
    }

    @AccessedField(declaringType = ConcreteBase.class, fieldType = Double.class, name = "DOUBLE_FIELD", line = 80)
    public double accessStaticField() {
        return ConcreteBase.DOUBLE_FIELD;
    }

    // TODO static super
}
