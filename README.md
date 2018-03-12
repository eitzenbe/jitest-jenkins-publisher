# jitest-jenkins-publisher

![](http://eitzen.at/wp-content/uploads/2018/01/jitestbanner-small.png) 

A jenkins CI plugin to allow to export job results to JiTest Testmanagement plugin for JIRA.

JiTest Testmanagement plugin is hosted at [http://eitzen.at/index.php/jitest-testmanagement-plugin-for-jira/]

The publisher expects the job to return a junit.xml file based upon the mystical specification of the JUnit lib.

## Installing the plugin

You can build the HPI plugin by cloning this repo and running ```mvn package``` or download the latest hpi file from the ```dist``` subfolder.

## Configuring the plugin
The plugin needs the following information to perform its action:

* URL to JIRA server, where JiTest plugin is installed and enabled
* credentials (user, password) that are allowed to perform REST requests on the specified JIRA server
* JIRA key of the JiTest test plan issue, to which the result of the performed job should be associated with ()by creating an associated JiTest test report issue and related test executions).

## JUnit XML specification

The plugin requests specification of the plan ticket key, then a report shall be created and the corresponding test case results shall be set in the related executions.
Test execs will be identified based on the **id** attribute of the testcase tag which must start with the corresponding
 JIRA JiTest test case key (WIHTOUT the dash "-"):

The first characters up to any non letter / non digit are checked for being a valid JIRA key:

As an example: ```JIT18-ThisTestsTheLogin``` will be identified as ```JIT-18```

White spaces will also be used as indicator for the end of the JIRA Key reference. 
Please also note that there is no dash to be used to separate the project key from the number part, thus the first digit will be used to indicate the end of the project key part.

XML Example:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites>
  <testsuite name="et.seleniet.testruns.testcases" tests="84" id="0" >
    <testcase id="JIT94-createEmptyProject" name="Create QA project" classname="et.seleniet.testruns.testcases" time="274">
    </testcase>
    ... other test cases
    <testcase id="JIT125-checkNoBinInPlanTree" name="check only first level has trash bin icon (to delete a node) in plan tree popup" classname="et.seleniet.testruns.testcases" time="312">
      <failure><![CDATA[et.seleniet.api.AbortException: RUN User requested test run abort!
Master test case:JIT125-checkNoBinInPlanTree.7
Failure stack: JIT125-checkNoBinInPlanTree - check only first level has trash bin icon (to delete a node) in plan tree popup &gt;&gt; sub-login {18}
Screen dump:None
	at et.seleniet.core.SelenietImpl.runTestcase(SelenietImpl.java:930)
	at et.seleniet.core.SelenietImpl.runTest(SelenietImpl.java:1640)
	at et.seleniet.core.composer.ExecuteConfigurator$4.run(ExecuteConfigurator.java:717)
Caused by: et.seleniet.api.UserQuitException: RUN User requested test run abort!
	at et.seleniet.core.SelenietImpl.runTestcase(SelenietImpl.java:788)
	at et.seleniet.plugins.SubtestPlugin.execute(SubtestPlugin.java:63)
	at et.seleniet.core.SelenietImpl.runTestcase(SelenietImpl.java:846)
	... 2 more
--------------
Console Dump:
20:42:26.021 INFO - JIT125-checkNoBinInPlanTree - RESUME TESTCASE JIT125-checkNoBinInPlanTree
20:42:26.021 INFO - JIT125-checkNoBinInPlanTree - 
20:42:26.021 INFO - JIT125-checkNoBinInPlanTree - 
20:42:26.021 INFO - JIT125-checkNoBinInPlanTree - END TESTCASE JIT125-checkNoBinInPlanTree
20:42:26.021 INFO - -------------------------------------------------------------------
20:42:26.021 INFO - JIT125-checkNoBinInPlanTree - 
      ]]></failure>
    </testcase>
    <testcase id="JIT126-checkTopNavLinks" name="check all TopNav Links" classname="et.seleniet.testruns.testcases" >
      <error><![CDATA[Not executed / Blocked due to earlier errors
      ]]></error>
    </testcase>
  </testsuite>
</testsuites>
```

So the result of this XML file would be that (amongst others) the execution of the test case JIT-94 will be marked as passed, while the executions of JIT-125 and JIT-126 will be marked as *Failed*.

You can also use a ```status``` attribute in the <testcase> tag. However any failed or error child node will overwrite the ```status``` attribute and mark the test case as *Failed*.

Possible values for the status attribute are:
* *Not executed*
* *Failed*
* *Passed*
* *Blocked*
* *Work in progress*
