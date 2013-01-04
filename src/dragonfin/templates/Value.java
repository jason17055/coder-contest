package dragonfin.templates;

public class Value
{
	private Value() {}

	public static boolean asBoolean(Object obj)
	{
		if (obj == null)
			return false;

		if (obj instanceof Boolean)
		{
			return ((Boolean)obj).booleanValue();
		}
		if (obj instanceof Number)
		{
			return ((Number)obj).doubleValue() != 0.0;
		}

		String s = obj.toString();
		return s.length() != 0;
	}

	public static String asString(Object obj)
	{
		if (obj == null)
			return "";
		else
			return obj.toString();
	}

	public static boolean checkEquality(Object a, Object b)
	{
		if (a == b)
			return true;
		else if (a != null)
			return a.equals(b);
		else
			return false;
	}
}
