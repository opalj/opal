package ai.taint;

public class CallBack {
	
	public Class<?> privateMethodOverwriteable(String name) throws ClassNotFoundException {
		return Class.forName(getPublicName());
	}

	public String getPublicName() {
		return null;
	}


}
