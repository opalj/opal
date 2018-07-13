/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package bugs;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public class UselessCode  {

    public static boolean rangeCheck(int n){
        if(n >= 0l){
            if(n >= 100l){  // this whole block is useless... the result will be
                            // the same because in the "else" branch we also return true!
                return true;
            }

            return true;
        }
        else{
            return false;
        }
    }

    public static Float unusedInstance(float f, float g){
        Float fContainer = new Float(f);
        Float gContainer = new Float(g);
        Float resultContainer = fContainer + gContainer;
        new Float(Math.sqrt(resultContainer.floatValue()));     //useless
        return resultContainer;
    }

    public Object uselessInstantiation(int i){
        Object o = new Object();    //useless
        o = null;
        if(i < 0 && o == null) {
            return new Object();
        }
        return o;
    }

    public Object uselessSimpleTest(int i){
        if (i < 10 || i > 10 ) return null;
        int j = i - 1; // => j == 9
        // we can't have an overflow here, hence, the
        // following test is useless
        if(j > i) {
            return "won't happen";
        } else {
            return "will happen";
        }
    }

    public Object uselessMoreComplexTest(int i){
        if (i < -10 || i > 10 ) return null;
        int j = i - 1;
        // we can't have an overflow here, hence, the
        // following test is useless
        if(j > i) {
            return "won't happen";
        } else {
            return "will happen";
        }
    }

    public Object uselessComputation(int i){
        // The following requires that we are able to track abstract
        // computations/constraints! In that case we can even find
        // bugs if we don't know the precise value!
        int j = i - 1;
        if(j/2 >= (i-1)/2) { // the computation is basically the same
            return "will happen";
        } else {
            return "won't happen";
        }
    }
}
