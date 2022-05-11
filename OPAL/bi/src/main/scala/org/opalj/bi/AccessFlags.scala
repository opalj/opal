/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

/**
 * Convenience methods related to access flags.
 */
object AccessFlags {

    final val ACC_PUBLIC_STATIC /*:Int*/ = ACC_PUBLIC.mask | ACC_STATIC.mask

    final val ACC_NATIVE_VARARGS /*:Int*/ = ACC_NATIVE.mask | ACC_VARARGS.mask

    final val ACC_SYNTHETIC_PUBLIC_SUPER = ACC_SYNTHETIC.mask | ACC_PUBLIC.mask | ACC_SUPER.mask

    final val ACC_SYNTHETIC_STATIC_PUBLIC = ACC_SYNTHETIC.mask | ACC_STATIC.mask | ACC_PUBLIC.mask

    final val ACC_SYNTHETIC_STATIC_PRIVATE = ACC_SYNTHETIC.mask | ACC_STATIC.mask | ACC_PRIVATE.mask

    /**
     * Returns the names of the access flags set in a respective vector.
     */
    def toStrings(accessFlags: Int, ctx: AccessFlagsContext): Iterator[String] = {
        AccessFlagsIterator(accessFlags, ctx) map { accessFlag =>
            accessFlag.javaName.getOrElse("/*"+accessFlag.toString+"*/")
        }
    }

    def toString(accessFlags: Int, ctx: AccessFlagsContext): String = {
        toStrings(accessFlags, ctx).mkString(" ")
    }

    def classFlagsToJava(accessFlags: Int): String = {
        // ACC_PUBLIC,ACC_FINAL,ACC_SUPER,ACC_INTERFACE,ACC_ABSTRACT,
        // ACC_SYNTHETIC,ACC_ANNOTATION,ACC_ENUM,ACC_MODULE

        if (ACC_MODULE.unapply(accessFlags))
            return "module";

        var flags = List.empty[String]

        if (ACC_SYNTHETIC.unapply(accessFlags)) {
            flags ::= "/*synthetic*/"
        }

        if (ACC_PUBLIC.unapply(accessFlags)) {
            flags ::= "public"
        }

        if (ACC_FINAL.unapply(accessFlags)) {
            flags ::= "final"
        }

        if (ACC_ABSTRACT.unapply(accessFlags) && !ACC_INTERFACE.unapply(accessFlags))
            flags ::= "abstract"

        if (ACC_INTERFACE.unapply(accessFlags)) {
            if (!ACC_ABSTRACT.unapply(accessFlags))
                flags ::= "/*NOT abstract (specification violation)*/"

            if (ACC_ANNOTATION.unapply(accessFlags))
                flags ::= "@interface"
            else
                flags ::= "interface"
        }

        if (!ACC_SUPER.unapply(accessFlags) && !ACC_MODULE.unapply(accessFlags))
            flags ::= "/*super bit NOT set*/"

        if (ACC_ENUM.unapply(accessFlags))
            flags ::= "enum"
        else if (!ACC_INTERFACE.unapply(accessFlags))
            flags ::= "class"

        flags.reverse.mkString(" ")
    }

}
