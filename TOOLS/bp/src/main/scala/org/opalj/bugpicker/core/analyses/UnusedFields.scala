/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.ConstantString
import org.opalj.br.analyses.StringConstantsInformation
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.issues.Issue
import org.opalj.issues.Relevance
import org.opalj.issues.IssueKind
import org.opalj.issues.IssueCategory
import org.opalj.issues.FieldLocation
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.fpcf.PropertyStore

/**
 * Identifies fields (static or instance) that are not used and which are also not useable.
 *
 * @author Michael Eichberg
 */
object UnusedFields {

    def apply(
        theProject:                 SomeProject,
        propertyStore:              PropertyStore,
        fieldAccessInformation:     FieldAccessInformation,
        stringConstantsInformation: StringConstantsInformation,
        classFile:                  ClassFile
    ): Seq[Issue] = {

        val typeExtensibility = theProject.get(TypeExtensibilityKey)

        val candidateFields = classFile.fields.filterNot { field =>
            (field.isSynthetic) ||
                // These fields are inlined by compilers; hence, even if the field is not accessed
                // it may be used in the source code.
                (field.isFinal && (field.fieldType.isBaseType || field.fieldType == ObjectType.String)) ||
                // The field is read at least once...
                (fieldAccessInformation.readAccesses(field).nonEmpty) ||
                // We may have some users of the field in the future...
                // IMPROVE use FutureFieldAccess property (TBD) to get the information if we may have future field accesses
                (!field.isPrivate && AnalysisModes.isLibraryLike(theProject.analysisMode))
        }

        if (candidateFields.isEmpty)
            return Nil;

        val unusedFields = candidateFields.filterNot { field =>
            // Test if the field defines a (probably inlined) constant string.
            field.isFinal && (field.fieldType eq ObjectType.String) &&
                {
                    field.constantFieldValue match {
                        case Some(ConstantString(value)) =>
                            stringConstantsInformation.get(value).isDefined
                        case _ =>
                            false
                    }
                }
        }

        val unusedAndNotReflectivelyAccessedFields = unusedFields.filterNot { field =>
            // Let's test if we can find:
            //  - the field's name,
            //  - or the simpleName followed by the field's name
            //  - or the fully qualified name followed by the field's name
            // in the code; if so we assume that the field is reflectively accessed
            // and we ignore it
            val fieldName = field.name
            stringConstantsInformation.get(fieldName).isDefined || {
                val thisSimpleTypeName = classFile.thisType.simpleName.replace('$', '.')
                val qualifiedFieldName = thisSimpleTypeName + '.' + fieldName
                stringConstantsInformation.get(qualifiedFieldName).isDefined
            } || {
                val thisFullyQualifiedTypeName = classFile.thisType.toJava.replace('$', '.')
                val fullyQualifiedFieldName = thisFullyQualifiedTypeName + '.' + fieldName
                stringConstantsInformation.get(fullyQualifiedFieldName).isDefined
            }
        }

        val unusedAndUnusableFields = {
            val analysisMode = theProject.analysisMode
            if (AnalysisModes.isApplicationLike(analysisMode)) {
                unusedAndNotReflectivelyAccessedFields
            } else if (analysisMode == AnalysisModes.OPA) {
                // Only private fields cannot be accessed by classes that access the currently
                // analyzed library.
                unusedAndNotReflectivelyAccessedFields.filter(_.isPrivate)
            } else if (analysisMode == AnalysisModes.CPA) {
                unusedAndNotReflectivelyAccessedFields.filter(f =>
                    f.isPrivate || f.isPackagePrivate || {
                        f.isProtected && typeExtensibility(classFile.thisType).isNo
                    })
            } else {
                val message = s"the analysis mode $analysisMode is unknown"
                OPALLogger.error("unused fields analysis", message)(GlobalLogContext)
                Nil
            }
        }

        for (unusedField <- unusedAndUnusableFields) yield {
            Issue(
                "UnusedField",
                Relevance.DefaultRelevance,
                s"the field ${unusedField.toJava} is unused",
                Set(IssueCategory.Correctness, IssueCategory.Comprehensibility),
                Set(IssueKind.UnusedField),
                List(new FieldLocation(None, theProject, classFile, unusedField))
            )
        }
    }
}
