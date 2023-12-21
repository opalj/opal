/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package NonSerializableClassHasASerializableInnerClass;

/**
 * A Serializable outer class with a non-Serializable inner class which has various inner
 * classes itself, some Serializable, some not.
 * 
 * @author Daniel Klauer
 */
public class SerializableOuterClass implements java.io.Serializable {

    private static final long serialVersionUID = 1l;

    class NonSerializableInnerClass {

        class SerializableInnerInnerClass implements java.io.Serializable {

            private static final long serialVersionUID = 1l;
        }

        class NonSerializableInnerInnerClass {
        }
    }
}
