/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.IntTrieSet

/**
 * For a given [[org.opalj.br.DeclaredMethod]] the incomplete call sites related to the
 * java.lang.Thread API.
 *
 * @author Florian Kuebler
 */
sealed trait ThreadRelatedIncompleteCallSitesPropertyMetaInformation
    extends DirectCalleesPropertyMetaInformation {

    final type Self = ThreadRelatedIncompleteCallSites
}

sealed trait ThreadRelatedIncompleteCallSites extends CalleesLike
    with ThreadRelatedIncompleteCallSitesPropertyMetaInformation {

    override def toString: String = {
        s"ThreadRelatedIncompleteCallSites(size=${this.size})"
    }

    final def key: PropertyKey[ThreadRelatedIncompleteCallSites] = ThreadRelatedIncompleteCallSites.key
}

sealed class ThreadRelatedIncompleteCallSitesImplementation(
        val incompleteCallSites: IntTrieSet
) extends {
    // here we need either the early definition or would need to mark it as a lazy val.
    protected[this] val calleesIds: IntMap[IntTrieSet] = IntMap.empty
} with AbstractCalleesLike with ThreadRelatedIncompleteCallSites

object NoThreadRelatedIncompleteCallSites
    extends ThreadRelatedIncompleteCallSitesImplementation(IntTrieSet.empty)

object NoThreadRelatedIncompleteCallSitesDueToNotReachableMethod
    extends CalleesLikeNotReachable with ThreadRelatedIncompleteCallSites

object ThreadRelatedIncompleteCallSites extends ThreadRelatedIncompleteCallSitesPropertyMetaInformation {

    final val key: PropertyKey[ThreadRelatedIncompleteCallSites] = {
        val name = "opalj.ThreadRelatedIncompleteCallSites"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    NoThreadRelatedIncompleteCallSitesDueToNotReachableMethod
                case _ ⇒
                    throw new IllegalStateException(s"No analysis is scheduled for property: $name")
            }
        )
    }

    def apply(incompleteCallSites: IntTrieSet): ThreadRelatedIncompleteCallSites = {
        if (incompleteCallSites.isEmpty)
            NoThreadRelatedIncompleteCallSites
        else
            new ThreadRelatedIncompleteCallSitesImplementation(incompleteCallSites)
    }
}
