/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package extensible_classes.transitivity.case2;

/* @author Michael Reif */

// Not extensible when this package is considered closed.
class Class { }

interface Interface { }

public final class TestCase extends Class implements Interface { }

class PackageVisibleSubclass extends Class implements Interface { }
