package nl.hoozemans.esb.utilities.webservices;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Xsd extends NamespaceBase
{
	protected List<Schema> schemas; // 

	public Xsd( String path )
	throws Exception
	{
		super(null, new String(Files.readAllBytes(Paths.get(path)), "UTF-8"), path);
		schemas = new ArrayList<Schema>();
		parse();
	}
	
	public boolean replaceNamespacePrefix( String oldPrefix, String newPrefix )
	throws Exception
	{
		throw new Exception("Can't invoke replaceNamespacePrefix on Xsd container level");
	}
	
	public boolean parse()
	throws Exception
	{
		// just the file; no further parsing required at this level
		schemas = Schema.extract(this, source);
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
		result.addAll(this.schemas);
		
		for( Schema schema : schemas )
		{
			for( Import imp : schema.imports )
			{
				result.addAll(((Xsd)imp.imported).getAllSchemas());
			}
		}
		
		return result;
	}
	
	public List<Schema> getUniqueSchemas()
	throws Exception
	{
		Map<String, Schema> uniqueSchemas = new HashMap<String, Schema>();
		
		List<Schema> schemas = getAllSchemas();
		for( Schema schema : schemas )
		{
			String tns = schema.getTargetNamespace().getUri();
			if( !uniqueSchemas.containsKey(tns) )
			{
				Schema copy = (Schema)schema.clone();
				copy.removeAllImports();
				uniqueSchemas.put(tns, copy);
			}
			else
			{
				uniqueSchemas.get(tns).addAllNamespaces(schema.getLocalNamespaces());
				uniqueSchemas.get(tns).addAllDeclarations(schema.getLocalDeclarations());
				uniqueSchemas.get(tns).addAllImports(schema.getLocalImports());		
			}
		}
		
		return new ArrayList<Schema>(uniqueSchemas.values());
	}
	
}
