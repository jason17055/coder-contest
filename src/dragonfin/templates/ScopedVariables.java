package dragonfin.templates;

import java.util.*;

class ScopedVariables extends HashMap<String,Object>
{
	public ScopedVariables(Map<String,?> parentScope)
	{
		this.putAll(parentScope);
	}
}
