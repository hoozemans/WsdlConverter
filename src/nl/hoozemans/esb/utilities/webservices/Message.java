package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message extends NamespaceBase
{
	public Message( Base parent, String source )
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

	public static List<Message> extract(Base parent, String from)
	throws Exception
	{
		List<Message> result = new ArrayList<Message>();
		
		String pattern = "<" + PATTERN_ANYNSPREFIX + "Message(.*?)>(.*?)</" + PATTERN_ANYNSPREFIX + "Message>";
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
		Matcher matcher = regex.matcher(from);
		
		while( matcher.find() )
		{
			Message message = new Message(parent, matcher.group(0));
			result.add(message);
		}
		
		return result;
	}

}
