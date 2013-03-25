#!/usr/bin/env groovy

class gfews
{
	static int main(String[] args)
	{
		def cmdText =              (args.length>0)?args[0]:"unknown"
		Actions.sourceDirectory =      (args.length>1)?args[1]:null
		Actions.destinationDirectory = (args.length>2)?args[2]:null
		
		def cmd = match(cmdText)
		cmd(cmdText,Actions.sourceDirectory,Actions.destinationDirectory)
		Actions.compileErrors
	}

	static def dirCount = 0;
	static def filesProcessed = 0
	static def filesSkipped = 0
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
	}

	static def match(requestedCommand)
	{
		commands[matchCommandName(requestedCommand)]
	}


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
		def initialDelay = 500
		def maxDelay = 5000

		def delay = initialDelay
		println "Starting autocompiler!"
		while(true)
		{
			filesProcessed = 0
			// TODO: This could be more efficient (eg in Java 1.7, using the nio package)
			// Also, this currently doesn't notice deletions/renames in the source folders
			compile(from,to)
			(new Object()).sleep(delay)
			
			if(filesProcessed==0)
				delay = Math.min(maxDelay,delay*2)
			else if(filesProcessed>=3)
				delay = initialDelay
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
	
	static def hiddenFileInPath(fn)
	{
		def sep = System.getProperty("file.separator")
		if(sep=="\\")
			sep="\\\\"
		return fn.split(sep).any{it[0]=="."}
	}
	
	static def processTree(from,to,actions)
	{
		def sourceDir = new File(from)
		(new File(to)).mkdirs()
		def sep = System.getProperty("file.separator")
		
		Actions.compileErrors = 0
		
		sourceDir.traverse {
			def relativeFn = it.path.replace("$from$sep","")
		    def destFn = "$to$sep$relativeFn"
			def destFile = new File(destFn)

			
			if(Actions.compileErrors >= errorThreshold)
			{
				println("Too many errors: Bailling out")
				return
			}

			if(hiddenFileInPath(relativeFn))
			{
				// skip due to hidden file in path
			}
			else if(it.directory)
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
					{
						log "--- $relativeFn --- "
						if(faillingFileModificationTimeStamps.containsKey(it.path))
							faillingFileModificationTimeStamps.remove(it.path)
					}
					else
					{
						faillingFileModificationTimeStamps[it.path]=it.lastModified()						
					}
					filesProcessed++
				}
			}
		}
	}
			
	// Action sets
	static def uncompileActions = [
		[pattern:".xml\$", translation:".groovy", action:Actions.uncompileConfigurationFile],
		[pattern:".cbin\$", action:Actions.ignoreFile],
		[pattern:".", action:Actions.copyFile]
	]
	
	static def compileActions = [
		[pattern:".bak\$",action:Actions.ignoreFile],
		[pattern:".~\$",action:Actions.ignoreFile],
		[pattern:"^__", action:Actions.ignoreFile],
		[pattern:".cbin\$", action:Actions.ignoreFile],
		[pattern:".groovy\$" , translation:".xml", action:Actions.compileConfigurationFile],
		[pattern:".", action:Actions.copyFile]
	]
	
	static showDebug=false

	static def debug(msg)
	{
		if(showDebug)
			println msg
	}

	static def log(msg)
	{
		println msg
	}
}