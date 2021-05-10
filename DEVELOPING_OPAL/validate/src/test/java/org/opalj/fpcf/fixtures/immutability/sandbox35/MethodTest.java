package org.opalj.fpcf.fixtures.immutability.sandbox35;

import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;

public class MethodTest {
	//TODO @DeepImmutableField("")
	@NonTransitivelyImmutableField("")
private Object object1 = new Object();
	@NonTransitivelyImmutableField("")
private Object object2 = new Object();
	@NonTransitivelyImmutableField("")
private Mut object3 = new Mut();
	@NonTransitivelyImmutableField("")
private Mut object4 = new Mut();

public MethodTest(Object o){
	this.object2 = o;
}

public Mut get3(){
 	return object3;
}

private Mut get4(){
	return object4;
}




}

class Mut{
	public int n = 5;
}
