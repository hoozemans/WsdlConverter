package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Namespace extends Base
{
	protected String prefix;
	protected String uri;

	public Namespace( Base parent, String source )
	throws Exception
	{
		super(parent, source, null);
		parse();
	}
	
	@Override
	public boolean parse()
	{
		String pattern = "xmlns:?([^=]+)?=" + PATTERN_LITERAL;
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher matcher = regex.matcher(source);
		if( matcher.find() )
		{
			this.prefix = matcher.group(1);
			this.uri = matcher.group(2);
			return true;
		}
		
		pattern = "(target)?namespace=" + PATTERN_LITERAL;
		regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		matcher = regex.matcher(source);
		if( matcher.find() )
		{
			this.prefix = null;
			this.uri = matcher.group(2);
			return true;
		}
		
		return false;
	}
	
	public String getPrefix()
	{
		return prefix;
	}
	
	public boolean setPrefix(String prefix)
	{
		this.prefix = prefix; 
		return setModified();
	}
	
	public String getUri()
	{
		return uri;
	}
	
	public boolean setUri(String uri)
	{
		this.uri = uri;
		return setModified();
	}
	
	public boolean isTargetNamespace()
	{
		return source.toLowerCase().contains("targetnamespace");
	}
	
	public static List<Namespace> extract(Base parent, String source)
	throws Exception
	{
		List<Namespace> result = new ArrayList<Namespace>();
		String from = source.replaceAll("(?si)<"+PATTERN_ANYNSPREFIX+"annotation(.*?)</"+PATTERN_ANYNSPREFIX+"annotation>", "");
		
		String pattern = "\\s+xmlns:?([^=]+)?=" + PATTERN_LITERAL;
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher matcher = regex.matcher(from);
		
		while( matcher.find() )
		{
			Namespace ns = new Namespace(parent, matcher.group(0));
			result.add(ns);
		}
		
		pattern = "\\s+targetnamespace=" + PATTERN_LITERAL;
		regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		matcher = regex.matcher(from);
		
		while( matcher.find() )
		{
			Namespace ns = new Namespace(parent, matcher.group(0));
			result.add(ns);
		}

		return result;
	}

	@Override
	public String getSource() 
	throws Exception 
	{
		if( !modified )
			return source;
		
		if( source.matches("(?si)\\s*xmlns:([^=]+)=(.*?)") )
			return " xmlns:" + getPrefix() + "=\"" + getUri() + "\" ";
		
		if( source.matches("(?si)\\s*xmlns=(.*?)") )
			return " xmlns=\"" + getUri() + "\" ";
		
		if( source.matches("(?si)\\s*targetnamespace=(.*?)") )
			return " targetNamespace=\"" + getUri() + "\" ";
		
		if( source.matches("(?si)\\s*namespace=(.*?)") )
			return " namespace=\"" + getUri() + "\" ";
		
		throw new Exception("Unknown namespace declaration format");
	}
}
