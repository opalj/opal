/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package NonSerializableClassHasASerializableInnerClass;

/**
 * A non-Serializable outer class with various inner classes, some Serializable, some not.
 * 
 * @author Daniel Klauer
 */
public class NonSerializableOuterClass {

    class SerializableInnerClass implements java.io.Serializable {

        private static final long serialVersionUID = 1l;
    }

    class NonSerializableInnerClass {
    }

    static class SerializableStaticInnerClass implements java.io.Serializable {

        private static final long serialVersionUID = 3l;
    }

    static class NonSerializableStaticInnerClass {
    }
}
