/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package equals_hashcode.case6;

import equals_hashcode.ContractFulfillment;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
@ContractFulfillment(fulfillContract = true, remark = "Overriding hashCode() not required because subclass can't hold more state information than superclass")
public class NoHashCodeOverrideSub extends NoHashCodeOverrideSuper {

    NoHashCodeOverrideSub(int t) {
        super(t);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof NoHashCodeOverrideSub))
            return false;
        return true;
    }

}
