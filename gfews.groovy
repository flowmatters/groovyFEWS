#!/usr/bin/env groovy
import com.google.common.io.Files
import javax.xml.parsers.DocumentBuilderFactory
import org.codehaus.groovy.tools.xml.DomToGroovy
import org.codehaus.groovy.control.CompilationFailedException

class gfews
{
	static int main(String[] args)
	{
		def cmdText =              (args.length>0)?args[0]:"unknown"
		sourceDirectory =      (args.length>1)?args[1]:null
		destinationDirectory = (args.length>2)?args[2]:null
		
		def cmd = match(cmdText)
		cmd(cmdText,sourceDirectory,destinationDirectory)
		errorCount
	}

	static def sourceDirectory = ""
	static def destinationDirectory = ""
	static def dirCount = 0;
	static def filesProcessed = 0
	static def filesSkipped = 0
	static def errorCount = 0
	static def errorThreshold = 5
	static def faillingFileModificationTimeStamps = [:]
	
	static def commands = [
	compile: {cmd,from,to -> compile(from,to)},
	uncompile: {cmd,from,to -> uncompile(from,to)},
	help: {cmd,from,to -> help()},
	autocompile: {cmd,from,to -> autocompile(from,to)},
	unknown: {cmd,from,to -> errorUnknown(cmd); help()}
	]

	static def matchCommandName(requestedCommand)
	{
		def pair = commands.find{it.key.find("^$requestedCommand")}
		
		pair ? pair.key : "unknown"
//		for(it in commands)
//			if(it.key.find("^$requestedCommand"))
//				return it.key
//		"unknown"
	}

	static def match(requestedCommand)
	{
		commands[matchCommandName(requestedCommand)]
	}

	static showDebug=false

	static def skipFile(sourceFile, destinationFile)
	{
		destinationUpToDate(sourceFile,destinationFile) || sourceFailling(sourceFile)
	}
	
	static def sourceFailling(sourceFile)
	{
		faillingFileModificationTimeStamps.containsKey(sourceFile.path) &&
		faillingFileModificationTimeStamps[sourceFile.path] == sourceFile.lastModified()
	}
	
	static def destinationUpToDate(sourceFile,destinationFile)
	{
		destinationFile.exists() && (destinationFile.lastModified()>sourceFile.lastModified())	
	}

	static def debug(msg)
	{
		if(showDebug)
			println msg
	}

	static def log(msg)
	{
		println msg
	}

	static def errorUnknown(requestedCommand)
	{
		println "Unknown command: $requestedCommand"
	}
	
	// Top level commands
	static def compile(from, to)
	{
		processTree(from,to,compileActions)
	}

	static def uncompile(from, to)
	{
		processTree(from,to,uncompileActions)
	}

	static def help()
	{
		println(
"""Usage:
     (WINDOWS) groovy gfews.groovy <cmd> <sourceFolder> <destinationFolder>
     (UNIX)    gfews.groovy <cmd> <sourceFolder> <destinationFolder>
		
   Where <cmd>:
     compile:     Compile a FEWS configuration under destinationFolder,
                  using the groovy source in sourceFolder
     uncompile:   Generate an initial groovy based FEWS configuration
                  under destinationFolder using the XML FEWS configuration 
                  under sourceFolder
     autocompile: Perform an initial compilation,
                  then remain open watching sourceFolder, (re)compiling
                  individual files as they change
		
groovyFEWS -- The fun way to develop your FEWS configuration. 
			  Maintained by Joel Rahman (joel@flowmatters.com.au)""")
	}

	static def autocompile(from, to)
	{
		println "Starting autocompiler!"
		while(true)
		{
			compile(from,to)
			(new Object()).sleep(1000)
		}
	}

	static def actionPack(actions,fn)
	{
		// todo:groovify -- find...
		for( a in actions )
			if(fn.find(a.pattern))
				return a

		return "Error: No action matching $fn"		
	}

	static def actionFor(actions,fn)
	{
		return actionPack(actions,fn).action
	}
	
	static def destinationFn(actions,fn)
	{
		def ap = actionPack(actions,fn)
		
		if( ap.translation )
			return fn.replaceAll(java.util.regex.Pattern.compile(ap.pattern),ap.translation)
		return fn
	}
	
