package org.opalj.fpcf.fixtures.immutability.reference;

import com.sun.org.apache.xalan.internal.xsltc.compiler.SyntaxTreeNode;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation;

public class Template {
    @LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation("")
    private Template _template;
    private Template _parent;

    public Template(Template parent){
        _parent = parent;
    }

    protected final Template getParent() {
        return _parent;
    }

    protected Template getTemplate() {

        if (_template == null) {
            Template parent = this;
            while (parent != null)
                parent = parent.getParent();
            _template = parent;
        }
        return _template;
    }

}