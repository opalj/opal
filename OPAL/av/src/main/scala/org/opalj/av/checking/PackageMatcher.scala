/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import org.opalj.br._
import org.opalj.br.analyses.SomeProject

/**
 * Matches all classes in the specified package.
 *
 * @author Marco Torsello
 */
case class PackageMatcher(
        namePredicate: NamePredicate,
        classMatcher:  ClassMatcher
)
    extends ClassLevelMatcher {

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        val packageName = classFile.thisType.packageName
        namePredicate(packageName) &&
            classMatcher.doesMatch(classFile)
    }

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        VirtualSourceElement.asVirtualSourceElements(
            project.allClassFiles filter { doesMatch(_) },
            classMatcher.matchMethods,
            classMatcher.matchFields
        )
    }
}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[PackageMatcher]]s.
 *
 * @author Marco Torsello
 */
object PackageMatcher {

    def apply(namePredicate: NamePredicate): PackageMatcher = {
        new PackageMatcher(namePredicate, AllClasses)
    }

    /**
     * Creates a [[PackageMatcher]], that relies on a [[ClassMatcher]] for matching
     *  the classType.
     *
     * @param packageName The name of a package in java notation.
     *      (I.e., "." are used to separate a package name's segments; e.g.,
     *      `java.lang.Object`).
     * @param classMatcher The [[ClassMatcher]], that will be used to match the class.
     * @param matchSubpackages If true, all packages, that start with the given package
     *      name are matched otherwise only classes declared in the given package are matched.
     *
     */
    def apply(
        packageName:      String,
        classMatcher:     ClassMatcher = AllClasses,
        matchSubpackages: Boolean      = false
    ): PackageMatcher = {
        val binaryPackageName = packageName.replace('.', '/')
        val namePredicate =
            if (matchSubpackages)
                StartsWith(binaryPackageName)
            else
                Equals(binaryPackageName)

        new PackageMatcher(namePredicate, classMatcher)
    }

    def apply(
        packageName:      String,
        matchSubpackages: Boolean
    ): PackageMatcher = {
        apply(packageName.replace('.', '/'), AllClasses, matchSubpackages)
    }

}
