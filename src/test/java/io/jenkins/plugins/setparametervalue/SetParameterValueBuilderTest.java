package io.jenkins.plugins.setparametervalue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.common.base.Charsets;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import jenkins.model.Jenkins;

public class SetParameterValueBuilderTest {

  private static Logger LOGGER = LogManager.getLogger();

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @SuppressWarnings({"checkstyle:membername"})
  final String _class = "hudson.model.StringParameterValue";
  final String name = "paramname";
  final String value = "paramvalue";
  final String job = "test-scripted-pipeline";
  final String run = "1";

  @Test
  public void testConfigRoundtrip() throws Exception {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    // Let's have Jenkins version in test output
    LOGGER.info("Jenkins version: " + Jenkins.getVersion());
    LOGGER.info("Check display name: "
        + new SetParameterValueBuilder(null, name, value, job, run).getDescriptor().getDisplayName());
    project.getBuildersList().add(new SetParameterValueBuilder(null, name, value, job, run));
    LOGGER.info("Initial project.getBuildersList(): " + project.getBuildersList());
    project = jenkins.configRoundtrip(project);
    LOGGER.info("After roundtrip project.getBuildersList(): " + project.getBuildersList());
    // null will be saved/returned as empty string, so compare to that
    jenkins.assertEqualDataBoundBeans(new SetParameterValueBuilder("", name, value, job, run),
        project.getBuildersList().get(0));
  }

  @Test
  public void testBuild() throws Exception {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    project.addProperty(new ParametersDefinitionProperty(paramDef));
    SetParameterValueBuilder builder = new SetParameterValueBuilder(null, "Foo", "Foo2", project.getName(), "1");
    project.getBuildersList().add(builder);
    LOGGER.info("Project project.getBuildersList(): " + project.getBuildersList());

    FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
    jenkins.assertLogContains("SetParameterValue with parameter: Foo, job: " + project.getName(), build);
  }

  @Test
  public void testPostCall() throws Exception {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    project.addProperty(new ParametersDefinitionProperty(paramDef));
    SetParameterValueBuilder builder = new SetParameterValueBuilder(null, "Foo", "Foo2", project.getName(), "1");
    project.getBuildersList().add(builder);

    FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

    String setValueUrl = "plugin/set-parameter-value/setParameterValue";
    String payload = 
        "{\"parameter\":[{\"_class\" : \"hudson.model.StringParameterValue\", "
        + "\"name\":\"Foo\", \"value\":\"Foo3\"}], "
        + "\"job\":\"" + project.getName() + "\", " 
        + "\"run\":\"" + build.getNumber() + "\"}";

    HttpPost httpPost = new HttpPost(jenkins.getURL().toExternalForm() + setValueUrl);

    StringEntity entity = new StringEntity(payload);
    httpPost.setEntity(entity);
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");

    NameValuePair crumb = getCrumbHeaderNvp();
    httpPost.setHeader(crumb.getName(), crumb.getValue());
    LOGGER.info("crumb.getName(): " + crumb.getName() + ", crumb.getValue(): " + crumb.getValue());

    CloseableHttpClient client = HttpClients.createDefault();
    CloseableHttpResponse response = client.execute(httpPost);
    assertThat("Status is 200", response.getStatusLine().getStatusCode(), equalTo(200));
    LOGGER.info("testPostCall Response: " + EntityUtils.toString(response.getEntity(), Charsets.UTF_8));
    String buildUrlPretty = "job/" + project.getName() + "/" + build.getNumber() + "/api/json?pretty=true";
    LOGGER.info("testPostCall build JSON: " + jenkins.getJSON(buildUrlPretty).getContentAsString());
    String buildUrl = "job/" + project.getName() + "/" + build.getNumber() + "/api/json";
    jenkins.assertStringContains(jenkins.getJSON(buildUrl).getContentAsString(),
        "\"_class\":\"hudson.model.StringParameterValue\",\"name\":\"Foo\",\"value\":\"Foo3\"");
    client.close();
  }

  @Test
  public void testPostCallAbsentJob() throws Exception {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    project.addProperty(new ParametersDefinitionProperty(paramDef));
    SetParameterValueBuilder builder = new SetParameterValueBuilder(null, "Foo", "Foo2", project.getName(), "1");
    project.getBuildersList().add(builder);

    FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

    String setValueUrl = "plugin/set-parameter-value/setParameterValue";
    String payload = 
        "{\"parameter\":[{\"_class\" : \"hudson.model.StringParameterValue\", "
        + "\"name\":\"Foo\", \"value\":\"Foo3\"}], "
        + "\"job\":\"" + "incorrect_job" + "\", " 
        + "\"run\":\"" + build.getNumber() + "\"}";

    HttpPost httpPost = new HttpPost(jenkins.getURL().toExternalForm() + setValueUrl);

    StringEntity entity = new StringEntity(payload);
    httpPost.setEntity(entity);
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");

    NameValuePair crumb = getCrumbHeaderNvp();
    httpPost.setHeader(crumb.getName(), crumb.getValue());
    LOGGER.info("crumb.getName(): " + crumb.getName() + ", crumb.getValue(): " + crumb.getValue());

    CloseableHttpClient client = HttpClients.createDefault();
    CloseableHttpResponse response = client.execute(httpPost);
    assertThat("Status is 400", response.getStatusLine().getStatusCode(), equalTo(400));
    String responseStr = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
    LOGGER.info("testPostCallAbsentJob Response: " + responseStr);
    jenkins.assertStringContains(responseStr, "\"message\":\"Specified job 'incorrect_job' was not found!\"");
    client.close();
  }

