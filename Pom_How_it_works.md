# How it works #

Following approach is used in this utility


  * Index of all packages and classes is created
  * Maven try to compile POM if it exists or create empty one
  * Parse errors for missing packages and classes
  * Search index for packages and classes
  * Install appropriate JARs into local Maven repository
  * Include JARs into dependencies of POM file
  * Go to compilation step until no errors or more than 10 steps

This approach is not always fulfilling 100% of dependencies. In most cases if tool could not resolve dependency it will be interrupted by steps count limitation.


After this you can compile POM by yourself and try to resolve issue. When issue is resolved you can run tool again on the modified POM file.