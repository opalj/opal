/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class TypeCastsInLazyInitialization {

    @MutableFieldReference("Analysis couldn't handle typecasts")
    private Integer iF;

    public synchronized Integer getIF(){
        if(iF==0F)
            iF = 5;
        return iF;
    }

    @MutableFieldReference("Analysis couldn't handle typecasts")
    private Integer iD;

    public synchronized Integer getiD(){
        if(iD==0D)
            iD = 5;
        return iD;
    }

    @MutableFieldReference("Analysis couldn't handle typecasts")
    private Integer iL;

    public synchronized Integer getiL(){
        if(iL==0L)
            iL = 5;
        return iL;
    }
}