	static def processTree(from,to,actions)
	{
		def sourceDir = new File(from)
		(new File(to)).mkdirs()
		def sep = System.getProperty("file.separator")
		
		errorCount = 0
		
		sourceDir.traverse {
			def relativeFn = it.path.replace("$from$sep","")
		    def destFn = "$to$sep$relativeFn"
			def destFile = new File(destFn)
			
			if(errorCount >= errorThreshold)
			{
				println("Too many errors: Bailling out")
				return
			}
			
			if(it.directory)
			{
				debug "Creating $destFn"
				destFile.mkdirs()
				dirCount++
			}
			else
			{			
				def action = actionFor(actions,it.name)
				destFn = destinationFn(actions,destFn)
				destFile = new File(destFn)
				
				if(skipFile(it,destFile))
				{
					debug("Skipping $relativeFn, $destFn is newer")
					filesSkipped++
				}
				else
				{
					def result = action(it.path,destFn)
					if(result)
						log "--- $relativeFn --- "						
					filesProcessed++
				}
			}
		}
	}

	// Individual actions
	static def copyFile = { sourceFile, destFile ->
		Files.copy(new File(sourceFile),new File(destFile))
		
		true
	}
	
	static def uncompileConfigurationFile = { sourceFile, destFile ->
		def groovyBegin = 
'''import static GroovyFewsHelpers.*
import groovy.xml.MarkupBuilder

def outputFile = new File(args[0])
def writer = outputFile.newWriter()
def xmlBegin = '<?xml version="1.0" encoding="UTF-8"?>'
writer.println(xmlBegin)

def xmlBuilder = new MarkupBuilder(writer)
xmlBuilder.doubleQuotes=true
xmlBuilder.'''

		def groovyEnd = 
'''
writer.close()
'''

		def groovyFile = new File(destFile)
		def output = groovyFile.newWriter()
		output.print(groovyBegin)
	
		def builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
		def inputStream = new FileInputStream(sourceFile)
		def document = builder.parse(inputStream)
		def converter = new DomToGroovy(new PrintWriter(output))

		converter.print(document)
		output.print(groovyEnd)			
		output.close()
		
		true
	}
	
	static def ignoreFile = { sourceFile, destFile ->
		debug "Skipping $sourceFile"
		
		false
	}
	
	static def buildShell(sourceFileFolder)
	{
		def baseFolder = new File(sourceDirectory)
		
		def shell = new GroovyShell()
		
		shell.classLoader.addClasspath(baseFolder.path)
		
		def folder = sourceFileFolder
		while( folder != baseFolder )
		{
			shell.classLoader.addClasspath(folder.path)
			folder = folder.parentFile
		}
		
		shell 
	}
	
	static def compileConfigurationFile = { sourceFn, destFn ->	
		def sourceFile = new File(sourceFn)
		def sourceFileFolder = sourceFile.parentFile
		
		def shell = buildShell(sourceFileFolder)
		debug "Running $destFn with classpath=${shell.classLoader.classPath.join('\n')}"
		
		try
		{
			shell.run(sourceFile,destFn,sourceFileFolder.path)
			if(faillingFileModificationTimeStamps.containsKey(sourceFile.path))
				faillingFileModificationTimeStamps.remove(sourceFile.path)
		}
		catch(CompilationFailedException cfe)
		{
			println "*** Error compiling groovy code"
			println "ERROR: $cfe.message"
			faillingFileModificationTimeStamps[sourceFile.path]=sourceFile.lastModified()
			errorCount++
			return false
		}
		catch(Exception e)
		{
			println "*** Error building $destFn from $sourceFn"
			println "ERROR: $e.message ($e.metaClass)"
			println "LOCATION:"
			println "* ${e.stackTrace[1..Math.min(10,e.stackTraceDepth)].join("\n * ")}"
			faillingFileModificationTimeStamps[sourceFile.path]=sourceFile.lastModified()
			errorCount++
			return false			
		}
		true
	}

	// Action sets
	static def uncompileActions = [
		[pattern:".xml\$", translation:".groovy", action:uncompileConfigurationFile],
		[pattern:".cbin\$", action:ignoreFile],
		[pattern:".", action:copyFile]
	]
	
	static def compileActions = [
		[pattern:"^__", action:ignoreFile],
		[pattern:".cbin\$", action:ignoreFile],
		[pattern:".groovy\$" , translation:".xml", action:compileConfigurationFile],
		[pattern:".", action:copyFile]
	]
}