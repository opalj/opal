/* BSD 2-Clause License - see OPAL/LICENSE for details. */
//package org.opalj
//package fpcf
//package analyses
//
//import org.opalj.br.analyses.ProjectInformationKey
//import org.opalj.br.analyses.PropertyStoreKey
//import org.opalj.fpcf.FPCFAnalysesManagerKey
//import org.opalj.br.analyses.SomeProject
//
///**
// * The ''key'' object to get information about the classes that can be instantiated
// * (either directly or indirectly).
// *
// * @example
// *      To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and pass in
// *      `this` object.
// *
// * @author Michael Reif
// */
//object InstantiableClassesIndexKey extends ProjectInformationKey[InstantiableClassesIndex] {
//
//    /**
//     * The [[InstantiableClassesIndex]] has no special prerequisites.
//     *
//     * @return `Nil`.
//     */
//    override protected def requirements = Seq(FPCFAnalysesManagerKey, PropertyStoreKey)
//
//    /**
//     * Computes the information which classes are (not) instantiable.
//     */
//    override protected def compute(project: SomeProject): InstantiableClassesIndex = {
//        InstantiableClassesIndex(project)
//    }
//}
