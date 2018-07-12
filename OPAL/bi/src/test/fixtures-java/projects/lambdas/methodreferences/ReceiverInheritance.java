/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas.methodreferences;

import java.util.LinkedHashSet;
import java.util.function.*;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.DYNAMIC;

/**
 * This class contains examples for method references where the called method is implemented
 * by a supertype of the receiver.
 *
 * <!--
 *
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 *
 *
 * -->
 *
 * @author Andreas Muttscheller
 */
public class ReceiverInheritance {

    public static <T, R> R someBiConsumerParameter(Supplier<R> s,
            BiConsumer<R, T> bc, BiConsumer<R, R> r, T t) {
        R state = s.get();
        bc.accept(state, t);
        r.accept(state, state);

        return state;
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/LinkedHashSet", name = "<init>", parameterTypes = { }, line = 68)
    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/LinkedHashSet", name = "add", line = 69)
    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/LinkedHashSet", name = "addAll", line = 70)
    public static <T> LinkedHashSet<T> callBiConsumer(T t) {
        LinkedHashSet<T> lhm = ReceiverInheritance.<T, LinkedHashSet<T>>someBiConsumerParameter(
                LinkedHashSet::new,
                LinkedHashSet::add,
                LinkedHashSet::addAll,
                t
        );

        return lhm;
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/LinkedHashSet", name = "contains", line = 80)
    public static <T> void instanceBiConsumer(T t) {
        LinkedHashSet<T> lhm = new LinkedHashSet<T>();
        Consumer<T> bc = lhm::contains;
        bc.accept(t);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/LinkedHashSet", name = "contains", line = 86)
    public static <T> boolean instanceBiFunction(T t) {
        LinkedHashSet<T> lhm = new LinkedHashSet<T>();
        Function<T, Boolean> f = lhm::contains;
        return f.apply(t);
    }
}
