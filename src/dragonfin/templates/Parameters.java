package dragonfin.templates;

import java.util.*;

class Parameters extends AbstractMap<String,Object>
{
	Map<String,?> params;
	Map<String,?> parentScope;

	public Parameters(Map<String,?> params, Map<String,?> parentScope)
	{
		this.params = params;
		this.parentScope = parentScope;
	}

	@Override
	public Set< Map.Entry<String,Object> > entrySet()
	{
		return new EntrySet();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return params.containsKey(key) || parentScope.containsKey(key);
	}

	@Override
	public Object get(Object key)
	{
		if (params.containsKey(key))
			return params.get(key);
		else
			return parentScope.get(key);
	}

	class EntrySet extends AbstractSet< Map.Entry<String,Object> >
	{
		HashSet<String> keys;
		EntrySet()
		{
			keys = new HashSet<String>();
			keys.addAll(params.keySet());
			keys.addAll(parentScope.keySet());
		}

		@Override
		public Iterator< Map.Entry<String,Object> > iterator()
		{
			return new MyEntrySetIterator(keys.iterator());
		}

		@Override
		public int size()
		{
			return keys.size();
		}

	}

	class MyEntrySetIterator implements Iterator< Map.Entry<String,Object> >
	{
		Iterator<String> keysIterator;

		MyEntrySetIterator(Iterator<String> keysIterator)
		{
			this.keysIterator = keysIterator;
		}

		public boolean hasNext()
		{
			return keysIterator.hasNext();
		}

		public Map.Entry<String,Object> next()
		{
			String k = keysIterator.next();

			return new AbstractMap.SimpleEntry<String,Object>(k,
				params.containsKey(k) ? params.get(k) :
					parentScope.get(k));
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
