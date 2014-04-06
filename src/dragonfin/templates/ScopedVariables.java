package dragonfin.templates;

import java.util.*;
import javax.script.Bindings;

class ScopedVariables extends HashMap<String,Object>
	implements Bindings
{
	public ScopedVariables(Bindings parentScope)
	{
		this.putAll(parentScope);
	}

	public ScopedVariables(Map<String,?> parentScope)
	{
		this.putAll(parentScope);
	}
}
