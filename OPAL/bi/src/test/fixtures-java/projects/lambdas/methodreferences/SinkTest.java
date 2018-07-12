/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas.methodreferences;

import java.util.ArrayList;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.DYNAMIC;

/**
 * This class contains examples for method references in sink.
 *
 * <!--
 *
 *
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 *
 *
 * -->
 *
 * @author Andreas Muttscheller
 */
public class SinkTest {

    // Copied from java.util.stream.Sink
    interface Sink<T> extends java.util.function.Consumer<T> {
        default void begin(long size) {}
        default void end() {}
        default boolean cancellationRequested() {
            return false;
        }
        default void accept(int value) {
            throw new IllegalStateException("called wrong accept method");
        }
        default void accept(long value) {
            throw new IllegalStateException("called wrong accept method");
        }
        default void accept(double value) {
            throw new IllegalStateException("called wrong accept method");
        }
        interface OfInt extends Sink<Integer> {
            @Override
            void accept(int value);

            @Override
            default void accept(Integer i) {
                accept(i.intValue());
            }
        }
    }

    public static class SinkOfInt implements Sink.OfInt {
        @Override
        public void accept(int value) {

        }
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/SinkTest$SinkOfInt", name = "accept", parameterTypes = { Object.class }, line = 90)
    public static void downstreamTest() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        Sink<Integer> downstream = new SinkOfInt();
        downstream.begin(list.size());
        list.forEach(downstream::accept);
        downstream.end();
    }
}
