package dragonfin.templates;

import java.io.*;
import java.util.*;
import javax.script.Bindings;

class Context
{
	TemplateToolkit toolkit;
	String templateName;
	Bindings vars;
	Writer out;
}
