/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Thomas Schlosser
 */
public interface SignatureTestInterface<T extends InputStream, Z> {

    public T m1();

    public void m2(T t, Z z);

    public <W> W m3();

    public <W extends T> W m4();

    public <W extends OutputStream> W m5();
}
