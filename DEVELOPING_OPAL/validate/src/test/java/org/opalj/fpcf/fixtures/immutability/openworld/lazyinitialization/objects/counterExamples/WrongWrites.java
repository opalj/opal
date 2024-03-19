/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.objects.counterExamples;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
/**
 * This class encompasses several cases of writes that hinder the correct lazy initialization.
 */
public class WrongWrites {

    @AssignableField("no valid lazy initialization due to write outside pattern")
    private Integer writeOutsideDCL;

    public Integer getWriteOutsideDCL() {
        if (writeOutsideDCL == null) {
            synchronized (this) {
                if (writeOutsideDCL == null) {
                }
            }
        }
        writeOutsideDCL = new Integer(5);
        return writeOutsideDCL;
    }

    @AssignableField("no valid lazy initialization due to second write outside dcl pattern")
    private Integer alsoWrittenOutsideTheDCLPattern;

    public Integer getAlsoWrittenOutsideTheDCLPattern() {
        if(alsoWrittenOutsideTheDCLPattern ==null){
            synchronized(this) {
                if(alsoWrittenOutsideTheDCLPattern ==null){
                    alsoWrittenOutsideTheDCLPattern = new Integer(5);
                }
            }
        }
        alsoWrittenOutsideTheDCLPattern = new Integer(5);
        return alsoWrittenOutsideTheDCLPattern;
    }

    @AssignableField("no valid lazy initialization due to write outside dcl pattern")
    private Integer multipleWritesWithinTheDCLPattern;

    public Integer getMultipleWritesWithinTheDCLPattern() {
        if(multipleWritesWithinTheDCLPattern ==null){
            synchronized(this) {
                if(multipleWritesWithinTheDCLPattern ==null){
                    multipleWritesWithinTheDCLPattern = new Integer(5);
                }
            }
            multipleWritesWithinTheDCLPattern = new Integer(5);
        }
        return multipleWritesWithinTheDCLPattern;
    }
    
    @AssignableField("field write outside guards and synchronized blocks")
    private Integer writeOutsidePattern;

    public Integer getWriteOutsidePattern() {
        if(writeOutsidePattern ==null){
        }
        synchronized(this) {
        }
        if(writeOutsidePattern ==null){

        }
        writeOutsidePattern = new Integer(5);
        return writeOutsidePattern;
    }
}
