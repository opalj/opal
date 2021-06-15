package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects.counterExamples;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * This class encompasses different cases of lazy initialization with wrongly used guards.
 */
public class WrongGuards {

    @AssignableField("the guard does not only dependent on field value")
    private Integer dclWithLock;

    public Integer getDclWithLockOr(boolean lock) {
        if(dclWithLock ==null && lock){
            synchronized(this) {
                if(dclWithLock ==null && lock){
                    dclWithLock = new Integer(5);
                }
            }
        }
        return dclWithLock;
    }

    @AssignableField("no lazy initialization due to wrong guard statement")
    private Integer wrongGuardStatement;

    public Integer getWrongGuardStatement() {
        if (wrongGuardStatement != null) {
            synchronized (this) {
                if (wrongGuardStatement != null) {
                    wrongGuardStatement = new Integer(5);
                }
            }
        }
        return wrongGuardStatement;
    }
}
