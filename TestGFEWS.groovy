
import static gfews.*

class TestGFEWS extends GroovyTestCase
{
	void testMatchCommand()
	{
		assertEquals "Match compile", "compile", matchCommandName("c")
		assertEquals "Match uncompile", "uncompile", matchCommandName("unc")
		assertEquals "Match autocompile", "autocompile", matchCommandName("autocompile")
		assertEquals "Fail match", "unknown", matchCommandName("bb")
		assertEquals "Fail match", "unknown", matchCommandName("compilee")
	}
	
	void testMatchAction()
	{
		assertEquals "Skip cbin", ignoreFile, actionFor(compileActions, "cache.cbin")
		assertEquals "Match compile groovy", compileConfigurationFile, actionFor(compileActions, "testin.groovy")
		assertEquals "Skip compile helper groovy", ignoreFile, actionFor(compileActions, "__testin.groovy")
		assertEquals "Copy some other file", copyFile, actionFor(compileActions, "mapfile.shp")
	}
	
	void testTranslations()
	{
		assertEquals "comp: groovy -> xml", "test.xml", destinationFn(compileActions, "test.groovy")
		assertEquals "comp: xml -> xml", "test.xml", destinationFn(compileActions, "test.xml")
		assertEquals "uncomp: xml -> groovy", "test.groovy", destinationFn(uncompileActions, "test.xml")
		assertEquals "comp: shp -> shp", "test.shp", destinationFn(compileActions, "test.shp")
	}
}