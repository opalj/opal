/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package call_targets;

/**
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public class SuperCalls {
    protected String[] stringValues() { return new String[]{ "String 1", "String2" }; }
}

class SubSuperCalls extends SuperCalls {

    @Override public String toString() {
        return "SubSuperClass";
    }
}

class SubSubSuperCalls extends SubSuperCalls {

    @Override protected String[] stringValues() {
        String[] sv = super.stringValues();
        sv[0] = "<EMPTY>";
        return sv;
    }

    @Override public String toString() {
        return "SubSuperClass";
    }
}
