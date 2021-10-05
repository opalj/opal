/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.collection.immutable.UIDSet
import org.opalj.br.ReferenceType

/**
 * This package contains definitions of common domains that can be used for the
 * implementation of analyses.
 *
 * ==Types of Domains==
 * In general, we distinguish two types of domains. First, domains that define a
 * general interface (on top of the one defined by [[Domain]]), but do not directly
 * provide an implementation. Hence, whenever you develop a new `Domain` you should
 * consider implementing/using these domains to maximize reusability. Second,
 * `Domain`s that implement a specific interface (trait).  In this case, we further
 * distinguish between domains that provide a default implementation (per ''interface''
 * only one of these `Domain`s can be used to create a '''final `Domain`''') and
 * those that can be stacked and basically refine the overall functionality.
 *
 * '''Examples'''
 *  - Domains That Define a General Interface
 *      - [[Origin]] defines two types which domains that provide information abou the
 *      origin of a value should consider to implement.
 *      - [[TheProject]] defines a standard mechanism how a domain can access the
 *      ''current'' project.
 *      - ...
 *
 *  - Domains That Provide a Default Implementation
 *      - [[Origin]] defines the functionality to return a value's origin if the value
 *      supports that.
 *      - [[org.opalj.ai.domain.TheProject]] default implementation of the class hierarchy related
 *      methods using the project's class hierarchy.
 *      - [[org.opalj.ai.domain.DefaultHandlingOfMethodResults]] basically implements a Domain's methods
 *      related to return instructions an uncaught exceptions.
 *      - ...
 *
 *  - Domains That Implement Stackable Functionality
 *      - [[org.opalj.ai.domain.RecordThrownExceptions]] records information about all uncaught exceptions
 *      by intercepting a `Domain`'s respective methods. However, it does provide a
 *      default implementation. Hence, a typical pattern is:
 *      {{{
 *      class MyDomain extends Domain with ...
 *          with DefaultHandlingOfMethodResults with RecordThrownExceptions
 *      }}}
 *
 * ==Thread Safety==
 * Unless explicitly documented, a domain is never thread-safe. The general programming
 * model is to use one `Domain` object per code block/method and therefore, thread-safety
 * is not required for `Domain`s that are used for the evaluation of methods. However
 * domains that are used to adapt/transfer values should be thread safe
 * (see [[org.opalj.ai.domain.ValuesCoordinatingDomain]] for further details).
 *
 * @author Michael Eichberg
 */
package object domain {

    final val EmptyUpperTypeBound = UIDSet.empty[ReferenceType]

    /**
     * Tries to determine the name of the method/class that is analyzed;
     * the result depends on the mixed-in domain(s).
     */
    def analyzedEntity(domain: Domain): String = {
        domain match {
            case d: TheMethod => d.method.toJava
            case _            => "<Unknown (the domain does not provide source information)>\n"
        }
    }

}
