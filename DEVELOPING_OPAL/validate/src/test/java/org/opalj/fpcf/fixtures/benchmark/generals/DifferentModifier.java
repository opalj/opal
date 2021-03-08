/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

/**
 * This testclass tests that different modifiers like transient, volatile or static
 * does not have an impact of mutability.
 *
 * @author Tobias Roth
 *
 */
public class DifferentModifier {

    @MutableField(value = "field has a mutable field reference")
    @MutableFieldReference(value = "field is public")
    public int mutableInt = 5;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private int immutableInt = 3;

    @MutableField(value = "field has a mutable field reference")
    @MutableFieldReference(value = "field is public")
    public transient int mutableTransientInt = 5;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private transient int immutableTransientInt = 5;

    @MutableField(value = "field has a mutable field reference")
    @MutableFieldReference(value = "field is public")
    public volatile int mutableVolatileInt = 5;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private volatile int immutableVolatileInt = 5;

    @MutableField(value = "field has a mutable field reference")
    @MutableFieldReference(value = "field is public")
    public volatile long mutableVolatileLong;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private volatile long immutableVolatileLong = 0L;

    DifferentModifier(long l){
        this.mutableVolatileLong = l;
    }

      static final class InnerClass {

          @MutableField(value = "field has a mutable field reference")
          @MutableFieldReference(value = "field is public")
          public static int mutableInnerStaticInt = 1;

          //@Immutable
          @DeepImmutableField("")
          @ImmutableFieldReference("")
          private static int immutableInnerStaticInt = 1;

          @MutableField(value = "field has a mutable field reference")
          @MutableFieldReference(value = "field is public")
          public int mutableInnerInt = 5;

          //@Immutable
          @DeepImmutableField("")
          @ImmutableFieldReference("")
          private int immutableInnerInt = 5;

          @MutableField(value = "field has a mutable field reference")
          @MutableFieldReference(value = "field is public")
        public transient int mutableInnerTransientInt = 5;

          //@Immutable
          @DeepImmutableField("")
          @ImmutableFieldReference("")
          private transient int immutableInnerTransientInt = 5;

          @MutableField(value = "field has a mutable field reference")
          @MutableFieldReference(value = "field is public")
        public volatile int mutableInnerVolatileInt = 5;

          //@Immutable
          @DeepImmutableField("")
          @ImmutableFieldReference("")
          private volatile int immutableInnerVolatileInt = 5;

          @MutableField(value = "field has a mutable field reference")
          @MutableFieldReference(value = "field is public")
        public volatile transient int mutableInnerVolatileTransientInt = 5;

          //@Immutable
          @DeepImmutableField("")
          @ImmutableFieldReference("")
          private volatile transient int immutableInnerVolatileTransientInt = 5;
    }
}
