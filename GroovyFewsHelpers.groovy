//package com.flowmatters.groovyfews

class GroovyFewsHelpers
{
static def tsDefaults=[valueType:'scalar',timeSeriesType:'external historical',readWriteMode:'add originals',synchLevel:1]

static def monthsOfTheYear=[
	'January','February','March','April','May','June',
	'July','August','September','October','November','December']
static def daysInMonth=[31,29,31,30,31,30,31,31,30,31,30,31]

static def fromArgsOrDefault(element,parent,args)
{
  if(args[element])
  {    
    parent.nodeCompleted(parent,parent.createNode(element,args[element]))
  }
  else if(tsDefaults[element])
  {
    parent.nodeCompleted(parent,parent.createNode(element,tsDefaults[element]))
  }
}

static def buildTimeSeriesSet(args,parent)
{
  parent.timeSeriesSet(){
    ['moduleInstanceId','valueType','parameterId','locationSetId','timeSeriesType',
    'timeStep','relativeViewPeriod','readWriteMode','synchLevel'].each{
      fromArgsOrDefault(it,delegate,args)
    }
  }
}

static def loadTable(fn)
{
	def f = new File(fn)
	csvTable(f.readLines())
}

static def csvTable(lines)
{
	def result = []
	lines.each{
		result.push it.split(',')
	}
	result	
}
}