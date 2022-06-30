/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject

import scala.collection.mutable

/**
 * Provides the context in which a method was invoked or an object was allocated.
 *
 * @author Dominik Helm
 */
trait Context {
    val hasContext: Boolean = true

    /** The method itself */
    def method: DeclaredMethod

    /** An identifier for the context */
    def id: Int
}

object Context {
    def unapply(context: Context): Option[DeclaredMethod] = {
        if (context.hasContext) Some(context.method)
        else None
    }
}

/**
 *  Represents unknown contexts.
 */
case object NoContext extends Context {
    override val hasContext: Boolean = false

    override def method: DeclaredMethod = throw new UnsupportedOperationException()

    val id: Int = -1
}

/**
 * A simple context that provides the bare minumum for context-insensitive analyses.
 */
case class SimpleContext private[properties] (method: DeclaredMethod) extends Context {
    override def id: Int = method.id
}

object SimpleContextsKey extends ProjectInformationKey[SimpleContexts, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys =
        Seq(DeclaredMethodsKey)

    override def compute(p: SomeProject): SimpleContexts = {
        new SimpleContexts(p.get(DeclaredMethodsKey))
    }

}

class SimpleContexts private[properties] (declaredMethods: DeclaredMethods) {

    @volatile private var id2Context = new Array[SimpleContext](declaredMethods._UNSAFE_size)

    def apply(method: DeclaredMethod): SimpleContext = {
        val id = method.id
        if (id < id2Context.length) {
            var context = id2Context(id)
            if (context ne null) context
            else {
                synchronized {
                    context = id2Context(id)
                    if (context ne null) context
                    else {
                        val newContext = SimpleContext(method)
                        id2Context(id) = newContext
                        newContext
                    }
                }
            }
        } else {
            synchronized {
                if (id < id2Context.length) {
                    val context = id2Context(id)
                    if (context ne null) context
                    else {
                        val newContext = SimpleContext(method)
                        id2Context(id) = newContext
                        newContext
                    }
                } else {
                    val newContext = SimpleContext(method)
                    val newMap = java.util.Arrays.copyOf(
                        id2Context, Math.max(declaredMethods._UNSAFE_size, id + 1)
                    )
                    newMap(id) = newContext
                    id2Context = newMap
                    newContext
                }
            }
        }
    }
}

/**
 * A context that includes a call string
 */
class CallStringContext private[properties] (
        val id:         Int,
        val method:     DeclaredMethod,
        val callString: List[(DeclaredMethod, Int)]
) extends Context {
    override def toString: String = {
        s"CallStringContext($method, $callString)"
    }
}

object CallStringContextsKey extends ProjectInformationKey[CallStringContexts, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys =
        Seq(DeclaredMethodsKey)

    override def compute(p: SomeProject): CallStringContexts = {
        new CallStringContexts()
    }

}

class CallStringContexts {

    @volatile private var id2Context = new Array[CallStringContext](32768)
    private val context2id = new mutable.HashMap[(DeclaredMethod, List[(DeclaredMethod, Int)]), CallStringContext]()

    private val nextId = new AtomicInteger(1)
    private val rwLock = new ReentrantReadWriteLock()

    def apply(id: Int): CallStringContext = {
        id2Context(id)
    }

    def apply(
        method:     DeclaredMethod,
        callString: List[(DeclaredMethod, Int)]
    ): CallStringContext = {
        val key = (method, callString)

        val readLock = rwLock.readLock()
        readLock.lock()
        try {
            val contextO = context2id.get(key)
            if (contextO.isDefined) {
                return contextO.get;
            }
        } finally {
            readLock.unlock()
        }

        val writeLock = rwLock.writeLock()
        writeLock.lock()
        try {
            val contextO = context2id.get(key)
            if (contextO.isDefined) {
                return contextO.get;
            }

            val context = new CallStringContext(nextId.getAndIncrement(), method, callString)
            context2id.put(key, context)
            val curMap = id2Context
            if (context.id < curMap.length) {
                curMap(context.id) = context
            } else {
                val newMap = java.util.Arrays.copyOf(curMap, curMap.length * 2)
                newMap(context.id) = context
                id2Context = newMap
            }
            context
        } finally {
            writeLock.unlock()
        }
    }
}