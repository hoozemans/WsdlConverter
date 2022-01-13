package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortType extends NamespaceBase
{
	public PortType( Base parent, String source )
	throws Exception
	{
		super(parent, source, null);
		parse();
	}
	
	public boolean replaceNamespacePrefix( String oldPrefix, String newPrefix )
	throws Exception
	{
		if( getNamespaceByPrefix(oldPrefix) != null )
			return false;
		
		return replaceNamespacePrefixInSource(oldPrefix, newPrefix);
	}
	
	public boolean parse()
	throws Exception
	{
		return super.parse();
	}

	public static List<PortType> extract(Base parent, String from)
	throws Exception
	{
		List<PortType> result = new ArrayList<PortType>();
		
		String pattern = "<" + PATTERN_ANYNSPREFIX + "PortType(.*?)>(.*?)</" + PATTERN_ANYNSPREFIX + "PortType>";
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
		Matcher matcher = regex.matcher(from);
		
		while( matcher.find() )
		{
			PortType portType = new PortType(parent, matcher.group(0));
			result.add(portType);
		}
		
		return result;
	}
}
