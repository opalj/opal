/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.compile_time_constancy;

import org.opalj.fpcf.properties.compile_time_constancy.CompileTimeConstant;
import org.opalj.fpcf.properties.compile_time_constancy.CompileTimeVarying;
import org.opalj.fpcf.properties.static_data_usage.UsesConstantDataOnly;
import org.opalj.fpcf.properties.static_data_usage.UsesNoStaticData;
import org.opalj.fpcf.properties.static_data_usage.UsesVaryingData;

/**
 * Tests for the CompileTimeConstancy and StaticDataUsage properties.
 */
public class CompileTimeConstancy {

    @CompileTimeConstant("Simple compile time constant")
    public static final int compileTimeConstant = 5;

    @CompileTimeVarying("Uses currentTimeMillis() at time of initialization")
    public static final long notCompileTimeConstant = System.currentTimeMillis();

    public int foo = 5;

    @UsesNoStaticData("Empty method")
    private void emptyMethod(){

    }

    @UsesNoStaticData("Uses instance field only")
    private int instanceField(CompileTimeConstancy other){
        return foo + other.foo;
    }

    @UsesNoStaticData("Uses compile time constant field, but this is removed by the compiler")
    private int getConstant(){
        return compileTimeConstant;
    }

    @UsesVaryingData("Uses a field that is not compile time constant")
    private long getNonConstant(){
        return notCompileTimeConstant;
    }
}
