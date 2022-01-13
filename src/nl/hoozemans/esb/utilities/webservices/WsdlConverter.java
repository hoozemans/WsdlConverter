package nl.hoozemans.esb.utilities.webservices;

import java.io.File;
import java.io.PrintWriter;
import java.io.ObjectInputStream.GetField;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.hoozemans.esb.utilities.files.PathUtilities;

public class WsdlConverter 
{
	protected String tempWsdlPath = "C:/Temp/wsdl.wsdl";
	
	public WsdlConverter( String path ) 
	{
	}

	public static void integrateWsdl(String path, String to)
	throws Exception
	{
		Wsdl wsdl = new Wsdl(path);
		
		// get all defining definitions recursively
		List<Definition> definitions = wsdl.getAllDefinitions();

		// first, integrate all definitions into one
		// the target namespace of the remaining definition should
		// be consistent with the message declaration
		Definition definition = null;
		if( definitions.size() == 1 )
		{
			definition = (Definition)definitions.get(0).clone();
		}
		else
		{
			String name = null;
			// first get the lead wsdl
			for( Definition d : definitions )
			{
				if( d.getLocalMessages().size() > 0 )
					definition = (Definition)d.clone();
				
				if( d.getName() != null )
					name = d.getName();
			}
			definition.setName(name);
			
			// then pile all toplevel namespace declarations on top
			// don't use the addAllNamespaces method, because we want 
			// control in case of doubles
			int seqNr = 0;
			for( Definition d : definitions )
			{
				seqNr++;
				if( !d.getPath().equals(definition.getPath()) )
				{
					List<Namespace> ns = d.getLocalNamespaces();
					for( Namespace n : ns )
					{
						if( !definition.addNamespace(n) )
						{
							if( n.isTargetNamespace() )
								; // skip tns
							else if( n.getPrefix() == null || n.getPrefix().equals("") )
								; // skip weird shit
							else
							{
								Namespace top = definition.getNamespaceByPrefix(n.getPrefix());
								if( !top.getUri().equals(n.getUri()) )
								{
									// we've got a duplicate prefix with different uri
									// we're going to recursively alter the namespace 
									// prefix in the *source* definition, *before* we
									// grab any of the other elements from that definition
									String oldprefix = n.getPrefix();
									String newprefix = "def" + Integer.toString(seqNr) + "_" + oldprefix;
									d.replaceNamespacePrefix(oldprefix, newprefix);
								}
							}
						} // if double
					} // for local namespaces
					
					// now add the corrected local namespaces from source to target
					definition.addAllNamespaces(ns);
				} // if not same as target definition
			} // for definitions 
		}
			
		
		// now that we've done our namespace magic, it's safe to gather the 
		// rest of our wsdl recursively
		List<Schema> schemas = wsdl.getUniqueSchemas();
		List<PortType> ports = wsdl.getAllPortTypes();
		List<Binding> bindings = wsdl.getAllBindings();
		List<Service> services = wsdl.getAllServices();
		List<Message> messages = wsdl.getAllMessages();
		
		// and then we start formatting the target definition
		definition.imports = new ArrayList<Import>();

		definition.bindings = new ArrayList<Binding>();
		for( Binding binding : bindings )
			definition.addBinding((Binding)binding.clone());

		definition.portTypes = new ArrayList<PortType>();
		for( PortType porttype : ports )
			definition.addPortType((PortType)porttype.clone());

		definition.messages = new ArrayList<Message>();
		for( Message msg : messages )
			definition.addMessage((Message)msg.clone());

		definition.services = new ArrayList<Service>();
		for( Service svc : services )
			definition.addService((Service)svc.clone());
		
		definition.schemas = new ArrayList<Schema>();
		for( Schema schema : schemas )
		{
			Schema s = (Schema)schema.clone();
			s.removeAllImports();
			//s.addAllNamespaces(definition.getLocalNamespaces());
			s.addEmptyImports();
			definition.addSchema(s);
		}

		PathUtilities.createPathForFile(to);
		PrintWriter writer = new PrintWriter(to, "UTF-8");
		writer.print(definition.getSource());
		writer.close();
	}
	
