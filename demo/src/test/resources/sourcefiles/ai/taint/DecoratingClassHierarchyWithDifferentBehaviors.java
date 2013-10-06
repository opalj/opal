package ai.taint;

public class DecoratingClassHierarchyWithDifferentBehaviors {

	public static interface BaseInterface {
		public Class<?> execute(String name1, String name2) throws ClassNotFoundException;
	}

	public static class SubClassA implements BaseInterface {

		private BaseInterface decoratee;

		public SubClassA(BaseInterface decoratee) {
			this.decoratee = decoratee;
		}

		@Override
		public Class<?> execute(String name1, String name2) throws ClassNotFoundException {
			return decoratee.execute("constant", name2);
		}

	}

	private static class SubClassB implements BaseInterface {

		private BaseInterface decoratee;

		private SubClassB(BaseInterface decoratee) {
			this.decoratee = decoratee;
		}

		@Override
		public Class<?> execute(String name1, String name2) throws ClassNotFoundException {
			return decoratee.execute(name2, name1);
		}
	}

	public static class SubClassC implements BaseInterface {

		@Override
		public Class<?> execute(String name1, String name2) throws ClassNotFoundException {
			return Class.forName(name1);
		}
	}
}
