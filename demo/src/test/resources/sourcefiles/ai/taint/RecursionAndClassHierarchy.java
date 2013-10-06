package ai.taint;

import java.util.Random;

public class RecursionAndClassHierarchy {

	public static interface BaseInterface {
		public Class<?> execute(String name) throws ClassNotFoundException;
	}

	public static class SubClassA implements BaseInterface {

		public SubClassA decoratee;

		@Override
		public Class<?> execute(String name) throws ClassNotFoundException {
			if (new Random().nextBoolean())
				return decoratee.execute(name);
			else
				return decoratee.execute(name);
		}

	}

	public static class SubClassB extends SubClassA {

		public BaseInterface decoratee;

		@Override
		public Class<?> execute(String name) throws ClassNotFoundException {
			if (new Random().nextBoolean())
				return decoratee.execute(name);
			else
				return decoratee.execute(name);
		}
	}

	public static class SubClassC extends SubClassA {

		@Override
		public Class<?> execute(String name) throws ClassNotFoundException {
			return Class.forName(name);
		}
	}
}
