/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.objects.counterExamples;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * This class encompasses different cases of lazy initialization with incorrectly used guards.
 */
public class IncorrectGuards {

    @AssignableField("the guard does not only dependent on field value")
    private Integer dclWithLock;

    public Integer getDclWithLock(boolean lock) {
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
