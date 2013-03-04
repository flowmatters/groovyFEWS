
import static gfews.*
import Actions

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
		assertEquals "Skip cbin", Actions.ignoreFile, actionFor(compileActions, "cache.cbin")
		assertEquals "Match compile groovy", Actions.compileConfigurationFile, actionFor(compileActions, "testin.groovy")
		assertEquals "Skip compile helper groovy", Actions.ignoreFile, actionFor(compileActions, "__testin.groovy")
		assertEquals "Copy some other file", Actions.copyFile, actionFor(compileActions, "mapfile.shp")
	}
	
	void testTranslations()
	{
		assertEquals "comp: groovy -> xml", "test.xml", destinationFn(compileActions, "test.groovy")
		assertEquals "comp: xml -> xml", "test.xml", destinationFn(compileActions, "test.xml")
		assertEquals "uncomp: xml -> groovy", "test.groovy", destinationFn(uncompileActions, "test.xml")
		assertEquals "comp: shp -> shp", "test.shp", destinationFn(compileActions, "test.shp")
	}
}