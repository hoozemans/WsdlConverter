package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Schema extends NamespaceBase
{
	protected List<Import> imports;
	protected List<Declaration> declarations;
	protected boolean skipImport;
	
	public Schema( Base parent, String source )
	throws Exception
	{
		super(parent, source, null);
		imports = new ArrayList<Import>();
		declarations = new ArrayList<Declaration>();
		skipImport = false;
		parse();
	}
	
	public boolean parse()
	throws Exception
	{
		super.parse();
		
		imports = Import.extract(this, source);
		declarations = Declaration.extract(this, source);
		
		return true;
	}
	
	public boolean removeAllImports()
	{
		imports = new ArrayList<Import>();
		return setModified();
	}
	
	public boolean addEmptyImports()
	throws Exception
	{
		Map<String, Import> localImports = new HashMap<String, Import>();
		for( Import i : imports )
		{
			if( i.getSource().matches("\\s+namespace=") )
			{
				String uri = i.getSource().replaceFirst("(.*?)namespace="+PATTERN_QUOTE+"(.*?)"+PATTERN_QUOTE+"(.*?)", "$2");
				localImports.put(uri, i);
			}
		}

		for( Declaration d : declarations )
		{
			List<Namespace> ns = d.getLocalNamespaces();
			
			for( Namespace n : ns )
			if( !n.getUri().equals(getTargetNamespace().getUri()) )
			{
				if( localImports.get(n.getUri()) != null )
					continue;
				
				String s = "<xsd:import xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" namespace=\""+n.getUri()+"\" />";
				Import imp = new Import(this, s);
				addImport(imp);
				localImports.put(n.getUri(), imp);
			}
		}
		
		return true;
	}
	
	public List<Declaration> getLocalDeclarations()
	{
		return declarations;
	}
	
	public Declaration getLocalDeclaration(String type, String namespace, String name)
	{
		for( Declaration d : declarations )
		{
			String tns = d.getTargetNamespace().getUri();
			String tname = d.getName();
			String ttype = d.getDeclarationType();
			if( ttype.equals(type) && tns.equals(namespace) && tname.equals(name) )
				return d;
		}
		
		return null;
	}
	
	public boolean addDeclaration( Declaration declaration )
	{
		for( Declaration d : declarations )
		{
			if( d.getDeclarationType().equals(declaration.getDeclarationType()) && d.getName().equals(declaration.getName()) )
				return false;
		}
		
		setModified();
		return declarations.add(declaration);
	}
	
	public boolean addAllDeclarations( List<Declaration> declaration )
	{
		for( Declaration d : declaration )
		{
			addDeclaration(d);
		}
		return true;
	}

	public boolean addImport( Import imp )
	{
		setModified();
		return imports.add(imp);
	}
	
	public boolean addAllImports( List<Import> imp )
	{
		for( Import i : imp )
		{
			addImport(i);
		}
		return true;
	}

	public List<Import> getLocalImports() 
	{
		return imports;
	}
	
	public boolean replaceNamespacePrefix( String oldPrefix, String newPrefix )
	throws Exception
	{
		if( getNamespaceByPrefix(oldPrefix) != null )
			return false;
		
		for( Declaration d : declarations )
		{
			d.replaceNamespacePrefix(oldPrefix, newPrefix);
		}
		
		return true;
	}	
	
	public static List<Schema> extract(Base parent, String from)
	throws Exception
	{
		List<Schema> result = new ArrayList<Schema>();
		
		String pattern = "<" + PATTERN_ANYNSPREFIX + "schema(.*?)>(.*?)</" + PATTERN_ANYNSPREFIX + "schema>";
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher matcher = regex.matcher(from);
		
		while( matcher.find() )
		{
			Schema schema = new Schema(parent, matcher.group(0));
			result.add(schema);
		}
		
		return result;
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

		String result = super.getSource();
		
		// remove all inline imports and declarations from source
		result = result.replaceFirst(
			"(?si)<"+PATTERN_ANYNSPREFIX+"schema(.*?)>(.*?)</"+PATTERN_ANYNSPREFIX+"schema>", 
			"<$1schema$2>\n</$1schema>"
		);
		
		// add declarations from list
		for( Declaration d : declarations )
		{
			String s = d.getSource();
			s = s.replace("\\", "\\\\");
			result = result.replaceFirst(">\\s+", ">\n"+s);
		}
		
		// add imports from list
		if( imports == null )
			imports = new ArrayList<Import>();
		
		for( Import i : imports )
		{
			result = result.replaceFirst(">\\s+", ">\n"+i.getSource());
		}
		
		return result;
	}

}
