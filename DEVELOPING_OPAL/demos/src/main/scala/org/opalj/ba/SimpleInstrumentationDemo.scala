/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

// the following is just a simple demo class which we are going to instrument
class SimpleInstrumentationDemo {

    def main(args: Array[String]): Unit = {
        new SimpleInstrumentationDemo().callsToString()
    }

    def callsToString(): Unit = {
        println("the length of the toString representation is: " + this.toString().length())
    }

    def returnsValue(i: Int): Int = {
        if (i % 2 == 0)
            return -1;
        else
            return 2;
    }

    def playingWithTypes(a: AnyRef): Unit = {
        a match {
            case i: Integer =>
                println("integer ")
                println(i.intValue())
            case c: java.util.Collection[?] =>
                print("some collection ")
                println(c) // let's assume that we want to know the type of c
            case s: String =>
                print("some string ")
                println(s)
        }
    }

    def endlessLoop(): Unit = {
        while {
            val t = System.currentTimeMillis
            val s = s"Juhu: $t"
            System.out.println(s)

            System.currentTimeMillis % 200 < 10
        } do ()
        System.out.println("Did it!")
    }

    def killMe1(): Unit = {
        System.out.println("kill me")
        System.out.println("at the end")
    }

    def killMe2(b: Boolean): Unit = {
        if (b) {
            System.out.println("kill me")
        }
        System.out.println("at the end")
    }

}
