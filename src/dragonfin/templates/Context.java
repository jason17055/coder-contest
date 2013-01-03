package dragonfin.templates;

import java.io.*;
import java.util.*;

class Context
{
	TemplateToolkit toolkit;
	String templateName;
	Map<String, ?> vars;
	Writer out;
}
