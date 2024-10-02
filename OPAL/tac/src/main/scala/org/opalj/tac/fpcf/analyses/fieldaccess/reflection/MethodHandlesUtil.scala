/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess
package reflection

import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject

object MethodHandlesUtil {

    private[reflection] def retrieveMatchersForMethodHandleConst(
        declaringClass: ReferenceType,
        name:           String,
        fieldType:      FieldType,
        isStatic:       Boolean
    )(implicit project: SomeProject): Set[FieldMatcher] = {
        Set(
            new LBTypeBasedFieldMatcher(fieldType),
            new NameBasedFieldMatcher(Set(name)),
            if (isStatic) StaticFieldMatcher else NonStaticFieldMatcher,
            if (declaringClass.isArrayType)
                new ClassBasedFieldMatcher(Set(ObjectType.Object), onlyFieldsExactlyInClass = false)
            else if (!isStatic)
                new ClassBasedFieldMatcher(
                    project.classHierarchy.allSubtypes(declaringClass.asObjectType, reflexive = true),
                    onlyFieldsExactlyInClass = false
                )
            else
                new ClassBasedFieldMatcher(Set(declaringClass.asObjectType), onlyFieldsExactlyInClass = false)
        )
    }
}