	public static void splitWsdl(String path, String temp, String to)
	throws Exception
	{
		// first, we need to get all schemas in one consistent wsdl
		integrateWsdl(path, temp);
		
		Wsdl wsdl = new Wsdl(temp);
		Definition definition = wsdl.getAllDefinitions().get(0);
		List<Schema> schemas = definition.getLocalSchemas();
		
		Definition newDefinition = (Definition)definition.clone();
		newDefinition.schemas = new ArrayList<Schema>();
		
		for( Schema schema : schemas )
		{
			// 	1. new inline schema for wsdl
			Schema inlineSchema = (Schema)schema.clone();
			inlineSchema.declarations = new ArrayList<Declaration>();
			inlineSchema.imports = new ArrayList<Import>();
			inlineSchema.skipImport = true;
			inlineSchema.path = to + "/this.wsdl";
			
			// 2a. gather unique names
			List<String> names = new ArrayList<String>();
			for( Declaration d : schema.getLocalDeclarations() )
			{
				if( !names.contains(d.getName()) )
					names.add(d.getName());
			}
			
			// 2b. create a remote schema for each name and gather named declarations
			for( String name : names )
			{
				System.out.println("Creating remote schema for " + name);
				
				Schema remoteSchema = (Schema)schema.clone();
				remoteSchema.declarations = new ArrayList<Declaration>();
				remoteSchema.imports = new ArrayList<Import>();
				remoteSchema.skipImport = true; // don't try to load included schemas!
				remoteSchema.parent = null; // standalone schema
				remoteSchema.path = PathUtilities.createPathFromNamespace(to, remoteSchema.getTargetNamespace().getUri()) + "/" + name + ".xsd";
				
				for( Declaration declaration : schema.getLocalDeclarations() )
				if( declaration.getName().equals(name) )
				{
					System.out.println("	Adding declaration for " + declaration.getDeclarationType() + ":" + declaration.getTargetNamespace().getUri() + ":" + declaration.getName());

					// add the declaration
					remoteSchema.addDeclaration(declaration);
					
					// add includes for type references to remote schema
					String pattern = "(?si)\\s+(type|ref|base)=" + Base.PATTERN_LITERAL + "";
					Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
					Matcher match = regex.matcher(declaration.getSource());
					
					while( match.find() )
					{
						String typeRef = match.group(2);
						String refType = match.group(1);
						if( !typeRef.contains(":") ) continue;

						String prefix = typeRef.replaceAll(":(.*?)$", "");
						String refname = typeRef.replaceAll("^(.*?):", "");
						Namespace ns = declaration.getNamespaceByPrefix(prefix);

						System.out.println("		declaration has type reference: "+refType+" = " + prefix + ":" + refname);
						
						for( Schema s : schemas )
						{
							Declaration typeDecl = null;
							if( refType.equals("ref") )
								typeDecl = s.getLocalDeclaration("element", ns.getUri(), refname);
							if( !refType.equals("ref") && typeDecl == null )
								typeDecl = s.getLocalDeclaration("simpletype", ns.getUri(), refname);
							if( !refType.equals("ref") && typeDecl == null )
								typeDecl = s.getLocalDeclaration("complextype", ns.getUri(), refname);
							if( !refType.equals("ref") && typeDecl == null )
								typeDecl = s.getLocalDeclaration("attribute", ns.getUri(), refname);
							if( typeDecl != null ) 
							{
								System.out.println("		found declaration for type reference: " + ns.getUri() + ":" + typeDecl.getName() + " in schema " + s.getTargetNamespace().getUri());
								
								// first, includes for the remote schema
								{
									String thisSchema = PathUtilities.getPathFromNamespace(to, remoteSchema.getTargetNamespace().getUri() + "/" + declaration.getName() + ".xsd");
									String thatSchema = PathUtilities.getPathFromNamespace(to, s.getTargetNamespace().getUri() + "/" + refname + ".xsd");
									
									if( thisSchema.equals(thatSchema) ) 
									{
										continue;
									}
									
									String relative = PathUtilities.getRelativePath(thatSchema, thisSchema);
									
									if( s.getTargetNamespace().getUri().equals(schema.getTargetNamespace().getUri()) )
									{
										String source = "<xsd:include xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" schemaLocation=\"" + relative + "\" />";
										System.out.println("		adding remote include: " + source);
										Import imp = new Import(remoteSchema, source);
										remoteSchema.addImport(imp);
									}
									else
									{
										String source = "<xsd:import xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" namespace=\""+ns.getUri()+"\" schemaLocation=\"" + relative + "\" />";
										System.out.println("		adding remote import: " + source);
										Import imp = new Import(remoteSchema, source);
										remoteSchema.addImport(imp);
									}
								} // remote
							} // endif typeDecl
						} // for schemas
					} // while type/element reference
				} // for local declarations matching name

				String xsdPrefix = remoteSchema.getSource().replaceFirst("(?si)^(.*?)<"+Base.PATTERN_NSPREFIX+":schema(.*?)$", "$2");
				Namespace ns = remoteSchema.getNamespaceByPrefix(xsdPrefix);
				if( ns == null )
				{
					String nsdecl = " xmlns:"+xsdPrefix+"=\"http://www.w3.org/2001/XMLSchema\" ";
					ns = new Namespace(remoteSchema, nsdecl);
					remoteSchema.addNamespace(ns);
				}
				
				PrintWriter writer = new PrintWriter(remoteSchema.path, "UTF-8");
				writer.print(remoteSchema.getSource());
				writer.close();
				
				// add remote schema to inline schema
				{
					String thisSchema = to + "/this.wsdl";
					String thatSchema = remoteSchema.path;
					String relative = PathUtilities.getRelativePath(thatSchema, thisSchema);
					
					if( inlineSchema.getTargetNamespace().getUri().equals(remoteSchema.getTargetNamespace().getUri()) )
					{
						String source = "<xsd:include xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" schemaLocation=\"" + relative + "\" />";
						System.out.println("		adding inline include: " + source);
						Import imp = new Import(inlineSchema, source);
						inlineSchema.addImport(imp);
					}
					else
					{
						String source = "<xsd:import xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" namespace=\""+remoteSchema.getTargetNamespace().getUri()+"\" schemaLocation=\"" + relative + "\" />";
						System.out.println("		adding inline import: " + source);
						Import imp = new Import(inlineSchema, source);
						inlineSchema.addImport(imp);
					}
				} // remote
				
				
			} // for names
						
			newDefinition.addSchema(inlineSchema);
		} // for local schemas
		
		PrintWriter writer = new PrintWriter(to + "/" + newDefinition.getName() + ".wsdl", "UTF-8");
		writer.print(newDefinition.getSource());
		writer.close();
	}

	public static void main(String[] args)
	throws Exception
	{
		String[] msgIds = {
			"<wsdl-path>"
		};
		
		List<String> notCompleted = new ArrayList<String>();
		
		for( String msgId : msgIds )
		{
			String origPath = "<project-path>/original/" + msgId + "/" + msgId + ".wsdl";
			String integralPath = "<project-path>/integrated/" + msgId + "/" + msgId + ".wsdl";
			String splitPath = "<project-path>/split/" + msgId;
			
			try
			{
				splitWsdl(origPath, integralPath, splitPath);
			}
			catch( Exception e )
			{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				notCompleted.add(origPath + ": " + sw.toString() + "\n");
			}
		}
		
		System.out.println("Not completed: " + notCompleted);
		
		System.out.println("\n\nAll done.\n");
		//integrateWsdl(path, "C:/Temp/wsdl.wsdl");
	}
}
