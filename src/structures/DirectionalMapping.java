package structures;

import java.util.Objects;

public class DirectionalMapping<T> {

	private T source;
	private T target;

	public DirectionalMapping(T source, T target) {
		super();
		this.source = source;
		this.target = target;
	}

	public T getSource() {
		return source;
	}

	public T getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "[source=" + source + ", target=" + target + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, target);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		DirectionalMapping<T> other = (DirectionalMapping<T>) obj;
		return Objects.equals(source, other.source) && Objects.equals(target, other.target);
	}

}
