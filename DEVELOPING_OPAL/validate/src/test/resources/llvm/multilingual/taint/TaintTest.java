public class TaintTest {
        private native int sum (int a, int b);
        private native int propagate_source ();
        private native int propagate_sanitize (int a);
        private native int propagate_sink (int a);
        private native int sanitize_only_a_into_sink (int a, int b);
        static
        {
            System.loadLibrary ("tainttest");
        }
        public static void main (String[] args)
        {
            TaintTest demo = new TaintTest();
            demo.test_1_flow();
            demo.test_2_no_flow();
            demo.test_3_no_flow();
            demo.test_4_flow();
            demo.test_5_flow();
            demo.test_6_no_flow();
            demo.test_7_no_flow();
            demo.test_8_flow();
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
            int native_tainted = this.sum(tainted, untainted);
            this.sink(native_tainted);
        }

        public void test_5_flow() {
            int taint = this.propagate_source();
            this.propagate_sink(taint);
        }

        public void test_6_no_flow() {
            this.propagate_sink(this.propagate_sanitize(this.propagate_source()));
        }

        public void test_7_no_flow() {
            int tainted = this.source();
            int untainted = 23;
            this.sink(this.sanitize_only_a_into_sink(tainted, untainted));
        }

        public void test_8_flow() {
            int tainted = this.source();
            int untainted = 23;
            this.sink(this.sanitize_only_a_into_sink(untainted, tainted));
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
