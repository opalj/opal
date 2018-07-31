/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.io.FileOutputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipInputStream;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Thomas Schlosser
 */
@SuppressWarnings("all")
public abstract class SignatureTestSubClass extends SignatureTestClass<ZipInputStream> {
	
    protected JarInputStream f1;

    @SuppressWarnings("unchecked")
    public abstract Integer m3();

    @SuppressWarnings("unchecked")
    public abstract FileOutputStream m5();
	
}
