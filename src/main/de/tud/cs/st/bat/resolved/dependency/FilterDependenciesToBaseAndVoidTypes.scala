/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat.resolved
package dependency

import DependencyType._

/**
 * If you do not want to have dependencies to base and void types, this trait can be mixed in.
 *
 * @author Thomas Schlosser
 */
trait FilterDependenciesToBaseAndVoidTypes extends DependencyBuilder {

    /**
     * Pseudo ID that is used to handle the filtering as cooperation between
     * <code>getID</code> and <code>addDependency</code>.
     */
    private val ID_OF_FILTERED = Int.MinValue

    /**
     * Filters ID lookups to base and void types.
     * For all other types, the ID that is determined by the class this trait is mixed in, is returned.
     *
     * @param t The type, an unique ID should be returned for.
     * @return Value of <code>ID_OF_FILTERED</code> if the given type is a base or void type.
     *         Otherwise, the result of super's <code>getID(Type)</code> method is returned.
     */
    abstract override def getID(t: Type): Int = {
        if (t.isBaseType || t.isVoidType)
            return ID_OF_FILTERED
        super.getID(t)
    }

    /**
     * Filters dependencies from/to base and void types.
     * These types are recognized by comparing the given IDs with the
     * value of the pseudo filter ID <code>ID_OF_FILTERED</code>.
     * Unfiltered dependencies are passed to the super's <code>addDependency</code> method.
     *
     * @param src The ID of the source node.
     * @param trgt The ID of the target node.
     * @param dType The type of the dependency between source and target.
     */
    abstract override def addDependency(src: Int, trgt: Int, dType: DependencyType) {
        if (src == ID_OF_FILTERED || trgt == ID_OF_FILTERED)
            return
        super.addDependency(src, trgt, dType)
    }
}