package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.List;

public abstract class Base implements Cloneable
{
	public static final String PATTERN_QUOTE = "['\\\"]";
	public static final String PATTERN_NOTQUOTE = "[^'\\\"]+";
	public static final String PATTERN_NSPREFIX = "([^:<>\\s]+)";
	public static final String PATTERN_ANYNSPREFIX = "([^:<>\\s]+:)?";
	public static final String PATTERN_LITERAL = "['\\\"]([^'\\\"]*)['\\\"]";
	public static final String PATTERN_TYPEREF = "['\\\"]"+PATTERN_NSPREFIX+":([^'\\\"]+)['\\\"]";
	
	protected String source;
	protected boolean modified;
	protected Base parent;
	protected String path;

	public Base( Base parent, String source, String path )
	throws Exception
	{
		this.source = source;
		this.modified = false;
		this.parent = parent;
		this.path = path;
	}
	
	public String getPath()
	{
		if( null!=path ) return path;
		if( this.parent instanceof Base ) return this.parent.getPath();
		return null;
	}
	
	public abstract boolean parse()
	throws Exception;
	
	public abstract String getSource()
	throws Exception;
	
	public static List<?> extract(Base parent, String from)
	throws Exception
	{
		List<Base> result = new ArrayList<Base>();
		
		return result;
	}
	
	public Base clone() 
	throws CloneNotSupportedException
	{
		return (Base)super.clone();
	}
	
	public boolean setModified()
	{
		modified = true;
		if( parent instanceof Base )
			parent.setModified();
		return true;
	}
}
