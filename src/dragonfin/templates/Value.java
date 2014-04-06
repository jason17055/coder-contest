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

	public static int asInt(Object obj)
		throws TemplateRuntimeException
	{
		if (obj == null)
			return 0;
		else if (obj instanceof Number)
		{
			return ((Number)obj).intValue();
		}
		else
		{
			try
			{
			int n = Integer.parseInt(obj.toString());
			return n;
			}
			catch (NumberFormatException e)
			{
				throw new TemplateRuntimeException("unable to convert to integer", e);
			}
		}
	}

	public static String asString(Object obj)
	{
		if (obj == null)
			return "";
		else
			return obj.toString();
	}

	public static int compare(Object a, Object b)
	{
		if (a == b)
			return 0;
		if (a instanceof Comparable) {
			@SuppressWarnings("unchecked")
			Comparable<Object> aa = (Comparable)a;
			return aa.compareTo(b);
		}
		if (b instanceof Comparable) {
			@SuppressWarnings("unchecked")
			Comparable<Object> bb = (Comparable)b;
			return -bb.compareTo(a);
		}
		throw new Error("Uncomparable objects");
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
