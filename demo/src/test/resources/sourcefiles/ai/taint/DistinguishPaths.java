package ai.taint;


public class DistinguishPaths {

	private Class<?> sink(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	public Class<?> okMethod(String name) throws ClassNotFoundException {
		Class<?> result = sink(name);
		System.out.println(result);
		Class<?> checkedResult = checkResult(null);
		return checkedResult;
	}

	public Class<?> leakingMethod(String name) throws ClassNotFoundException {
		Class<?> result = sink(name);
		Class<?> checkedResult = checkResult(result);
		return checkedResult;
	}

	private Class<?> checkResult(Class<?> result) {
		return result;
	}
}
