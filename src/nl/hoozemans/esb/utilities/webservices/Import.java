package nl.hoozemans.esb.utilities.webservices;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.hoozemans.esb.utilities.files.PathUtilities;

public class Import extends NamespaceBase
{
	protected enum typeEnum { IMPORT, INCLUDE };
	protected typeEnum type;
	protected String location;
	protected String absolute;
	protected boolean resolved;
	protected NamespaceBase imported;

	public static Map<String, Import> pathRegister;
	static {
		pathRegister = new HashMap<String, Import>();
	}
	
	public Import( Base parent, String source )
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
		String pattern = "<" + PATTERN_ANYNSPREFIX + "(import|include)(.*?)>";
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher matcher = regex.matcher(source);
		matcher.find();
		
		this.type = matcher.group(2).equalsIgnoreCase("import") ? typeEnum.IMPORT : typeEnum.INCLUDE;
		
		// get location
		pattern = "(schema)?location=" + PATTERN_LITERAL;
		regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		matcher = regex.matcher(source);
		if( matcher.find() )
		{
			this.location = matcher.group(2);
			this.absolute = PathUtilities.beautifyPath(PathUtilities.getBasePath(this.getPath()) + "/" + this.location);
			this.resolved = (new File(this.absolute)).exists();
		}
		
		// parse namespaces
		super.parse();
		
		if( this.absolute == null || !this.resolved ) return true;
		
		if( Import.pathIsRegistered(this.absolute) )
		{
			imported = Import.getByPath(this.absolute).imported;
		}
		else if( parent instanceof Schema && ((Schema)parent).skipImport )
		{
			// do nothing
			return true;
		}
		else if( absolute.toLowerCase().endsWith(".wsdl") )
		{
			Import.registerPath(this.absolute, this);
			imported = new Wsdl(absolute);
		}
		else if( absolute.toLowerCase().endsWith(".xsd") )
		{
			Import.registerPath(this.absolute, this);
			imported = new Xsd(absolute);
		}
		
		return true;
	}

	public static List<Import> extract(Base parent, String from) 
	throws Exception
	{
		List<Import> result = new ArrayList<Import>();
		
		String pattern = "<" + PATTERN_ANYNSPREFIX + "(import|include)(.*?)>";
		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher matcher = regex.matcher(from);
		
		while( matcher.find() )
		{
			Import imp = new Import(parent, matcher.group(0));
			result.add(imp);
		}
		
		return result;
	}
	
	public static boolean registerPath(String path, Import obj)
	{
		if( Import.pathIsRegistered(path) ) return false;
		
		Import.pathRegister.put(path, obj);
		return true;
	}
	
	public static boolean pathIsRegistered(String path)
	{
		return Import.pathRegister.containsKey(path);
	}
	
	public static Import getByPath(String path)
	{
		return Import.pathRegister.get(path);
	}

}
