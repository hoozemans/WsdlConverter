package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Declaration extends NamespaceBase
{
	protected static List<String> declarationTypes;
	static {
		String[] s = {"element", "complextype", "attribute", "simpletype"};
		declarationTypes = Arrays.asList(s);
	}
	protected String declarationType;
	
	public Declaration( Base parent, String source )
	throws Exception
	{
		super(parent, source, null);
		parse();
	}
	
	public String getDeclarationType()
	{
		return declarationType;
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
		super.parse();
		
		// determine type of top-level declaration: attribute, element, simpletype, complextype
		{
			String pattern = "(?si)\\s*</?" + PATTERN_ANYNSPREFIX + "([^>\\s]+)";
			Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher match = regex.matcher(source);
			match.find();
			this.declarationType = match.group(2).toLowerCase();
		}

		// and now we need to add namespace declarations from inline tags
		{
			String inline = source.replaceAll("(?si)<"+PATTERN_ANYNSPREFIX+"annotation(.*?)</"+PATTERN_ANYNSPREFIX+"annotation>", "");
			inline = source.replaceFirst("(?si)^(.*?)>(.*?)$", "$2");
			List<Namespace> ns = Namespace.extract(this, inline);
			addAllNamespaces(ns);
		}
		
		// we'll also add ns declarations from parents used inline
		{
			String pattern = "(?si)\\s+(type|ref|base|element|message|binding)="+PATTERN_QUOTE+"([^:]+):("+PATTERN_NOTQUOTE+")"+PATTERN_QUOTE;
			Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher match = regex.matcher(source);
			while( match.find() )
			{
				Namespace ns = getNamespaceByPrefix(match.group(2));
				if( ns != null )
				{
					Namespace n = (Namespace)ns.clone();
					n.parent = this;
					addNamespace(n);
				}
			}
		}
		
		return true;
	}

	public static List<Declaration> extract(Base parent, String from) 
	throws Exception
	{
		List<Declaration> result = new ArrayList<Declaration>();
		
		String s = from.replaceAll("(?si)</?"+PATTERN_ANYNSPREFIX+"schema(.*?)>", "");
		while( s.length() > 0 )
		{
			String particle = "";
			
			// the first tag signifies a top level element
			{
				String pattern = "(?si)\\s*<" + PATTERN_ANYNSPREFIX + "([^>]+)\\/?>\\s*";
				Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
				Matcher match = regex.matcher(s);
				if( !match.find() ) break;
				String tag = match.group(0);
				particle = particle + tag;
				s = s.substring(tag.length());
			}
			
			// count all the following opening and closing tags until level=0
			int level = 1;
			if( !particle.trim().endsWith("/>") )
			do
			{
				String pattern = "(?si)([^<]*)</?" + PATTERN_ANYNSPREFIX + "([^>]+)>\\s*";
				Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
				Matcher match = regex.matcher(s);
				
				if( !match.find() ) break;
				
				String tag = match.group(0);
				if( tag.trim().endsWith("/>") );
				else if( tag.contains("</") ) level--;
				else level++;
				
				particle = particle + tag;
				s = s.substring(tag.length());
			}
			while( level > 0 );
			
			{
				String pattern = "(?si)\\s*</?" + PATTERN_ANYNSPREFIX + "([^>\\s]+)";
				Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
				Matcher match = regex.matcher(particle);
				
				match.find();
				
				if( declarationTypes.contains(match.group(2).toLowerCase()) )
				{
					Declaration declaration = new Declaration(parent, particle);
					result.add(declaration);
				}
			}
		}
		
		return result;
	}
}
