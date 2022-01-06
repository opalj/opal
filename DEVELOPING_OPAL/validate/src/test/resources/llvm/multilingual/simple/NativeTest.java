public class NativeTest {
        public native int nativeFunction (int a, int b);
        public native int foo ();
        static
        {
            System.loadLibrary ("nativetest");   /* lowercase of classname! */
        }
        public static void main (String[] args)
        {
            NativeTest demo = new NativeTest();
            System.out.println("result is: " + demo.nativeFunction(23,42));
            System.out.println("foo returns: " + demo.foo());
        }
}
