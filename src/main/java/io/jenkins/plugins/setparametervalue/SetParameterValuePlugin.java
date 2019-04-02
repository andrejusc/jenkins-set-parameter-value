package io.jenkins.plugins.setparametervalue;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;

import hudson.Extension;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Api;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Core plugin code to set parameter value.
 * 
 * @author Andrejus Chaliapinas
 */
@Extension
@Singleton
@ExportedBean
public class SetParameterValuePlugin extends Plugin {

  private static final Logger LOGGER = Logger.getLogger(SetParameterValuePlugin.class.getName());

  public Api getApi() {
    return new Api(this);
  }

  @Override
  public void postInitialize() throws Exception {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("--- postInitialize");
    }
    super.postInitialize();
  }

  @Override
  protected XmlFile getConfigXml() {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("--- getConfigXml");
    }
    return super.getConfigXml();
  }

  @Override
  public void setServletContext(ServletContext context) {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("--- setServletContext: " + context);
    }
    super.setServletContext(context);
  }

  @Override
  public PluginWrapper getWrapper() {
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("--- getWrapper");
    }
    return super.getWrapper();
  }

  @Exported
  public String getInformation() {
    return "some string";
  }

  /**
   * Invokes set parameter value POST call.
   * @param req Request.
   * @param rsp Response.
   * @throws IOException Possible excepttion1.
   * @throws ServletException Possible excepttion2.
   */
  @RequirePOST
  public void doSetParameterValue(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    final Jenkins jenkins = Jenkins.get();
    // Protect from anonymous call 
    jenkins.checkPermission(Run.UPDATE);
    try {
      String reqStr = httpServletRequestToString(req);
      JSONObject json = JSONObject.fromObject(reqStr);
      String jobStr = json.getString("job");
      String runStr = json.getString("run");
      LOGGER.info("SetParameterValue for job: " + jobStr
          + ", and job's run: " + runStr);
      Job job = (Job) jenkins.getItemByFullName(jobStr);
      if (job == null) {
        rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        HttpResponses.errorJSON(String.format("Specified job '%s' was not found!", jobStr))
          .generateResponse(req, rsp, null);
        return;
      }
      Run run = (Run) job.getBuild(runStr);
      if (run == null) {
        rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        HttpResponses.errorJSON(String.format("Specified job's run '%s' was not found!", runStr))
          .generateResponse(req, rsp, null);
        return;
      }

      ParametersAction pa = run.getAction(ParametersAction.class);
      if (pa == null) {
        rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        HttpResponses.errorJSON(String.format("Specified job '%s' doesn't have parameters defined!", jobStr))
          .generateResponse(req, rsp, null);
        return;
      }
      List<ParameterValue> pvs = pa.getAllParameters();
      List<Parameter> l = req.bindJSONToList(Parameter.class, json.getJSONArray("parameter"));

      // Compare provided against defined
      for (Parameter paramProvided : l) {
        boolean found = false;
        for (ParameterValue paramDefined : pvs) {
          if (paramProvided.getName().equals(paramDefined.getName())) {
            found = true;
            break;
          }
        }
        if (!found) {
          rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          HttpResponses.errorJSON(String.format("Provided parameter '%s' isn't defined for job '%s'!",
            paramProvided.getName(), jobStr))
            .generateResponse(req, rsp, null);
          return;
        }
      }

      for (Parameter p : l) {
        ParameterValue pv = new StringParameterValue(p.getName(), p.getValue());
        //          ParameterValue pv = new TextParameterValue(p.getName(), p.getValue());
        Action actionParams = new ParametersAction(pv);
        run.addOrReplaceAction(actionParams);
        run.save();
      }

      rsp.setStatus(HttpServletResponse.SC_OK);
      HttpResponses.okJSON().generateResponse(req, rsp, null);
    } catch (IllegalStateException e) {
      LOGGER.log(Level.SEVERE, "Set parameter value exception!", e);
    }
  }

  private String httpServletRequestToString(StaplerRequest request) throws IOException {

    ServletInputStream mServletInputStream = request.getInputStream();
    byte[] httpInData = new byte[request.getContentLength()];
    int retVal = -1;
    StringBuilder stringBuilder = new StringBuilder();

    while ((retVal = mServletInputStream.read(httpInData)) != -1) {
      for (int i = 0; i < retVal; i++) {
        stringBuilder.append(Character.toString((char) httpInData[i]));
      }
    }

    return stringBuilder.toString();
  }

}
