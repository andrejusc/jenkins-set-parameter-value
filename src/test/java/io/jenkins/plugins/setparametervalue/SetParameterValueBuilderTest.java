package io.jenkins.plugins.setparametervalue;

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
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.common.base.Charsets;

import hudson.model.Action;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
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

  @SuppressWarnings({"checkstyle:javadocmethod"})
  //    @Test
  public void testBuild() throws Exception {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    SetParameterValueBuilder builder = new SetParameterValueBuilder(null, name, value, job, run);
    project.getBuildersList().add(builder);
    LOGGER.info("Project project.getBuildersList(): " + project.getBuildersList());
    
    ParameterDefinition paramDef = new StringParameterDefinition("Foo", "Foo");
    project.addProperty(new ParametersDefinitionProperty(paramDef));
    
    FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
    jenkins.assertLogContains("SetParameterValue with parameter: " + name, build);
    
    LOGGER.info("Project name: " + project.getName() + ", build: " + build.getNumber());
    LOGGER.info("Jenkins URL: " + jenkins.getURL());
    String buildUrl = "job/" + project.getName() + "/" + build.getNumber() + "/api/json?pretty=true";
    String configUrl = "job/" + project.getName() + "/config.xml";
    LOGGER.info("buildURL: " + buildUrl);
    LOGGER.info("JSON: " + jenkins.getJSON(buildUrl).getContentAsString());
    LOGGER.info("configURL: " + configUrl);
    JenkinsRule.WebClient webClient = jenkins.createWebClient();
    Page runsPage = null;
    try {
      runsPage = webClient.goTo(configUrl, "application/xml");
    } catch (SAXException e) {
      // goTo shouldn't be throwing a SAXException for JSON.
      throw new IllegalStateException("Unexpected SAXException.", e);
    }
    WebResponse webResponse = runsPage.getWebResponse();
    LOGGER.info("Config: " + webResponse.getContentAsString());

    ParameterValue pv2 = new StringParameterValue("Foo", "Foo2");
    Action actionParams2 = new ParametersAction(pv2);
    build.addOrReplaceAction(actionParams2);
    build.save();
    LOGGER.info("JSON2: " + jenkins.getJSON(buildUrl).getContentAsString());
    
    //        String paramsUrl = "job/" + project.getName() + "/" + build.getNumber() + "/setParameter";
    String paramsUrl = "plugin/set-parameter-value/api/json";
    try {
      // default content type is "text/html"
      runsPage = webClient.goTo(paramsUrl, "application/json");
    } catch (SAXException e) {
      // goTo shouldn't be throwing a SAXException for JSON.
      throw new IllegalStateException("Unexpected SAXException.", e);
    }
    
    webResponse = runsPage.getWebResponse();
    LOGGER.info("Response: " + webResponse.getContentAsString());

    String paramsUrl2 = "plugin/set-parameter-value/setValue";
    //        webResponse = jenkins.postJSON(paramsUrl2, 
    //            "{\"parameter\":[{\"_class\" : \"hudson.model.StringParameterValue\",
    //            + \"name\":\"Foo\", \"value\":\"Foo3\"}, {\"name\":\"Foo2\", \"value\":\"Foo22\"}], "
    //            + "\"job\":\"" + project.getName() + "\", " 
    //            + "\"run\":\"" + build.getNumber() + "\"}");
    //        LOGGER.info("Response2: " + webResponse.getContentAsString() + ", status: "
    //            + webResponse.getStatusCode());
    //        LOGGER.info("JSON3: " + jenkins.getJSON(buildURL).getContentAsString());
    
    String payload = 
        "{\"parameter\":[{\"_class\" : \"hudson.model.StringParameterValue\", "
        + "\"name\":\"Foo\", \"value\":\"Foo3\"}, {\"name\":\"Foo2\", \"value\":\"Foo22\"}], "
        + "\"job\":\"" + project.getName() + "\", " 
        + "\"run\":\"" + build.getNumber() + "\"}";

    HttpPost httpPost = new HttpPost(jenkins.getURL().toExternalForm() + paramsUrl2);

    StringEntity entity = new StringEntity(payload);
    httpPost.setEntity(entity);
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");

    NameValuePair crumb = getCrumbHeaderNvp();
    httpPost.setHeader(crumb.getName(), crumb.getValue());
    LOGGER.info("crumb.getName(): " + crumb.getName() + ", crumb.getValue(): " + crumb.getValue());

    CloseableHttpClient client = HttpClients.createDefault();
    CloseableHttpResponse response = client.execute(httpPost);
    //assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
    int statusCode = response.getStatusLine().getStatusCode();
    LOGGER.info("Response2: " + EntityUtils.toString(response.getEntity(), Charsets.UTF_8)
        + ", status: " + statusCode);
    LOGGER.info("JSON3: " + jenkins.getJSON(buildUrl).getContentAsString());
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