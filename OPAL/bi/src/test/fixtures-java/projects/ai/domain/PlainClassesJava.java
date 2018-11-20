/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

/**
 * This class' methods contain do computations related to Class objects to test the
 * ClassValues resolution.
 *
 * @author Arne Lottmann
 */
public class PlainClassesJava {

    public Class<?> staticClassValue() {
        return String.class;
    }

    public Class<?> staticPrimitveClassValue() {
        return int.class;
    }

    public Class<?> noLiteralStringInClassForName(String s) throws ClassNotFoundException {
        return Class.forName(s, false, this.getClass().getClassLoader());
    }

    public Class<?> literalStringInLongClassForName() throws ClassNotFoundException {
        return Class.forName("java.lang.Integer", false, this.getClass().getClassLoader());
    }

    public Class<?> literalStringInClassForName() throws ClassNotFoundException {
        return Class.forName("java.lang.Integer");
    }

    public Class<?> stringVariableInClassForName() throws ClassNotFoundException {
        String className = "java.lang.Integer";
        return Class.forName(className);
    }

    public Class<?> literalStringAsParameterInClassForName() throws ClassNotFoundException {
        return getClass("java.lang.Integer");
    }

    private Class<?> getClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    public Class<?> getClassOrElseObject(String className) throws ClassNotFoundException {
        if (className != null) {
            return Class.forName(className);
        }

        return Object.class;
    }

    public Object getClassByKey(int i) throws Exception {
        Class<?> c = null;
        switch (i) {
        case 0:
            c = Class.forName("java.util.Set");
            break;
        case 1:
            c = Class.forName("java.util.List");
            break;
        default:
            c = Object.class;
        }
        return c.newInstance();
    }

    public Object getClassByKeyAlt(int i) throws Exception {
        String s;
        switch (i) {
        case 0:
            s = "java.util.Set";
            break;
        case 1:
            s = "java.util.List";
            break;
        default:
            s = "java.lang.Object";
        }
        Class<?> c = Class.forName(s);
        return c.newInstance();
    }

    public Class<?> stringVariableAsParameterInClassForName() throws ClassNotFoundException {
        String className = "java.lang.Integer";
        return getClass(className);
    }
}
