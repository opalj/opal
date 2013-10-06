package ai.taint;

public final class SourceOnCallstack {
	
	public Class<?> baz(String name) throws ClassNotFoundException { //error here
		name = bar(name);
		return foo(name);
	}	
	
	private Class<?> foo(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	public String bar(String name) {
		return name;
	}

}
