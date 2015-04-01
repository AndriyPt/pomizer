# Why we need such tool #

Here is one of the scenarios where this tool can be useful. On some big or medium projects for support we have a lot of the source code in the different branches and different repositories supported by different teams. We have environment with already installed application (it means that we know root folder(s) for all JARs which are used in the project).

Project files which are present with partially accessible sources are


  * for propriety IDE
  * referencing libraries which are available and updated in the local infrastructure of the customer. And you have slow or limited access to it.
  * Missing project files at all
  * No maven support


In order to setup project in such environment you need to spend a lot of time resolving different dependencies issues and compilation issues. Almost 100% automatic approach is proposed using Pomizer toolset.