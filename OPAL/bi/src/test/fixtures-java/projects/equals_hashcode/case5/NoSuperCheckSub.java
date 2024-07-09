/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package equals_hashcode.case5;

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
@ContractFulfillment(fulfillContract = true, remark = "This implementation only 'happens' to fulfill the contract because t_super can't be modified in the superclass after its creation (suspicious!)")
public class NoSuperCheckSub extends NoSuperCheckSuper {

    private int t_sub = 42;

    NoSuperCheckSub(int t) {
        super(t);
        this.t_sub = t;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + t_sub;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        // if (!super.equals(obj))
        // return false;
        if (!(obj instanceof NoSuperCheckSub))
            return false;
        NoSuperCheckSub other = (NoSuperCheckSub) obj;
        if (t_sub != other.t_sub)
            return false;
        return true;
    }

}