  @Test
  public void testScriptedPipelineSingleline() throws Exception {
    String agentLabel = "my-agent";
    jenkins.createOnlineSlave(Label.get(agentLabel));
    WorkflowJob jobObj = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    jobObj.addProperty(new ParametersDefinitionProperty(paramDef));
    
    String pipelineScript
            = "node {\n"
            + "  setParameterValue "
            +    "'_class' : '" + _class + "', "
            +    "'name' : 'Foo',"
            +    "'value' : 'Foo3',"
            +    "'job' : '" + job + "',"
            +    "'run' : '" + run + "'\n"
            + "}";
    jobObj.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(jobObj.scheduleBuild2(0));
    LOGGER.info("Run log: " + JenkinsRule.getLog(completedBuild));
    LOGGER.info("Run plugins: "  + jenkins.getPluginManager().getPlugins());
    LOGGER.info("Run completedBuild: "  + completedBuild);
    String expectedString = "SetParameterValue with parameter: Foo, job: " + job + ", and job's run: " + run;
    jenkins.assertLogContains(expectedString, completedBuild);
  }

  @Test
  public void testScriptedPipelineMultiline() throws Exception {
    String agentLabel = "my-agent";
    jenkins.createOnlineSlave(Label.get(agentLabel));
    WorkflowJob jobObj = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    jobObj.addProperty(new ParametersDefinitionProperty(paramDef));
    
    String pipelineScript
            = "node {\n"
            + "  setParameterValue(\n"
            +    "'_class' : '" + _class + "',\n"
            +    "'name' : 'Foo',\n"
            +    "'value' : 'Foo4',\n"
            +    "'job' : '" + job + "',\n"
            +    "'run' : '" + run + "'\n"
            +    ")\n"
            + "}";
    jobObj.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(jobObj.scheduleBuild2(0));
    LOGGER.info("Run log: " + JenkinsRule.getLog(completedBuild));
    LOGGER.info("Run plugins: "  + jenkins.getPluginManager().getPlugins());
    LOGGER.info("Run completedBuild: "  + completedBuild);
    String expectedString = "SetParameterValue with parameter: Foo, job: " + job + ", and job's run: " + run;
    jenkins.assertLogContains(expectedString, completedBuild);
  }

  @Test
  public void testScriptedPipelineMultilineIncorrectJob() throws Exception {
    String agentLabel = "my-agent";
    jenkins.createOnlineSlave(Label.get(agentLabel));
    WorkflowJob jobObj = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    jobObj.addProperty(new ParametersDefinitionProperty(paramDef));
    
    String incorrectJob = "incorrect_job";
    String pipelineScript
            = "node {\n"
            + "  setParameterValue(\n"
            +    "'_class' : '" + _class + "',\n"
            +    "'name' : 'Foo',\n"
            +    "'value' : 'Foo4',\n"
            +    "'job' : '" + incorrectJob + "',\n"
            +    "'run' : '" + run + "'\n"
            +    ")\n"
            + "}";
    jobObj.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun failedBuild = jenkins.assertBuildStatus(Result.FAILURE, jobObj.scheduleBuild2(0));
    LOGGER.info("Run log: " + JenkinsRule.getLog(failedBuild));
    String expectedString = "ERROR: Specified job '" + incorrectJob + "' was not found!";
    jenkins.assertLogContains(expectedString, failedBuild);
  }

  @Test
  public void testScriptedPipelineMultilineIncorrectRun() throws Exception {
    String agentLabel = "my-agent";
    jenkins.createOnlineSlave(Label.get(agentLabel));
    WorkflowJob jobObj = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    jobObj.addProperty(new ParametersDefinitionProperty(paramDef));
    
    String incorrectRun = "incorrect_run";
    String pipelineScript
            = "node {\n"
            + "  setParameterValue(\n"
            +    "'_class' : '" + _class + "',\n"
            +    "'name' : 'Foo',\n"
            +    "'value' : 'Foo4',\n"
            +    "'job' : '" + job + "',\n"
            +    "'run' : '" + incorrectRun + "'\n"
            +    ")\n"
            + "}";
    jobObj.setDefinition(new CpsFlowDefinition(pipelineScript, true));
    WorkflowRun failedBuild = jenkins.assertBuildStatus(Result.FAILURE, jobObj.scheduleBuild2(0));
    LOGGER.info("Run log: " + JenkinsRule.getLog(failedBuild));
    String expectedString = "ERROR: Specified job's run '" + incorrectRun + "' was not found!";
    jenkins.assertLogContains(expectedString, failedBuild);
  }

  private NameValuePair getCrumbHeaderNvp() {
    return new NameValuePair(jenkins.jenkins.getCrumbIssuer().getDescriptor().getCrumbRequestField(),
                    jenkins.jenkins.getCrumbIssuer().getCrumb(null));
  }

}