package ai.taint;

public class MergeTest {

	public void a(String className) throws ClassNotFoundException {
		b(className);
		c(className);
	}

	private void b(String className) throws ClassNotFoundException {
		Class.forName(className);
	}

	private void c(String className) throws ClassNotFoundException {
		Class.forName(className);
	}
}
