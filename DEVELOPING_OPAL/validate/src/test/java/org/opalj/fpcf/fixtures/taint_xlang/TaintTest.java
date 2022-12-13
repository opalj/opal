/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.taint_xlang;

import org.opalj.fpcf.properties.taint_xlang.XlangBackwardFlowPath;

/**
 * Add VM argument to run this test: -Djava.library.path=DEVELOPING_OPAL/validate/src/test/resources/llvm/cross_language/taint
 */
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
            // force call graph analysis of indirect methods
            // otherwise their callees are not analyzed,
            // as they are only reachable through native code
            // TODO: trigger cga from within other analysis
            demo.indirect_sink(demo.indirect_sanitize(demo.indirect_source()));

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

        @XlangBackwardFlowPath({"test_java_flow", "sink"})
        public void test_java_flow() {
            System.out.println("java");
            int tainted = source();
            sink(tainted);
        }

        @XlangBackwardFlowPath({})
        public void test_java_sanitize_no_flow() {
            System.out.println("java sanitize");
            int tainted = source();
            sink(sanitize(tainted));
        }

        @XlangBackwardFlowPath({})
        public void test_java_untainted_no_flow() {
            System.out.println("java untainted");
            int untainted = 23;
            sink(untainted);
        }

        @XlangBackwardFlowPath({"test_native_sum_flow", "sink"})
        public void test_native_sum_flow() {
            System.out.println("native sum");
            int tainted = source();
            int untainted = 23;
            int native_tainted = sum(tainted, untainted);
            sink(native_tainted);
        }

        @XlangBackwardFlowPath({"test_native_to_java_to_native_flow", "Java_org_opalj_fpcf_fixtures_taint_1xlang_TaintTest_propagate_1sink", "sink"})
        public void test_native_to_java_to_native_flow() {
            System.out.println("native to java to native");
            int taint = propagate_source();
            propagate_sink(taint);
        }

        @XlangBackwardFlowPath({})
        public void test_native_to_java_to_native_sanitized_no_flow() {
            System.out.println("native to java to native sanitized");
            propagate_sink(propagate_sanitize(propagate_source()));
        }

        @XlangBackwardFlowPath({})
        public void test_native_indirect_sanitized_no_flow() {
            System.out.println("native indirect sanitized");
            int tainted = source();
            int untainted = 23;
            sink(sanitize_only_a_into_sink(tainted, untainted));
        }

        @XlangBackwardFlowPath({"test_native_indirect_flow", "Java_org_opalj_fpcf_fixtures_taint_1xlang_TaintTest_sanitize_1only_1a_1into_1sink", "sink"})
        public void test_native_indirect_flow() {
            System.out.println("native indirect");
            int tainted = source();
            int untainted = 23;
            sink(sanitize_only_a_into_sink(untainted, tainted));
        }

        @XlangBackwardFlowPath({"test_native_identity_flow", "Java_org_opalj_fpcf_fixtures_taint_1xlang_TaintTest_propagate_1identity_1to_1sink", "sink"})
        public void test_native_identity_flow() {
            System.out.println("native identity");
            propagate_identity_to_sink(source());
        }

        @XlangBackwardFlowPath({})
        public void test_native_zero_no_flow() {
            System.out.println("native zero");
            propagate_zero_to_sink(source());
        }

        @XlangBackwardFlowPath({"test_native_array_tainted_flow", "Java_org_opalj_fpcf_fixtures_taint_1xlang_TaintTest_native_1array_1tainted", "sink"})
        public void test_native_array_tainted_flow() {
            System.out.println("native array tainted");
            native_array_tainted();
        }

        @XlangBackwardFlowPath({})
        public void test_native_array_untainted_no_flow() {
            System.out.println("native array untainted");
            native_array_untainted();
        }

        @XlangBackwardFlowPath({"test_native_call_java_sink_flow", "Java_org_opalj_fpcf_fixtures_taint_1xlang_TaintTest_propagate_1to_1java_1sink", "indirect_sink", "sink"})
        public void test_native_call_java_sink_flow() {
            System.out.println("native call java sink");
            propagate_to_java_sink(source());
        }

        @XlangBackwardFlowPath({"test_native_call_java_source_flow", "sink"})
        public void test_native_call_java_source_flow() {
            System.out.println("native call java source");
            sink(propagate_from_java_source());
        }

        @XlangBackwardFlowPath({})
        public void test_native_call_java_sanitize_no_flow() {
            System.out.println("native call java sanitize");
            sink(propagate_java_sanitize(source()));
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
