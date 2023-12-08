/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package equals_hashcode.case7;

import equals_hashcode.ContractFulfillment;

@ContractFulfillment(fulfillContract = false, remark = "Missing call to super.equals() means object state is ignored completely in equals(), but inherited implementation of hashCode() considers state")
public class NoHashCodeOverrideNoSuperCheckSub extends
        NoHashCodeOverrideNoSuperCheckSuper {

    NoHashCodeOverrideNoSuperCheckSub(int t) {
        super(t);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        // if (!super.equals(obj))
        // return false;
        if (!(obj instanceof NoHashCodeOverrideNoSuperCheckSub))
            return false;
        return true;
    }

}
