public class TaintTest {
        private native int sum (int a, int b);
        private native int propagate_source ();
        private native int propagate_sanitize (int a);
        private native int propagate_sink (int a);
        private native int sanitize_only_a_into_sink (int a, int b);
        private native void propagate_identity_to_sink(int a);
        private native void propagate_zero_to_sink(int a);
        private native void native_array_tainted();
        private native void native_array_untainted();
        private native void propagate_to_java_sink(int a);
        private native int propagate_from_java_source();
        private native int propagate_java_sanitize(int a);
        static
        {
            System.loadLibrary ("tainttest");
        }
        public static void main (String[] args)
        {
            TaintTest demo = new TaintTest();
            demo.test_java_flow();
            demo.test_java_sanitize_no_flow();
            demo.test_java_untainted_no_flow();
            demo.test_native_sum_flow();
            demo.test_native_to_java_to_native_flow();
            demo.test_native_to_java_to_native_sanitized_no_flow();
            demo.test_native_indirect_sanitized_no_flow();
            demo.test_native_indirect_flow();
            demo.test_native_identity_flow();
            demo.test_native_zero_no_flow();
            demo.test_native_array_tainted_flow();
            demo.test_native_array_untainted_no_flow();
            demo.test_native_call_java_sink_flow();
            demo.test_native_call_java_source_flow();
            demo.test_native_call_java_sanitize_no_flow();
            System.out.println("done");
        }

        public void test_java_flow() {
            System.out.println("java");
            int tainted = this.source();
            this.sink(tainted);
        }

        public void test_java_sanitize_no_flow() {
            System.out.println("java sanitize");
            int tainted = this.source();
            this.sink(this.sanitize(tainted));
        }

        public void test_java_untainted_no_flow() {
            System.out.println("java untainted");
            int untainted = 23;
            this.sink(untainted);
        }

        public void test_native_sum_flow() {
            System.out.println("native sum");
            int tainted = this.source();
            int untainted = 23;
            int native_tainted = this.sum(tainted, untainted);
            this.sink(native_tainted);
        }

        public void test_native_to_java_to_native_flow() {
            System.out.println("native to java to native");
            int taint = this.propagate_source();
            this.propagate_sink(taint);
        }

        public void test_native_to_java_to_native_sanitized_no_flow() {
            System.out.println("native to java to native sanitized");
            this.propagate_sink(this.propagate_sanitize(this.propagate_source()));
        }

        public void test_native_indirect_sanitized_no_flow() {
            System.out.println("native indirect sanitized");
            int tainted = this.source();
            int untainted = 23;
            this.sink(this.sanitize_only_a_into_sink(tainted, untainted));
        }

        public void test_native_indirect_flow() {
            System.out.println("native indirect");
            int tainted = this.source();
            int untainted = 23;
            this.sink(this.sanitize_only_a_into_sink(untainted, tainted));
        }

        public void test_native_identity_flow() {
            System.out.println("native identity");
            this.propagate_identity_to_sink(source());
        }

        public void test_native_zero_no_flow() {
            System.out.println("native zero");
            this.propagate_zero_to_sink(source());
        }

        public void test_native_array_tainted_flow() {
            System.out.println("native array tainted");
            this.native_array_tainted();
        }

        public void test_native_array_untainted_no_flow() {
            System.out.println("native array untainted");
            this.native_array_untainted();
        }

        public void test_native_call_java_sink_flow() {
            System.out.println("native call java sink");
            this.propagate_to_java_sink(source());
        }

        public void test_native_call_java_source_flow() {
            System.out.println("native call java source");
            this.sink(this.propagate_from_java_source());
        }

        public void test_native_call_java_sanitize_no_flow() {
            System.out.println("native call java sanitize");
            this.sink(this.propagate_java_sanitize(this.source()));
        }

        public int indirect_source() {
            return source();
        }

        public void indirect_sink(int a) {
            sink(a);
        }

        public int indirect_sanitize(int a) {
            return sanitize(a);
        }
        
        private static int source()
        {
            return 42;
        }
        
        private static void sink(int a) {
            System.out.println("java " + a);
        }
        
        private static int sanitize(int a)
        {
            return a - 19;
        }
}
