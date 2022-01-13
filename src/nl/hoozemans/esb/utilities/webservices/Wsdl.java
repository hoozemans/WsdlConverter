package nl.hoozemans.esb.utilities.webservices;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wsdl extends NamespaceBase
{
	protected List<Definition> definitions; // 

	public Wsdl( String path )
	throws Exception
	{
		super(null, new String(Files.readAllBytes(Paths.get(path)), "UTF-8"), path);
		definitions = new ArrayList<Definition>();
		parse();
	}
	
	public boolean parse()
	throws Exception
	{
		// this is just the file; don't do any element level parsing
		// by calling the super
		definitions = Definition.extract(this, source);
		return true;
	}

	public static List<Base> extract(Base parent, String from) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public List<Schema> getAllSchemas()
	{
		List<Schema> result = new ArrayList<Schema>();
		result.addAll(this.definitions.get(0).schemas);
		
		for( Definition definition : definitions )
		{
			for( Import imp : definition.imports )
			{
				List<Schema> add = new ArrayList<Schema>();
				if( imp.imported instanceof Xsd )
					add = ((Xsd)imp.imported).getAllSchemas();
				else if( imp.imported instanceof Wsdl )
					add = ((Wsdl)imp.imported).getAllSchemas();
				result.addAll(add);
			}
		}
		
		for( Schema schema : this.definitions.get(0).schemas )
		{
			for( Import imp : schema.imports )
			{
				List<Schema> add = new ArrayList<Schema>();
				if( imp.imported instanceof Xsd )
					add = ((Xsd)imp.imported).getAllSchemas();
				else if( imp.imported instanceof Wsdl )
					add = ((Wsdl)imp.imported).getAllSchemas();
				result.addAll(add);
			}
		}
		
		return result;
	}

	public List<Definition> getAllDefinitions()
	{
		List<Definition> result = new ArrayList<Definition>();
		result.addAll(this.definitions);
		
		for( Definition definition : definitions )
		{
			for( Import imp : definition.getLocalImports() )
			{
				List<Definition> add = new ArrayList<Definition>();
				if( imp.imported instanceof Wsdl )
					add = ((Wsdl)imp.imported).getAllDefinitions();
				result.addAll(add);
			}
		}

		return result;
	}
	
	public List<Binding> getAllBindings()
	{
		List<Binding> result = new ArrayList<Binding>();
		
		for( Definition def : getAllDefinitions() )
		{
			result.addAll(def.getLocalBindings());
		}
		
		return result;
	}

	public List<PortType> getAllPortTypes()
	{
		List<PortType> result = new ArrayList<PortType>();
		
		for( Definition def : getAllDefinitions() )
		{
			result.addAll(def.getLocalPortTypes());
		}
		
		return result;
	}

	public List<Message> getAllMessages()
	{
		List<Message> result = new ArrayList<Message>();
		
		for( Definition def : getAllDefinitions() )
		{
			result.addAll(def.getLocalMessages());
		}
		
		return result;
	}
	
	public boolean replaceNamespacePrefix( String oldPrefix, String newPrefix )
	throws Exception
	{
		throw new Exception("Can't invoke replaceNamespacePrefix on Wsdl container level");
	}

	public List<Service> getAllServices()
	{
		List<Service> result = new ArrayList<Service>();
		
		for( Definition def : getAllDefinitions() )
		{
			result.addAll(def.getLocalServices());
		}
		
		return result;
	}

	public boolean addDefinition( Definition def )
	throws Exception
	{
		if( def == null ) 
			return false;
		if( definitions == null ) 
			definitions = new ArrayList<Definition>();
		
		for( Definition d : definitions )
		{
			if( d.getName().equals(def.getName()) && d.getTargetNamespace().equals(def.getTargetNamespace()) )
				return false;
		}
		
		return definitions.add(def);
	}
	
	public List<Schema> getUniqueSchemas()
	throws Exception
	{
		Map<String, Schema> uniqueSchemas = new HashMap<String, Schema>();
		
		List<Schema> schemas = getAllSchemas();
		for( Schema schema : schemas )
		{
			Namespace tns = schema.getTargetNamespace();
			if( uniqueSchemas.get(tns.getUri()) == null )
			{
				System.out.println("getUniqueSchemas: adding schema from " + schema.getPath() + " into " + tns.getUri());
				Schema copy = (Schema)schema.clone();
				copy.removeAllImports();
				uniqueSchemas.put(tns.getUri(), copy);
			}
			else
			{
				System.out.println("getUniqueSchemas: merging " + schema.getPath() + " into " + tns.getUri());
				uniqueSchemas.get(tns.getUri()).addAllNamespaces(schema.getLocalNamespaces());
				uniqueSchemas.get(tns.getUri()).addAllDeclarations(schema.getLocalDeclarations());
				uniqueSchemas.get(tns.getUri()).addAllImports(schema.getLocalImports());
			}
		}
		
		return new ArrayList<Schema>(uniqueSchemas.values());
	}
	
	
}
