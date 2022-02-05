public class NativeTest {
        public native int f1 (int a, int b);
        public native int f2 ();
        public native int f3 (int a);
        public native int f4 (int a);
        public native int f5 (int a, int b);
        static
        {
            System.loadLibrary ("nativetest");   /* lowercase of classname! */
        }
        public static void main (String[] args)
        {
            NativeTest demo = new NativeTest();
            int tainted = demo.source();
            demo.sink(tainted);
            demo.sink(demo.sanitize(tainted));
            int untainted = 23;
            demo.sink(untainted);
            int native_tainted = demo.f1(tainted, untainted);
            demo.sink(native_tainted);
            demo.f4(native_tainted);
            int native_source_taint = demo.f2();
            demo.f4(demo.f3(native_tainted));
            demo.sink(demo.f5(tainted, untainted));
            demo.sink(demo.f5(untainted, tainted));
            System.out.println("done");
        }
        
        public static int source()
        {
            return 42;
        }
        
        public static void sink(int a) {}
        
        public static int sanitize(int a)
        {
            return a;
        }
}
