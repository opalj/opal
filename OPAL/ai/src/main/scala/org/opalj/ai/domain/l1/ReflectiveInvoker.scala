/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.lang.reflect.InvocationTargetException

import org.opalj.br._

/**
 * Support the invocation of methods (using Java reflection) of Java objects that
 * represent concrete domain values.
 *
 * This greatly facilitates the implementation of methods that need to simulate
 * the logic of a specific object.
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
trait ReflectiveInvoker extends DefaultJavaObjectToDomainValueConversion with AsJavaObject {
    domain: Domain =>

    def warnOnFailedReflectiveCalls: Boolean = true

    /**
     * Performs a reflective call to the specified method. The declaring class needs
     * to be in the classpath of the analyzing JavaVM.
     *
     * @see [[DefaultJavaObjectToDomainValueConversion]] for details.
     *
     * @return `None` when the reflective invocation has failed.
     */
    def invokeReflective(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands
    ): Option[MethodCallResult] = {

        val (method, jReceiver, jOperands) =
            try {
                val declaredParametersCount = descriptor.parametersCount
                var operandCount = 0
                var jReceiver: Object = null
                var jOperands: List[Object] = Nil
                // Recall: the last method parameter is the top-most stack value ...
                // the receiver (if existing) is the last operand.
                operands foreach { op =>
                    operandCount += 1
                    val jObject =
                        toJavaObject(pc, op) match {
                            case Some(jObject) => jObject
                            case _             => return None /* <------- EARLY RETURN FROM METHOD */
                        }
                    if (operandCount > declaredParametersCount) {
                        // this is also the last operand
                        jReceiver = jObject
                    } else {
                        jOperands = jObject :: jOperands
                    }
                }
                val jParameterClassTypes = descriptor.parameterTypes map (_.toJavaClass)
                val method =
                    declaringClass.toJavaClass.getDeclaredMethod(
                        name, jParameterClassTypes: _*
                    )
                (method, jReceiver, jOperands)
            } catch {
                case e: ClassNotFoundException =>
                    if (warnOnFailedReflectiveCalls)
                        Console.println(
                            Console.YELLOW+
                                "[warn] calling the method \""+descriptor.toJava(name)+"\" is not possible ("+
                                e.getMessage+
                                ") class is not found on the JVM's classpath."+
                                Console.RESET
                        )
                    return None; /* <------- EARLY RETURN FROM METHOD */
                case _: NoSuchMethodException =>
                    if (warnOnFailedReflectiveCalls)
                        Console.println(
                            Console.YELLOW+
                                "[warn] the method \""+descriptor.toJava(name)+
                                "\" is not defined by the class on the JVM's class path: "+
                                declaringClass.toJava+"."+
                                Console.RESET
                        )
                    return None; /* <------- EARLY RETURN FROM METHOD */
            }

        try {
            val result = method.invoke(jReceiver, jOperands: _*)
            (descriptor.returnType.id: @scala.annotation.switch) match {
                case VoidType.id =>
                    Some(ComputationWithSideEffectOnly)
                case BooleanType.id =>
                    Some(ComputedValue(BooleanValue(
                        pc,
                        result.asInstanceOf[java.lang.Boolean].booleanValue()
                    )))
                case ByteType.id =>
                    Some(ComputedValue(ByteValue(
                        pc,
                        result.asInstanceOf[java.lang.Byte].byteValue()
                    )))
                case ShortType.id =>
                    Some(ComputedValue(ShortValue(
                        pc,
                        result.asInstanceOf[java.lang.Short].shortValue()
                    )))
                case CharType.id =>
                    Some(ComputedValue(CharValue(
                        pc,
                        result.asInstanceOf[java.lang.Character].charValue()
                    )))
                case IntegerType.id =>
                    Some(ComputedValue(IntegerValue(
                        pc,
                        result.asInstanceOf[java.lang.Integer].intValue()
                    )))
                case LongType.id =>
                    Some(ComputedValue(LongValue(
                        pc,
                        result.asInstanceOf[java.lang.Long].longValue()
                    )))
                case FloatType.id =>
                    Some(ComputedValue(FloatValue(
                        pc,
                        result.asInstanceOf[java.lang.Float].floatValue()
                    )))
                case DoubleType.id =>
                    Some(ComputedValue(DoubleValue(
                        pc,
                        result.asInstanceOf[java.lang.Double].doubleValue()
                    )))
                case _ =>
                    Some(ComputedValue(toDomainValue(pc, result)))
            }
        } catch {
            // The exception happens as part of the execution of the underlying method;
            // hence, we want to capture it and use it in the following!
            case _: NullPointerException      => Some(justThrows(NullPointerException(pc)))
            case e: InvocationTargetException => Some(justThrows(toDomainValue(pc, e.getCause())))
        }
    }
}
