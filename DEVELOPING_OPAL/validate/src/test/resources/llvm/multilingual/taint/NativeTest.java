public class NativeTest {
        private native int f1 (int a, int b);
        private native int f2 ();
        private native int f3 (int a);
        private native int f4 (int a);
        private native int f5 (int a, int b);
        static
        {
            System.loadLibrary ("nativetest");   /* lowercase of classname! */
        }
        public static void main (String[] args)
        {
            NativeTest demo = new NativeTest();
            demo.test_1_flow();
            demo.test_2_no_flow();
            demo.test_3_no_flow();
            demo.test_4_flow();
            demo.test_5_flow();
            demo.test_6_no_flow();
            demo.test_7();
            demo.test_8();
            System.out.println("done");
        }

        public void test_1_flow() {
            int tainted = this.source();
            this.sink(tainted);
        }

        public void test_2_no_flow() {
            int tainted = this.source();
            this.sink(this.sanitize(tainted));
        }

        public void test_3_no_flow() {
            int untainted = 23;
            this.sink(untainted);
        }

        public void test_4_flow() {
            int tainted = this.source();
            int untainted = 23;
            int native_tainted = this.f1(tainted, untainted);
            this.sink(native_tainted);
        }

        public void test_5_flow() {
            int taint = this.f2();
            this.f4(taint);
        }

        public void test_6_no_flow() {
            this.f4(this.f3(this.f2()));
        }

        public void test_7() {
            int tainted = this.source();
            int untainted = 23;
            this.sink(this.f5(tainted, untainted));
        }

        public void test_8() {
            int tainted = this.source();
            int untainted = 23;
            this.sink(this.f5(untainted, tainted));
        }
        
        private static int source()
        {
            return 42;
        }
        
        private static void sink(int a) {}
        
        private static int sanitize(int a)
        {
            return a;
        }
}
