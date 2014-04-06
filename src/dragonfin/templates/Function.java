package dragonfin.templates;

import javax.script.Bindings;

public interface Function
{
	Object invoke(Bindings args) throws Exception;
}
