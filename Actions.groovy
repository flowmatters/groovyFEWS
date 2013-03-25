import org.codehaus.groovy.tools.xml.DomToGroovy
import org.codehaus.groovy.control.CompilationFailedException
import javax.xml.parsers.DocumentBuilderFactory
import com.google.common.io.Files

class Actions
{
	static def compileErrors = 0
	static def sourceDirectory = ""
	static def destinationDirectory = ""
	
	static def uncompileConfigurationFile = { sourceFile, destFile ->
	def groovyBegin = 
'''import static GroovyFewsHelpers.*
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

def outputFile = new File(args[0])
def writer = outputFile.newWriter()

def xmlBuilder = new StreamingMarkupBuilder()
xmlBuilder.useDoubleQuotes=true
def builderResult = xmlBuilder.bind {'''

	def groovyEnd = 
'''
}

writer << XmlUtil.serialize(builderResult)
writer.close()
'''

		def groovyFile = new File(destFile)
		def output = groovyFile.newWriter()
		output.print(groovyBegin)

		def factory = DocumentBuilderFactory.newInstance()
		factory.validating = false
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",false)
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
		def builder = factory.newDocumentBuilder()
		def inputStream = new FileInputStream(sourceFile)
		def document = builder.parse(inputStream)
		def converter = new DomToGroovy(new PrintWriter(output))

		converter.print(document)
		output.print(groovyEnd)			
		output.close()

		true
	}

	static def ignoreFile = { sourceFile, destFile ->
		false
	}
	
	static def copyFile = { sourceFile, destFile ->
		Files.copy(new File(sourceFile),new File(destFile))
		
		true
	}

	static def compileConfigurationFile = { sourceFn, destFn ->	
		def sourceFile = new File(sourceFn)
		def sourceFileFolder = sourceFile.parentFile
		
		def shell = buildShell(sourceFileFolder)
//		debug "Running $destFn with classpath=${shell.classLoader.classPath.join('\n')}"
		
		try
		{
			shell.run(sourceFile,destFn,sourceFileFolder.path)
		}
		catch(CompilationFailedException cfe)
		{
			println "*** Error compiling groovy code"
			println "ERROR: $cfe.message"
			compileErrors++
			return false
		}
		catch(Exception e)
		{
			println "*** Error building $destFn from $sourceFn"
			println "ERROR: $e.message ($e.metaClass)"
			println "LOCATION:"
			println "* ${e.stackTrace[1..Math.min(10,e.stackTraceDepth)].join("\n * ")}"
			compileErrors++
			return false			
		}
		true
	}

	static def buildShell(sourceFileFolder)
	{
		def baseFolder = new File(sourceDirectory)
		
		def shell = new GroovyShell()
			
		def folder = sourceFileFolder;
		
		while( folder != baseFolder )
		{
			shell.classLoader.addClasspath(folder.path)
			folder = folder.parentFile			
		}

		shell.classLoader.addClasspath(baseFolder.path)
				
		shell 
	}
		
}