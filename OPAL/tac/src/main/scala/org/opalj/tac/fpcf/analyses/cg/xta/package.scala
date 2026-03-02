/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.ClassHierarchy
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.Project
import org.opalj.fpcf.Entity

/**
 * @author Andreas Bauer
 */
package object xta {
    /**
     * Type alias for Entity/AnyRef for better code comprehension.
     * Within the context of propagation-based call graph construction algorithms,
     * each entity has a corresponding "set entity". The type set of an entity is
     * attached to its set entity. The set entity may be itself, or some other entity.
     * The set of a single set entity may be shared among multiple entities.
     * The assignment of entity to set entity varies per algorithm. For example,
     * in XTA, the set entity of a DefinedMethod is the DefinedMethod itself, but
     * in CTA, the set entity is the method's class.
     */
    type TypeSetEntity = Entity

    /**
     * Checks whether a given candidate type is compatible to a given filter type in terms of XTA type propagation.
     * @param candidateType The candidate type to check
     * @param filterType The type to check compatibility to
     * @param classHierarchy The current class hierarchy
     * @param project The current project
     * @return True if the types match, false if not
     */
    def candidateMatchesTypeFilter(candidateType: ReferenceType, filterType: ReferenceType)(
        implicit
        classHierarchy: ClassHierarchy,
        project:        Project[?]
    ): Boolean = {
        val answer = classHierarchy.isASubtypeOf(candidateType, filterType)

        if (answer.isYesOrNo) {
            // Here, we know for sure that the candidate type is or is not a subtype of the filter type.
            answer.isYes
        } else {
            // If the answer is Unknown, we don't know for sure whether the candidate is a subtype of the filter type.
            // However, ClassHierarchy returns Unknown even for cases where it is very unlikely that this is the case.
            // Therefore, we take some more features into account to make the filtering more precise.

            // Important: This decision is a possible but unlikely cause of unsoundness in the call graph!

            // If the filter type is not a project type (i.e., it is external), we assume that any candidate type
            // is a subtype. This can be any external type or project types for which we have incomplete supertype
            // information.
            // If the filter type IS a project type, we consider the candidate type not to be a subtype since this is
            // very likely to be not the case. For the candidate type, there are two options: Either it is an external
            // type, in which case the candidate type could only be a subtype if project types are available in the
            // external type's project at compile time. This is very unlikely since external types are almost always
            // from libraries (like the JDK) which are not available in the analysis context, and which were almost
            // certainly compiled separately ("Separate Compilation Assumption").
            // The other option is that the candidate is also a project type, in which case we should have gotten a
            // definitive Yes/No answer before. Since we didn't get one, the candidate type probably has a supertype
            // which is not a project type. In that case, the above argument applies similarly.

            val filterTypeIsProjectType = if (filterType.isClassType) {
                project.isProjectType(filterType.asClassType)
            } else {
                val at = filterType.asArrayType
                project.isProjectType(at.elementType.asClassType)
            }

            !filterTypeIsProjectType
        }
    }
}
