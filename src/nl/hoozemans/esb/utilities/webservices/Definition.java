package nl.hoozemans.esb.utilities.webservices;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Definition extends NamespaceBase
{
	protected List<Schema> schemas; // = new ArrayList<Schema>();
	protected List<Import> imports; // = new ArrayList<Import>();
	protected List<PortType> portTypes; // = new ArrayList<PortType>();
	protected List<Service> services; // = new ArrayList<Service>();
	protected List<Binding> bindings; // = new ArrayList<Binding>();
	protected List<Message> messages; // = new ArrayList<Message>();
	
	public Definition( Base parent, String source )
	throws Exception
	{
		super(parent, source, null);
		parse();
	}
	
	public boolean parse()
	throws Exception
	{
		super.parse();

		schemas = Schema.extract(this, source);
		portTypes = PortType.extract(this, source);
		bindings = Binding.extract(this, source);
		services = Service.extract(this, source);
		messages = Message.extract(this, source);
		
		// remove inline schemas so that their imports and includes aren't processed
		String s = source.replaceAll("(?si)<"+PATTERN_ANYNSPREFIX+"schema(.*)</"+PATTERN_ANYNSPREFIX+"schema>", "");
		imports = Import.extract(this, s);
	
		return true;
	}
	
	public static List<Definition> extract(Base parent, String from)
	throws Exception
	{
		List<Definition> result = new ArrayList<Definition>();
		
		String pattern = "<" + PATTERN_ANYNSPREFIX + "definitions(.*?)>(.*?)</" + PATTERN_ANYNSPREFIX + "definitions>";
		//String pattern = "<(\\S+?)(.*?)>(.*?)</\\1>";
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
		Matcher matcher = regex.matcher(from);
		
		while( matcher.find() )
		{
			System.out.println("	found definition");
			Definition definition = new Definition(parent, matcher.group(0));
			result.add(definition);
		}
		
		return result;
	}
	
	public List<Schema> getLocalSchemas()
	{
		return schemas;
	}
	
	public boolean addSchema( Schema schema )
	throws Exception
	{
		setModified();
		return schemas.add(schema);
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

	public boolean addMessage( Message msg )
	{
		setModified();
		return messages.add(msg);
	}
	
	public boolean addAllMessages( List<Message> msg )
	{
		for( Message m : msg )
		{
			addMessage(m);
		}
		return true;
	}

	public List<Message> getLocalMessages() 
	{
		return messages;
	}

	public boolean addPortType( PortType p )
	{
		setModified();
		return portTypes.add(p);
	}
	
	public boolean addAllPortTypes( List<PortType> pts )
	{
		for( PortType p : pts )
		{
			addPortType(p);
		}
		return true;
	}

	public List<PortType> getLocalPortTypes() 
	{
		return portTypes;
	}

	public boolean addBinding( Binding bind )
	{
		setModified();
		return bindings.add(bind);
	}
	
	public boolean addAllBindings( List<Binding> bind )
	{
		for( Binding b : bind )
		{
			addBinding(b);
		}
		return true;
	}

	public List<Binding> getLocalBindings() 
	{
		return bindings;
	}

	public boolean addService( Service svc )
	{
		setModified();
		return services.add(svc);
	}
	
	public boolean addAllServices( List<Service> svc )
	{
		for( Service s : svc )
		{
			addService(s);
		}
		return true;
	}

	public List<Service> getLocalServices() 
	{
		return services;
	}
	
	public boolean replaceNamespacePrefix( String oldPrefix, String newPrefix )
	throws Exception
	{
		System.out.println("		replacing "+oldPrefix+" with "+newPrefix+" in "+this.getPath());
		
		for( Namespace ns :  namespaces )
		{
			if( ns.getPrefix() != null && ns.getPrefix().equals(oldPrefix) )
			{
				ns.setPrefix(newPrefix);
			}
		}
		
		// replace ns prefix in subsections only if prefix is not redeclared
		
		for( Service s : services )
			if( s.getNamespaceByPrefix(oldPrefix) == null )
				s.replaceNamespacePrefix(oldPrefix, newPrefix);

		for( Message m : messages )
			if( m.getNamespaceByPrefix(oldPrefix) == null )
				m.replaceNamespacePrefix(oldPrefix, newPrefix);

		for( Schema s : schemas )
			if( s.getNamespaceByPrefix(oldPrefix) == null )
				s.replaceNamespacePrefix(oldPrefix, newPrefix);

		for( PortType p : portTypes )
			if( p.getNamespaceByPrefix(oldPrefix) == null )
				p.replaceNamespacePrefix(oldPrefix, newPrefix);

		for( Binding b : bindings )
			if( b.getNamespaceByPrefix(oldPrefix) == null )
				b.replaceNamespacePrefix(oldPrefix, newPrefix);

		return true;
	}	
	

	public String getSource()
	throws Exception
	{
		if( !modified )
			return source;
		
		String result = super.getSource();
		
		// remove all inline imports, porttypes, bindings, services from source
		result = result.replaceFirst(
			"(?si)<"+PATTERN_ANYNSPREFIX+"definitions(.*?)>(.*?)</"+PATTERN_ANYNSPREFIX+"definitions>", 
			"<$1definitions$2>\n</$1definitions>"
		);
		
		// re-add from list
		String inline = "";
		for( Import i : imports )
		{
			inline = inline + "\n" + i.getSource();
		}
		inline = inline + "<wsdl:types xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">";
		for( Schema s : schemas )
		{
			inline = inline + "\n" + s.getSource();
		}
		inline = inline + "</wsdl:types>";
		for( Message m : messages )
		{
			inline = inline + "\n" + m.getSource();
		}
		for( PortType p : portTypes )
		{
			inline = inline + "\n" + p.getSource();
		}
		for( Binding b : bindings )
		{
			inline = inline + "\n" + b.getSource();
		}
		for( Service s : services )
		{
			inline = inline + "\n" + s.getSource();
		}

		inline = inline.replace("\\", "\\\\");
		result = result.replaceFirst(">", ">"+inline);
		//System.out.println("Definition.getSource.result: " + result);
		
		return result;
	}
}
