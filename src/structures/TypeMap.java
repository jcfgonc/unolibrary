package structures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class TypeMap {

	public static final int TYPE_INTEGER = 1;
	public static final int TYPE_DOUBLE = 2;
	public static final int TYPE_STRING = 3;
	public static final int TYPE_UNDEFINED = Integer.MAX_VALUE;

	private Object2IntOpenHashMap<String> nameToType;

	public TypeMap() {
		nameToType = new Object2IntOpenHashMap<>();
	}

	public void loadFromTextFile(File filename) throws IOException {
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split("[\t]+", 2);
			if (tokens.length == 0)
				continue;
			String name = tokens[0];
			int type;
			if (tokens.length > 1) {
				type = stringToType(tokens[1]);
			} else {
				type = TYPE_UNDEFINED;
			}
			nameToType.put(name, type);
		}
		reader.close();
	}

	private int stringToType(String s) {
		if (s.equals("integer") || s.equals("int")) {
			return TYPE_INTEGER;
		}
		if (s.equals("double")) {
			return TYPE_DOUBLE;
		}
		if (s.equals("string")) {
			return TYPE_STRING;
		}
		return TYPE_UNDEFINED;
	}

	public void clear() {
		nameToType.clear();
	}

	public int getType(String key) {
		if (nameToType.containsKey(key)) {
			return nameToType.getInt(key);
		} else
			return TYPE_UNDEFINED;
	}

	public boolean isTypeInteger(String key) {
		if (nameToType.containsKey(key)) {
			return getType(key) == TYPE_INTEGER;
		} else
			return false;
	}

	public boolean isTypeDouble(String key) {
		if (nameToType.containsKey(key)) {
			return getType(key) == TYPE_DOUBLE;
		} else
			return false;
	}

	public boolean isTypeString(String key) {
		if (nameToType.containsKey(key)) {
			return getType(key) == TYPE_STRING;
		} else
			return false;
	}

	public boolean isTypeUndefined(String key) {
		if (nameToType.containsKey(key)) {
			return getType(key) == TYPE_UNDEFINED;
		} else
			return false;
	}

	public boolean isTypeNumeric(String key) {
		return isTypeInteger(key) || isTypeDouble(key);
	}

}
