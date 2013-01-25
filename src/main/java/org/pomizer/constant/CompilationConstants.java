package org.pomizer.constant;

import java.util.regex.Pattern;

public class CompilationConstants {

    public static final String BUILD_SUCCESSFUL = "[INFO] BUILD SUCCESSFUL";
    
    private static final String ERROR_PREFIX = Pattern.quote("[ERROR]");
    
    private static final String WARNING_PREFIX = Pattern.quote("[WARNING]");
    
    private static final String INFO_PREFIX = Pattern.quote("[INFO]");
    
    // [ERROR] COMPILATION ERROR : 
    public static final Pattern COMPILATION_ERRORS_SECTION_START = Pattern.compile("^" + ERROR_PREFIX 
            + "\\s+COMPILATION ERROR\\s*:?.*$");
    
    public static final Pattern ERROR_LINE = Pattern.compile("^" + ERROR_PREFIX + ".*$");
    
    public static final Pattern ANY_MESSAGE_TYPE_LINE = Pattern.compile("^(" + WARNING_PREFIX + "|" 
            + INFO_PREFIX + "|" + ERROR_PREFIX + ").*$"); 

    // [ERROR] /C:/com/test/my/MyClass.java:[78,49] C:/com/test/my/MyClass.java:78: cannot access javax.ejb.CreateException
    // class file for javax.ejb.CreateException not found
    public static final Pattern CANNOT_ACCESS_CLASS = Pattern.compile("^" + ERROR_PREFIX 
            + ".+cannot access\\s+(\\S+)\\s+class file for.+not found\\s*$");
    
    // [ERROR] /C:/test/src/TestClass.java:[74,53] C:\test\src\TestClass.java:74: cannot find symbol
    // symbol : class TestClass
    // location: package com.test.second.third
    public static final Pattern CANNOT_FIND_CLASS_WITH_PACKAGE = Pattern.compile("^" + ERROR_PREFIX 
            + ".+cannot find symbol\\s+symbol\\s*:?\\s*class\\s+(\\S+)\\s+location\\s*:?\\s+package\\s+(\\S+)\\s*$");
    
    // [ERROR] /C:/test/src/TestClass.java:[74,53] C:\test\src\TestClass.java:74: cannot find symbol
    // symbol  : variable TEST_12
    // location: class com.test.second.third.SomeClass
    
    // [ERROR] /C:/test/src/TestClass.java:[74,53] C:\test\src\TestClass.java:74: cannot find symbol
    // symbol : method send(javax.jms.ObjectMessage)
    // location: interface javax.jms.MessageProducer
    
    // [ERROR] /C:/test/src/TestClass.java:[74,53] C:\test\src\TestClass.java:74: cannot find symbol
    // symbol  : method send(javax.jms.ObjectMessage)
    // location: class javax.jms.MessageProducer
    public static final Pattern CANNOT_FIND_SYMBOL = Pattern.compile("^" + ERROR_PREFIX 
            + ".+cannot find symbol\\s+symbol\\s*:?\\s*(variable|method).+location\\s*:?\\s*(class|interface)\\s+(\\S+)\\s*$");
    
    // [ERROR] /C:/test/src/TestClass.java:[74,53] C:\test\src\TestClass.java:74: cannot find symbol
    // symbol: class Action
    
    // [ERROR] /C:/test/src/TestClass.java:[74,53] C:\test\src\TestClass.java:74: cannot find symbol
    // symbol  : class MyClass
    // location: class com.test.second.third.TestClass
    public static final Pattern CANNOT_FIND_CLASS = Pattern.compile("^" + ERROR_PREFIX 
            + ".+cannot find symbol\\s+symbol\\s*:?\\s*class\\s+(\\S+)(\\s*|\\s+location\\s*:?\\s*(interface|class)\\s+\\S+\\s*)$");
   
    // [ERROR] /C:/test/src/TestClass.java:[42,36] C:/test/src/TestClass.java:18: package com.test.second.third does not exist
    public static final Pattern CANNOT_FIND_PACKAGE = Pattern.compile("^" + ERROR_PREFIX 
            + ".+package\\s+(\\S+)\\s+does not exist\\s*$");
}
