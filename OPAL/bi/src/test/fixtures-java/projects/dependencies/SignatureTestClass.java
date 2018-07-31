/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Thomas Schlosser
 */
public abstract class SignatureTestClass<Q extends FilterInputStream>
	implements SignatureTestInterface<Q, String> {

    protected Q f1;

    protected List<Long> f2;

    public abstract Q m1();

    public abstract void m2(Q t, String z);

    @SuppressWarnings("unchecked")
    public abstract Integer m3();

    public abstract Q m4();

    @SuppressWarnings("unchecked")
    public abstract FileOutputStream m5();

    public abstract List<String> m6(ArrayList<Integer> p);
}
