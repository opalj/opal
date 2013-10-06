package ai.taint;

public class WhiteboardGraph {

	public Class<?> vulnerable(String s3) throws ClassNotFoundException {
		String s2 = checkParam(s3);
		Class<?> r2 = loadIt(s2);
		Class<?> r3 = checkReturn(r2);
		return r3;
	}

	private Class<?> checkReturn(Class<?> y) {
		return y;
	}

	private Class<?> loadIt(String s) throws ClassNotFoundException {
		Class<?> ret = Class.forName(s);
		return ret;
	}

	private String checkParam(String x) {
		return x;
	}

	private Class<?> notVulnerable(String s3) throws ClassNotFoundException {
		if (s3.length() > 10) {
			Class<?> r1 = loadIt(s3);
			r1 = null;
			return r1;
		} else {
			Class<?> r2 = loadIt("constant");
			return r2;
		}
	}
	
	public Class<?> notVulnerableWrapper(String s3) throws ClassNotFoundException {
		return notVulnerable(s3);
	}
}
