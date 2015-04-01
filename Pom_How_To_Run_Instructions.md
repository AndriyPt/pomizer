# How to run #

## Pre-requirements ##

You need to have installed Maven on your machine. Please follow these instructions http://maven.apache.org/download.cgi#Installation_Instructions

_**Note:** mvn should also be in the search path of the OS which you are using._

## General information ##
Tool is build from two components.

**First component** `org.pomizer.application.JarIndexer` is used for index creation of all JAR files in the list of folders. Command line parameters for this component are (please keep this order):

`<version> <base path 1> [<base path 2> ... [<base path N>]]`

> `<version>` – any string which represent version of the project

> `<base path K>` - root of the search path for JARs files.

As the result component will produce index called full\_jars\_index.dat in the current directory.

**Second component** `org.pomizer.application.PomFromIndexCreator` is used to generate POM file using Maven and index file. Command line parameters for this component are (please keep this order):

`<index file> <project directory> [<sources relative path, default "src">]`

> `<index file>` – this is file produced by the first component

> `<project directory>` – this is root directory where POM file should be

> `<sources relative path>` ­– directory for sources in the `<project directory>`

Component will take existing POM and install missing JARs to local Maven repository and add missing dependencies to POM file.

_Please note:
  1. POM file will be fully overridden only dependencies section will remain
  1. Sometimes it is not possible to resolve all dependencies correctly. Tool can continue processing the same packages and classes or found none to process. In this can human interaction is needed for recompilation and fixing current compilation issue. After this component can proceed resolving dependencies._

## To run application you can use following approach ##

  * Download source code of the tool
  * execute

`mvn compile`

  * run any from two components using command line similar to following

`mvn exec:java -Dexec.mainClass="org.pomizer.application.PomFromIndexCreator" -Dexec.args="c:\full_jars_index.dat c:\MyProject"`