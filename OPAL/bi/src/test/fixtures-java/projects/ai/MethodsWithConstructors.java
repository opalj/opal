/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * Methods that contain constructors and inter-constructor calls. framework.
 * 
 * @author Michael Eichberg
 */
public class MethodsWithConstructors {

    public static class MWCSuper {
        private String value;

        public MWCSuper(String value) {
            super();
            this.value = value;
        }

        public MWCSuper(int value) {
            this("This is an int value: " + value);
        }

        public String getValue() {
            return value;
        }
    }

    public static class MWCSub extends MWCSuper {

        private Object object;

        public MWCSub(Object o) {
            super(o.toString());
        }

        public Object getObject() {
            return object;
        }
    }
}
