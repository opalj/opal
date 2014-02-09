package de.tud.cs.st.bat.resolved

object SigTest {

    // This is the signature of a field with type java.util.LinkedList<de.tud.cs.se.flashcards.model.FlashcardObserver>
    val cts = ClassTypeSignature(Some("java/util/"), SimpleClassTypeSignature("LinkedList", Some(List(ProperTypeArgument(None, ClassTypeSignature(Some("de/tud/cs/se/flashcards/model/"), SimpleClassTypeSignature("FlashcardObserver", None), List()))))), List())

    cts match {
        case ClassTypeSignature(Some("java/util/"), SimpleClassTypeSignature("LinkedList", Some(List(ProperTypeArgument(None, ClassTypeSignature(Some("de/tud/cs/se/flashcards/model/"), SimpleClassTypeSignature("FlashcardObserver", None), List()))))), List()) ⇒ true
        case _ ⇒ false
    }

    cts match {
        case ClassTypeSignature(Some("java/util/"), SimpleClassTypeSignature("LinkedList", Some(List(ProperTypeArgument(None, BasicClassTypeSignature(ObjectType("de/tud/cs/se/flashcards/model/FlashcardObserver")))))), List()) ⇒ true
        case _ ⇒ false
    }

    cts match {
        case GenericContainer(c, t) ⇒ c.toJava+"<"+t.toJava+">"
        case _                      ⇒ "Not a generic container"
    }
}