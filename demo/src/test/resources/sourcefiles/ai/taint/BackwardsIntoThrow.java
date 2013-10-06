package ai.taint;

public class BackwardsIntoThrow {

	public Class<?> foo(String name) throws ClassNotFoundException {
		String checkedName = checkParam(name);
		return Class.forName(checkedName);
	}

	private String checkParam(String name) {
		if (name.length() > 5)
			throw new IllegalArgumentException();
		else
			return name;
	}
}
