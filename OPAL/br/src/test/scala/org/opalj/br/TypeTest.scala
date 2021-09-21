/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TypeTest extends AnyFunSpec {

    describe("types") {
        it("should be properly initialized on first usage") {
            Seq(ByteType, CharType, ShortType, IntegerType, LongType, FloatType, DoubleType, BooleanType) foreach { t ⇒
                val wt = t.WrapperType
                val pt = ObjectType.primitiveType(wt)
                assert(pt.isDefined, s"primitive type lookup failed (${t.WrapperType.toJava})")
                assert(pt.get != null, s"primitive type for ${t.WrapperType.toJava} was null")
            }
        }
    }

}
