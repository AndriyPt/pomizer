# How to run #

## Pre-requirements ##

You need to have installed SVN command line tool on your machine. You can install [Slik SVN](http://www.sliksvn.com/en/download) or similar distribution.

Note: svn should also be in the search path of the OS which you are using.

## General information ##
Tool is build from two components.

**First component** `org.pomizer.application.Deployer` is used for deployment of the changes on the server. As command line parameter you should provide full path to configuration file.


Component will create backup folder where all files would be copied before changes will be applied. Also changeset file will be created with listed file in the backup folder and their original path.

**Second component** `org.pomizer.application.Revertor` is used to revert all changes done by Deployer. As command line parameter you should provide full path to configuration file.


This component will copy all files from backup folder to original location based on the changeset file. After this changeset file will be deleted as well as copies of the original files.

## To run application you can use following approach ##

  * Download source code of the tool
  * execute

`mvn compile`

  * run any from two components using command line similar to following

`mvn exec:java -Dexec.mainClass="org.pomizer.application.Deployer" -Dexec.args="c:\Projects\deployer.project"`


## Configuration file ##

Configuration is presented in the form of the XML document with the following structure

```xml

<deployer>

<!-- Global settings (optional) -->
<settings>

<!-- Should project use index to match JARs for updated class. Default true -->
<use_index>true

Unknown end tag for </use\_index>




Unknown end tag for &lt;/settings&gt;



<!-- Location of the index file -->
<index>c:\Projects\MyProject\index\full_jars_index.dat

Unknown end tag for &lt;/index&gt;



<!-- Project entry. Used for changes tracking from SVN -->
<project path="c:\Projects\MyFirstProject">

<!-- Project settings. If present override global settings (optional) -->
<settings>

<use_index>true

Unknown end tag for </use\_index>





Unknown end tag for &lt;/settings&gt;




<!-- Java source code location and output files location -->
<sources path="src\main\java" output="target\classes"/>

<!-- Path to static resources for changes tracking -->
<resources path="static_content\sample\jsps">

<!-- Deployment path of the changed resource on server -->
<target>\\server\webserver\home\sample\jsps

Unknown end tag for &lt;/target&gt;




Unknown end tag for &lt;/resources&gt;





Unknown end tag for &lt;/project&gt;



<project path="C:\Projects\MySecondProject">

<sources path="src\main\java" output="target\classes">

<!-- Additional deployment path of changed class besides JARs determined by index -->
<target>\\server\webserver\webapps\my-second-project.war\WEB-INF\lib\common.jar

Unknown end tag for &lt;/target&gt;





Unknown end tag for &lt;/sources&gt;





Unknown end tag for &lt;/project&gt;



<!-- Command to always copy files to target directories -->
<copy path="C:\Projects\MyThirdProject\output\sample.html">

<target>\\server\webserver\home\sample\html\sample.html

Unknown end tag for &lt;/target&gt;





Unknown end tag for &lt;/copy&gt;



<!-- Call JMX job or any other web method at the end of the deployment -->
<call_url><![CDATA[http://admin:password@server.com:8080/jmx-console/?action=invokeOp&methodIndex=10]]>

Unknown end tag for </call\_url>



<!-- Call command line in case if folder was updated -->
<command updated_path="\\server\webserver\lib\" run="c:\tools\Sysinternals\pskill.exe \\server webserver.exe"/>



Unknown end tag for &lt;/deployer&gt;


```