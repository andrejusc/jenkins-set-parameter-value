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
   * Invokes set parameter POST call.
   * @param req Request.
   * @param rsp Response.
   * @throws IOException Possible excepttion1.
   * @throws ServletException Possible excepttion2.
   */
  @RequirePOST
  public void doSetParameter(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
    LOGGER.info("--- Set parameter TestPlugin req: " + req);
    LOGGER.info("--- Set parameter TestPlugin contentType: " + req.getContentType());
    final Jenkins jenkins;
    try {
      jenkins = Jenkins.get();
      LOGGER.info("--- Set parameter Jenkins: " + jenkins);
      Job job1 = (Job) jenkins.getItemByFullName("test0");
      LOGGER.info("--- Set parameter TestPlugin job: " + job1);
      String reqStr = httpServletRequestToString(req);
      LOGGER.info("--- Set parameter TestPlugin reqStr: " + reqStr);
      //      JSONTokener tokener = new JSONTokener(reqStr);
      //      JSONObject json = req.getSubmittedForm();
      JSONObject json = JSONObject.fromObject(reqStr);
      LOGGER.info("--- Set parameter TestPlugin json from reqStr: " + json);
      //      JSONArray ar = json.getJSONArray("parameter");
      //      LOGGER.info("--- Set parameter TestPlugin ar: " + ar);
      //      Parameter p = req.bindJSON(Parameter.class, json);
      List<Parameter> l = req.bindJSONToList(Parameter.class, json.getJSONArray("parameter"));
      LOGGER.info("--- Set parameter TestPlugin parameter: " + l);
      String jobStr = json.getString("job");
      Job job = (Job) jenkins.getItemByFullName(jobStr);
      if (job == null) {
        LOGGER.info("--- Set parameter TestPlugin job not retrieved!");
        rsp.setStatus(StaplerResponse.SC_NOT_FOUND);
        //return HttpResponses.errorJSON("{}");
        return;
      }
      LOGGER.info("--- Set parameter TestPlugin job: " + job);
      String runStr = json.getString("run");
      Run run = (Run) job.getBuild(runStr);
      if (run == null) {
        LOGGER.info("--- Set parameter TestPlugin run not retrieved!");
        rsp.setStatus(StaplerResponse.SC_NOT_FOUND);
        //return HttpResponses.errorJSON("{}");
        return;
      }
      LOGGER.info("--- Set parameter TestPlugin run: " + run);

      for (Parameter p : l) {
        LOGGER.info("--- Set parameter TestPlugin p name: " + p.getName() + ", class: " + p.get_class());
      }

      for (Parameter p : l) {
        if ("Foo".contentEquals(p.getName())) {
          ParameterValue pv = new StringParameterValue(p.getName(), p.getValue());
          //          ParameterValue pv = new TextParameterValue(p.getName(), p.getValue());
          Action actionParams = new ParametersAction(pv);
          run.addOrReplaceAction(actionParams);
          run.save();
          break;
        }
      }
      // test
      //String payload = "{}";
      //rsp.setHeader("Content-Type", "application/json");
      //rsp.setStatus(StaplerResponse.SC_NOT_FOUND);
      //rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      // rsp.getOutputStream().write(payload.getBytes());
      // rsp.sendError(StaplerResponse.SC_NOT_FOUND);
      //rsp.getWriter().write(payload);

      //      rsp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      //      rsp.addHeader("Allow", "POST");
      //      rsp.setContentType("text/html");
      //      PrintWriter w = rsp.getWriter();
      //      w.println("<html><head><title>POST required</title></head><body>");
      //      w.println("POST is required for ...<br>");
      //      w.println("<form method='POST'><input type='submit' value='Try POSTing'></form>");
      //      w.println("</body></html>");

      rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      HttpResponses.errorJSON("some error here").generateResponse(req, rsp, null);
    } catch (IllegalStateException e) {
      LOGGER.info("--- Set parameter exception!");
      e.printStackTrace();
    }
    //return HttpResponses.errorJSON("some error here");
    //return HttpResponses.errorWithoutStack(400, "some error");
    //return HttpResponses.error(400, "abc");
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
