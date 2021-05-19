/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This testclass tests that different modifiers like transient, volatile or static
 * does not have an impact of mutability.
 *
 * @author Tobias Roth
 *
 */
@MutableType("")
@MutableClass("")
public class DifferentModifier {

    @MutableField(value = "field has a mutable field reference")
    @AssignableField(value = "field is public")
    public int mutableInt = 5;

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private int immutableInt = 3;

    //@Immutable
    @MutableField(value = "field has a mutable field reference")
    @AssignableField(value = "field is public")
    public transient int mutableTransientInt = 5;

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private transient int immutableTransientInt = 5;

    //@Immutable
    @MutableField(value = "field has a mutable field reference")
    @AssignableField(value = "field is public")
    public volatile int mutableVolatileInt = 5;

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private volatile int immutableVolatileInt = 5;

    //@Immutable
    @MutableField(value = "field has a mutable field reference")
    @AssignableField(value = "field is public")
    public volatile long mutableVolatileLong;

    //@Immutable
    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private volatile long immutableVolatileLong = 0L;

    DifferentModifier(long l){
        this.mutableVolatileLong = l;
    }

      static final class InnerClass {

          @MutableField(value = "field has a mutable field reference")
          @AssignableField(value = "field is public")
          public static int mutableInnerStaticInt = 1;

          //@Immutable
          @TransitivelyImmutableField("")
          @EffectivelyNonAssignableField("")
          private static int immutableInnerStaticInt = 1;

          @MutableField(value = "field has a mutable field reference")
          @AssignableField(value = "field is public")
          public int mutableInnerInt = 5;

          //@Immutable
          @TransitivelyImmutableField("")
          @EffectivelyNonAssignableField("")
          private int immutableInnerInt = 5;

          @MutableField(value = "field has a mutable field reference")
          @AssignableField(value = "field is public")
        public transient int mutableInnerTransientInt = 5;

          //@Immutable
          @TransitivelyImmutableField("")
          @EffectivelyNonAssignableField("")
          private transient int immutableInnerTransientInt = 5;

          @MutableField(value = "field has a mutable field reference")
          @AssignableField(value = "field is public")
        public volatile int mutableInnerVolatileInt = 5;

          //@Immutable
          @TransitivelyImmutableField("")
          @EffectivelyNonAssignableField("")
          private volatile int immutableInnerVolatileInt = 5;

          @MutableField(value = "field has a mutable field reference")
          @AssignableField(value = "field is public")
        public volatile transient int mutableInnerVolatileTransientInt = 5;

          //@Immutable
          @TransitivelyImmutableField("")
          @EffectivelyNonAssignableField("")
          private volatile transient int immutableInnerVolatileTransientInt = 5;
    }
}
