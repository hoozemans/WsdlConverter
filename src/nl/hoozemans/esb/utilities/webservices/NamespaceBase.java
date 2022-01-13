package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class NamespaceBase extends Base
{
	protected List<Namespace> namespaces;
	protected String name;
	
	public NamespaceBase( Base parent, String source, String path )
	throws Exception
	{
		super(parent, source, path);
		name = null;
		namespaces = new ArrayList<Namespace>();
	}
	
	public Namespace getLocalNamespaceByPrefix( String prefix )
	{
		for( Namespace ns : namespaces )
		{
			if( ns.getPrefix() != null && ns.getPrefix().equals(prefix) )
			{
				return ns;
			}
		}
		return null;
	}
	
	public Namespace getNamespaceByPrefix( String prefix )
	{
		Namespace ns = getLocalNamespaceByPrefix(prefix);
		if( ns == null && this.parent instanceof NamespaceBase ) 
			ns = ((NamespaceBase)this.parent).getNamespaceByPrefix(prefix);
		return ns;
	}
	
	public List<Namespace> getLocalNamespaceByUri( String uri )
	{
		List<Namespace> result = new ArrayList<Namespace>();
		for( Namespace ns : namespaces )
		{
			if( ns.getUri().equals(uri) )
			{
				result.add(ns);
			}
		}
		return result;
	}
	
	public List<Namespace> getNamespaceByUri( String uri )
	{
		List<Namespace> result = getLocalNamespaceByUri(uri);
		if( result.size() == 0 && this.parent instanceof NamespaceBase ) 
			result = ((NamespaceBase)this.parent).getNamespaceByUri(uri);
		return result;
	}
	
	public List<String> getUniqueNamespaceUris()
	{
		Map<String, String> uris = new HashMap<String, String>();
		for( Namespace ns : namespaces )
			uris.put(ns.getUri(), ns.getUri());
		return new ArrayList<String>(uris.values());
	}
	
	public Namespace getTargetNamespace()
	{
		for( Namespace ns : namespaces )
		{
			if( ns.isTargetNamespace() ) return ns;
		}
		
		if( parent instanceof NamespaceBase )
			return ((NamespaceBase)parent).getTargetNamespace();
		
		return null;
	}
	
	public boolean setTargetNamespace( String uri )
	throws Exception
	{
		Namespace ns = getTargetNamespace();
		if( ns == null )
		{
			ns = new Namespace(this, " targetNamespace=\""+uri+"\" ");
			return this.addNamespace(ns);
		}
		
		return ns.setUri(uri);
	}
	
	public abstract boolean replaceNamespacePrefix( String oldPrefix, String newPrefix )
	throws Exception;
	
	protected boolean replaceNamespacePrefixInSource( String oldPrefix, String newPrefix )
	{
		String pattern = "(?si)\\s+(type|ref|base|element|message|binding)="+PATTERN_QUOTE+"([^:]+):("+PATTERN_NOTQUOTE+")"+PATTERN_QUOTE;
		source = source.replaceAll(pattern, " $1=\""+newPrefix+":$3\"");
		return setModified();
	}
	
	@Override
	public boolean parse()
	throws Exception
	{
		String opening = source.replaceAll("(?si)>(.*?)$", ">");
		//String rest = source.replaceAll("(?si)(.*?)>(.*?)", "$2");
		
		// only get namespace declarations from opening tag
		namespaces = Namespace.extract(this, opening);
		
		// get name
		{
			
			String pattern = "\\s+name=" + PATTERN_LITERAL;
			Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher match = regex.matcher(opening);
			
			if( match.find() )
			{
				this.name = match.group(1);
			}
		}
		
		return true;
	}
	
	public String getName()
	{
		return name;
	}

	public boolean setName(String name)
	{
		this.name = name;
		return setModified();
	}
	
	public List<Namespace> getLocalNamespaces()
	{
		return namespaces;
	}
	
	public boolean addNamespace( Namespace ns )
	{
		if( namespaces.size() == 0 )
		{
			setModified();
			return namespaces.add(ns);
		}
		for( Namespace n : namespaces )
		{
			// there can only be one target namespace
			if( n.isTargetNamespace() && ns.isTargetNamespace() )
			{
				return false;
			}
			// cant have two nsdecl with the same prefix
			if( (n.getPrefix() != null && ns.getPrefix() != null && n.getPrefix().equals(ns.getPrefix())) )
			{
				return false;
			}
			// xmlns= or namespace=
			if( (n.getPrefix() == null && ns.getPrefix() == null)  
				&& n.isTargetNamespace() == ns.isTargetNamespace()
				&& n.getUri().equals(ns.getUri()) )
			{
				return false;
			}
		}
		
		setModified();
		return namespaces.add(ns);
	}
	
	public boolean addAllNamespaces( List<Namespace> ns )
	{
		for( Namespace n : ns )
		{
			addNamespace(n);
		}
		return true;
	}
	
	@Override
	public String getSource() 
	throws Exception 
	{
		if( !modified )
			return source;
		
		if( source != null && source.contains("pattern value") )
		{
			//System.out.println("break");
		}
		
		// remove namespace declarations, targetNamespace and name
		// only rebuild namespaces from the opening tag of the schema element
		String opening = source.replaceAll("(?si)>(.*?)$", ">").trim();
		opening = opening.replaceAll("(?si)\\s+xmlns(:[^=]+)?="+PATTERN_LITERAL, "");
		opening = opening.replaceAll("(?si)\\s+targetNamespace="+PATTERN_LITERAL, "");
		opening = opening.replaceAll("(?si)\\s+name="+PATTERN_LITERAL, "");
		
		for( Namespace ns : namespaces )
		{
			opening = opening.replaceFirst("(/?>)", " " + ns.getSource() + "$1");
		}
		
		if( getName() != null )
			opening = opening.replaceFirst("(/?>)", " name=\""+getName()+"\"$1");
		
		String result = source.replaceFirst("(?si)<(.*?)>", opening);
		
		return result;
	}
	
}
