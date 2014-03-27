object AdaptingDomainValues {



trait X {

	trait Y
}

def f(x:X)(y :x.Y){}                              //> f: (x: AdaptingDomainValues.X)(y: x.Y)Unit



/*

    import de.tud.cs.st.util._
    import de.tud.cs.st.bat._
    import de.tud.cs.st.bat.resolved._
    import de.tud.cs.st.bat.resolved.ai._
    import de.tud.cs.st.bat.resolved.ai.domain._

    val d1 = new DefaultDomain
    val d1i = d1.newStringValue(-1, "foo.bar")

    val d2 = new DefaultDomain
    d2.isSubtypeOf(
        d1i.adapt(d2),
        ObjectType.Object,
        No)

    val d3 = new ConfigurablePreciseDomain("this")
    val d3i = d3.newStringValue(-1, "foo.bar")

    d3.isSubtypeOf(
        d1i.adapt(d3),
        ObjectType.Object,
        No)

    val d3iInD2 = d3i.adapt(d2)
    d2.isSubtypeOf(
        d3iInD2,
        ObjectType.Object,
        No)
        */
}