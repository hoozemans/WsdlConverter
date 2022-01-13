package nl.hoozemans.esb.utilities.files;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public  class PathUtilities 
{
	public static String getBasePath(String path)
	{
		Path p = Paths.get(path);
		String basePath = p.getParent().toString();
		return beautifyPath(basePath);
	}
	
	public static String getFilename(String path)
	{
		Path p = Paths.get(path);
		String name = p.getFileName().toString();
		return name;
	}
	
	public static String getPathFromNamespace(String base, String namespace)
	{
		if( namespace.length() == 0 ) return base;
		
		namespace = namespace.replaceAll("^http.?\\/\\/", "") + "/";
		String domain = "";
		try
		{
			domain = namespace.substring(0, namespace.indexOf("/"));
			domain = domain.replaceAll(":", "_");
		}
		catch( Exception e )
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
		}
		// we don't want version numbers encoded in the domain part to be split and reversed, so:
		domain = domain.replaceAll("([0-9]+)\\.([0-9]+)", "$1_$2");
		List<String> parts = new ArrayList<String>(Arrays.asList(domain.split("\\.")));
		Collections.reverse(parts);
		namespace = namespace.substring(domain.length()+1);
		parts.addAll(Arrays.asList(namespace.split("/")));
		
		String path = base;
		for( String part : parts )
		{
			path = path + "/" + part.replaceAll(":", "_");
		}
		path = beautifyPath(path);
		
		return path;
	}
	
	public static String createPathFromNamespace(String base, String namespace)
	{
		if( namespace.length() == 0 ) return base;
		
		String path = getPathFromNamespace(base, namespace);
		(new File(path)).mkdirs();
		
		return path;
	}
	
	public static boolean createPathForFile(String path)
	{
		return (new File(getBasePath(path))).mkdirs();
	}
	
	// assumes both target and to point to files
	public static String getRelativePath(String target, String to)
	{
		String relative = "./";
		String toPath = getBasePath(to);
		String targetPath = getBasePath(target);
		while( !toPath.equals("") && !targetPath.startsWith(toPath) )
		{
			relative = relative + "../";
			toPath = getBasePath(toPath);
		}
		
		targetPath = targetPath.replaceFirst(toPath, "");
		relative = relative + "/" + targetPath + "/" + getFilename(target);
		relative = relative.replaceFirst("\\./\\.\\./", "../");
		relative = beautifyPath(relative);
		return relative;
	}
	
	public static String beautifyPath( String path )
	{
		String result = path.replaceAll("\\\\", "/");
		while( result.contains("/./") ) result = result.replace("/./", "/");
		while( result.contains("//") ) result = result.replace("//", "/");
		Pattern pattern = Pattern.compile("[^\\./]+/\\.\\./");
		for( int i = 0; i < 10; i++ )
		{
			Matcher match = pattern.matcher(result);
			result = match.replaceFirst("");
		}
		//log("	Beautified	"+path+"\n	to		"+result);
		return result;
	}

}
