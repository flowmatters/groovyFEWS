# groovyFEWS

groovyFEWS manages translations of [Delft-FEWS][fews] configurations between the application\'s native XML and the Groovy programming language. Configurations can be authored and maintained in groovy, giving you opportunities to create sophisticate FEWS setups with less duplication and externalisation of data.

By [DRYing][DRY] up your FEWS configuration, broad changes can be made quickly and safely.

## Basic Operation

groovyFEWS itself does two things:

1. **uncompile**: Convert an existing, XML based FEWS configuration to groovy code
2. **compile**: Translate a groovy based FEWS configuration to an XML based configuration for loading into FEWS

Where you have an existing FEWS configuration in XML, you would use the uncompile command once to create the Groovy based configuration and then you would maintain this version, using the compile command to regenerate fresh XML for loading into FEWS.

Both commands take an entire directory structure and create a mirrored structure where either the XML files are translated to Groovy or vice versa. Other files in the directory structure are copied into the corresponding location in the mirror, so common files such as .shp, .dbf, .png, etc can be stored in the groovy based directory structure, making this the new point of truth for the configuration.

![folderStructure]

## Getting Started


1. Install prerequisites:
  1. [JDK] if you haven\'t already got it,
  2. The [Groovy language][groovy] and configure your path to find the groovy toolset,
  3. The [Google Guava library][guava] for some supporting functionality (eg efficient file copy implementation) and put it somewhere in the groovy classpath (simplest is the groovy folder)
2. Download groovyFEWS
3. \"Uncompile\" an existing XML based FEWS configuration into Groovy: 

```
groovy -classpath <path to groovyFEWS> \
       <path to groovyFEWS>\gfews.groovy uncompile \
       <path to existing FEWS configuration> \
       <destination path to new Groovy>
```

You can then examine and edit the groovy configuration files and \"compile\" back to xml.

### Prerequisites

groovyFEWS is built in Groovy, so you need [that][groovy]. Groovy is a Java Virtual Machine language, so you\'ll need one of [those][JDK] (but you should already have it since you\'re running FEWS). We also use the Google Guava library for a few things, so you\'ll need to get [that one too][guava].

On Windows you have the option of downloading Groovy as an installer or as a zip file. On other platforms you just download the archive. The installer has the advantage of configuring the environment variables for you, but there are only two you need:

```
%GROOVY_HOME%=<path to groovy>
%PATH%=%PATH%:%GROOVY_HOME%\bin
```

(or equivalent on Linux).

### Using groovyFEWS

groovyFEWS supports three command, `compile`, `uncompile` and `autocompile`.

```
Usage:
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
```

### Editing the .groovy configuration files

After an uncompile, the various groovy files should very similar to your original XML files, just with Groovy syntax.

```xml
<explorerTask name="Archiving">
  <iconFile>%FEWSDIR%/Icons/folder.png</iconFile>
  <mnemonic>V</mnemonic>
  <arguments>ArchiveDialog</arguments>
  <taskClass>nl.wldelft.fews.gui.plugin.taskRunDialog.TaskRunDialogFewsExplorerPlugin</taskClass>
  <toolbarTask>false</toolbarTask>
  <menubarTask>true</menubarTask>
  <allowMultipleInstances>false</allowMultipleInstances>
  <accelerator>ctrl V</accelerator>
</explorerTask>
```

becomes

```groovy
explorerTask(name:'Archiving') {
  iconFile('%FEWSDIR%/Icons/folder.png')
  mnemonic('V')
  arguments('ArchiveDialog')
  taskClass('nl.wldelft.fews.gui.plugin.taskRunDialog.TaskRunDialogFewsExplorerPlugin')
  toolbarTask('false')
  menubarTask('true')
  allowMultipleInstances('false')
  accelerator('ctrl V')
}
```

In essence, there are ways in which the FEWS configuration elements appear:

Simple elements appear in the form `elementName('elementContent')`, as in

```groovy
toolbarTask('false')
```

While nested elements appear as `elementName(attribute1Name:attribute1Value,...){ /* Inner elements */ }`, as in

```groovy
explorerTask(name:'Archiving') {
  iconFile('%FEWSDIR%/Icons/folder.png')
  . . .
}
```

It\'s possible to write the entire configuration this way, mimicking the XML structure and essentially just avoiding XML closing tags (`</elementName>`). The real power of the system comes in being able to construct more complex configurations using loops, logic and reusable methods.

