/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package extensible_classes.transitivity.case1;

// Not extensible when this package is considered closed.
class Class { }

/**
 * This case shows in a closed package scenario that the class <code>Class</code>
 * is transitively extensiblie over this (direct) subtype.
 */
public class PublicSubclass extends Class { }
