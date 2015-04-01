# How it works #

Following approach is used in this tool

  * Index of all packages and classes is created (optional)
  * SVN command line get changes of the projects
  * Check if Java sources were changes then update JAR with appropriate class file from compiled sources directory
  * JARs can be determined based on index or provided directly in configuration file
  * Check if resources were changed then copy them on server
  * Directories of deployment are read from configuration file
  * Post process URLs are called
  * Post process commands are called

Post process URLs can be used to reload context using JMX method call

Post process commands can be used to kill web server process in case if update libraries require this.
For this purpose pskill utility for Windows from [Sysinternals suite](http://www.sysinternals.com) can be used.