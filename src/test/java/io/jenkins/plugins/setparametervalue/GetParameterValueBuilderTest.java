package io.jenkins.plugins.setparametervalue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Label;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;

/**
 * Test cases to test scripted pipeline for get operation.
 * 
 * @author Andrejus Chaliapinas
 *
 */
public class GetParameterValueBuilderTest {

  private static Logger LOGGER = LogManager.getLogger();

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  final String name = "Foo";
  final String job = "test-scripted-pipeline";
  final String run = "1";

  @Test
  public void testGetScriptedPipelineMultiline() throws Exception {
    String agentLabel = "my-agent";
    jenkins.createOnlineSlave(Label.get(agentLabel));
    WorkflowJob jobObj = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
    ParameterDefinition paramDef = new StringParameterDefinition(name, "Foo1");
    jobObj.addProperty(new ParametersDefinitionProperty(paramDef));
    
    String pipelineScript
            = "node {\n"
            +   "// Here need to use such constructor due to presence in whitelist of only applicable signature\n"
            + "  def valueList = new java.util.ArrayList(new java.util.HashSet())\n"
            + "  getParameterValue(\n"
            +    "'name' : '" + name + "',\n"
            +    "'job' : '" + job + "',\n"
            +    "'run' : '" + run + "',\n"
            +    "'list' : valueList\n"
            +    ")\n"
            +   "def retValue = valueList.get(0).toString()\n"
            +   "echo 'Returned value is: ' + retValue\n"
            + "}";
    jobObj.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(jobObj.scheduleBuild2(0));
    LOGGER.info("Run log: " + JenkinsRule.getLog(completedBuild));
    LOGGER.info("Run plugins: "  + jenkins.getPluginManager().getPlugins());
    LOGGER.info("Run completedBuild: "  + completedBuild);
    //String expectedString = "GetParameterValue with parameter: Foo, job: " + job + ", and job's run: " + run;
    //jenkins.assertLogContains(expectedString, completedBuild);
  }
}