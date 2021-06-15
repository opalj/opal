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
 * This testclass checks that different modifiers like transient, volatile or static
 * does not have an impact of the assignability and immutability of a given field.
 *
 * @author Tobias Roth
 *
 */
@MutableType("Class is mutable")
@MutableClass("The class has mutable fields")
public class DifferentModifier {

    @MutableField("The field is assignable")
    @AssignableField("The field is public")
    public int assignableInt = 5;

    //@Immutable
    @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
    @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
    private int effectivelyNonAssignableInt = 3;

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field is public")
    public transient int assignableTransientInt = 5;

    //@Immutable
    @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
    @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
    private transient int effectivelyNonAssignableTransientInt = 5;

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field is public")
    public volatile int assignableVolatileInt = 5;

    //@Immutable
    @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
    @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
    private volatile int effectivelyNonAssignableVolatileInt = 5;

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field is public")
    public volatile long assignableVolatileLong;

    //@Immutable
    @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
    @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
    private volatile long immutableVolatileLong = 0L;

    DifferentModifier(long l){
        this.assignableVolatileLong = l;
    }

      static final class InnerClass {

          @MutableField("The field is assignable")
          @AssignableField("The field is public")
          public static int assignableInnerStaticInt = 1;

          //@Immutable
          @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
          @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
          private static int effectivelyNonAssignableInnerStaticInt = 1;

          @MutableField("The field is assignable")
          @AssignableField("The field is public")
          public int assignableInnerInt = 5;

          //@Immutable
          @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
          @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
          private int effectivelyNonAssignableInnerInt = 5;

          @MutableField("The field is assignable")
          @AssignableField("The field is public")
        public transient int assignableInnerTransientInt = 5;

          //@Immutable
          @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
          @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
          private transient int effectivelyNonAssignableInnerTransientInt = 5;

          @MutableField("The field is assignable")
          @AssignableField("The field is public")
        public volatile int assignableInnerVolatileInt = 5;

          //@Immutable
          @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
          @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
          private volatile int effectivelyNonAssignableInnerVolatileInt = 5;

          @MutableField("The field is assignable")
          @AssignableField("The field is public")
        public volatile transient int mutableInnerVolatileTransientInt = 5;

          //@Immutable
          @TransitivelyImmutableField("The field is effectively non-assignable and has a primitive type")
          @EffectivelyNonAssignableField("The field is private and effectively assigned only once")
          private volatile transient int immutableInnerVolatileTransientInt = 5;
    }
}
