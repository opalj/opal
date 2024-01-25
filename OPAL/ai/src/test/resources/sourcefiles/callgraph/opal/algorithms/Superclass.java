/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.opal.algorithms;

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * <!-- INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * -->
 * 
 * @author Michael Reif
 */

public class Superclass {

    private SubclassLevel1 someSubtype;
    private Superclass top;

    public Superclass() {
        someSubtype = new SubclassLevel2();
        top = new SubclassLevel2();
    }

    public void implementedInEachSubclass() {
        this.toString();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 92, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 92, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 92),

            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 93, isContainedIn = {}),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 93, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 93),

            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 94, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 94, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 94), })
    public void callMethods() {
        getInstance().implementedInEachSubclass();
        someSubtype.implementedInEachSubclass();
        top.implementedInEachSubclass();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 111, isContainedIn = { CallGraphAlgorithm.CHA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 111, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/AltSubclassLevel2", name = "implementedInEachSubclass", line = 111, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA,
                    CallGraphAlgorithm.DefaultVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 111) })
    public void testInstanceOfExtVTABranchElimination() {
        Superclass field = null;
        if (getInstance() instanceof AltSubclassLevel2)
            field = new AltSubclassLevel2();
        else
            field = this.someSubtype;
        field.implementedInEachSubclass();
    }

    private Superclass getInstance() {
        return top;
    }
}
