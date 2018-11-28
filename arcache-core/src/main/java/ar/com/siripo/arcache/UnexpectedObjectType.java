package ar.com.siripo.arcache;

/**
 * This is returned when the cached object type is not the expected
 * 
 * @author mariano
 *
 */
public class UnexpectedObjectType extends Exception {

	private static final long serialVersionUID = 1L;
	private Class<?> expectedClass;
	private Class<?> foundClass;

	public UnexpectedObjectType(Class<?> expected, Class<?> found) {
		super();
		this.expectedClass = expected;
		this.foundClass = found;
	}

	@Override
	public String toString() {
		return "UnexpectedObjectType expecting:" + expectedClass.getName() + " found:" + foundClass.getName();
	}

}
