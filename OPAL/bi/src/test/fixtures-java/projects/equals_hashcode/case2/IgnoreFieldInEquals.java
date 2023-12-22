/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package equals_hashcode.case2;

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
@ContractFulfillment(fulfillContract = false, remark = "Consider instance field in hashCode(), but not in equals()")
public class IgnoreFieldInEquals {

    private int t = 42;

    IgnoreFieldInEquals(int t) {
        this.t = t;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + t;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        // IgnoreFieldInEquals other = (IgnoreFieldInEquals) obj;
        // if (t != other.t)
        // return false;
        return true;
    }

}