For example, you can use the Groovy array initialiser to quickly set up the accumulation window for a grid display:

```groovy
[6,12,24,36].each{ hr ->
    	movingAccumulationTimeSpan(multiplier:hr, unit:'hour')
}
```

compiles to XML as:

```xml
<movingAccumulationTimeSpan multiplier="6" unit="hour" />
<movingAccumulationTimeSpan multiplier="12" unit="hour" />
<movingAccumulationTimeSpan multiplier="24" unit="hour" />
<movingAccumulationTimeSpan multiplier="36" unit="hour" />
```

If the same set of configuration elements are required in multiple places, it\'s simple to extract a reusable method. For example, if the same set of `<movingAccumulatiopnTimeSpan>` elements are required on multiple `<gridPlot>` elements, you can extract a method to the top of the groovy file and call it where needed:
	
```groovy
def standardAccumulations(delegateFromCaller)
{
	[6,12,24,36].each{ hr ->
	    	delegateFromCaller.movingAccumulationTimeSpan(multiplier:hr, unit:'hour')
	}
}

// Later, where its needed
  standardAccumulations(delegate)
```

When extracting methods, the key thing is to include a parameter to exchange the current value of `delegate`. `delegate` provides a context for the current element in the XML building process and is updated as you move in and out of nested elements.

### Adding other (non groovy/xml) files

Whenever you add a file to your groovy configuration directory structure, it will get processed on the next compile. Non-groovy files simply get copied to the corresponding location in the XML directory structure. So, for example, to add a new shapefile to the `MapLayerFiles` directory, simply copy the shapefile (.shp, .dbf, .prj, etc) into the `MapLayerFiles` directory in the groovy configuration.

There are exceptions to the copy rule: some files are ignored (.cbin) and files starting with `__` are treated as helper files and are discussed next.

### Helper Files

In the compilation process, filenames starting with `__` are treated as helpers, to be used by one or more of the .groovy files. These helper files can be other groovy files, which can be selectively imported in your groovy code:

```groovy
import __MappingHelpers
import static __GlobalHelpers.*
```

Alternatively, the helper files can be data files to be processed in your groovy code:

```groovy
workDir = args[1] // Your groovy code is invoked with two command line arguments, the second is the working directory
table = loadTable("$workDir/__PETPattern.csv")
```

#### Finding Helpers

In compiling your groovy configuration files, groovyFEWS provides your code with a classpath containing the current directory, and every parent directory up to the base directory of the configuration. For example, in compiling `GridDisplay.groovy`, which would usually be stored in `<baseDir>\Config\DisplayConfigFiles`, the classpath would include

* `<baseDir>`
* `<baseDir>\Config`
* `<baseDir>\Config\DisplayConfigFiles`

so groovy import statements would pick up helper classes stored in any of these directories.

A Helper should be placed as close to where it will be used as possible. Where a single Helper will be used by multiple files from different directories, put it in the common parent directory. For example, a `__MappingHelpers` class that is used by the `Explorer`, `GridDisplay` and reporting configuration files, would be placed in the `<baseDir>\Config` folder.

#### Structuring Helper classes

Helper code files (as opposed to data files), are Groovy classes. Any class design is possible, but it can be convenient (if not elegant!) to use `static` convenience methods. For example, the `__MappingHelpers` class could include code to configure standard layers used in various spatial displays

```groovy
static def stdMapSetup(delegate)
{
	delegate.geoDatum('WGS 1984')
    delegate.projection('orthographic')
	allExtents(delegate,0)
    delegate.scaleBarVisible('true')
    delegate.northArrowVisible('true')
    delegate.labelsVisible('true')
    delegate.backgroundColor('light sky blue2')
}
```

#### Generic Helpers

groovyFEWS includes a small set of generic helpers in the `GroovyFewsHelpers` class. More helpers will move into here over time (and contributions are very welcome!).

### Feedback

Feedback, suggestions and contributions are all very welcome!

[fews]: http://www.deltares.nl/en/software/479962/delft-fews
[folderStructure]: docs/translation.png
[guava]: https://code.google.com/p/guava-libraries/
[groovy]: http://groovy.codehaus.org/
[JDK]: http://www.java.com/
[DRY]: http://en.wikipedia.org/wiki/Don't_repeat_yourself
