package io.jenkins.plugins.setparametervalue;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

/**
 * Builder to expose get parameter value functionality into pipeline scripting.
 * 
 * @author Andrejus Chaliapinas
 *
 */
public class GetParameterValueBuilder extends Builder implements SimpleBuildStep {

  private final String name;
  private final String job;
  private final String run;
  private final Object list;

  /**
   * Default ctor.
   * @param name Parameter name.
   * @param job Job.
   * @param run Run.
   */
  @DataBoundConstructor
  public GetParameterValueBuilder(String name, String job, String run, Object list) {
    this.name = name;
    this.job = job;
    this.run = run;
    this.list = list;
  }

  public String getName() {
    return name;
  }

  public String getJob() {
    return job;
  }

  public String getRun() {
    return run;
  }

  public Object getList() {
    System.out.println("getList: " + list);
    return list;
  }

  @Override
  public void perform(Run<?, ?> performrun, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {
    listener.getLogger().println("GetParameterValue with parameter: " + name + ", job: " + job
        + ", and job's run: " + run);
    listener.getLogger().println("list: " + list + (list != null ? list.getClass() : "null"));
    listener.getLogger().println("performrun: " + performrun);

    Job<?, ?> jobObj = (Job<?, ?>) Jenkins.get().getItemByFullName(job);
    if (jobObj == null) {
      listener.getLogger().println(String.format("ERROR: Specified job '%s' was not found!", job));
      performrun.setResult(Result.FAILURE);
      return;
    }
    listener.getLogger().println("jobObj: " + jobObj);
    Run<?, ?> runObj = (Run<?, ?>) jobObj.getBuild(run);
    if (runObj == null) {
      listener.getLogger().println(String.format("ERROR: Specified job's run '%s' was not found!", run));
      performrun.setResult(Result.FAILURE);
      return;
    }
    listener.getLogger().println("runObj: " + runObj);
    
    if (list == null) {
      listener.getLogger().println("ERROR: Specified list to return value to was null!");
      performrun.setResult(Result.FAILURE);
      return;
    }
    List<ParametersAction> l = runObj.getActions(ParametersAction.class);
    boolean found = false;
    for (ParametersAction pa : l) {
      ParameterValue pv = pa.getParameter(name);
      //listener.getLogger().println("name: " + name + ", pv.getName(): " + pv.getName());
      if (name.equals(pv.getName())) {
        //listener.getLogger().println("list adding value: " + pv.getValue().toString());
        ((List) list).add(pv.getValue().toString());
        //listener.getLogger().println("list size: " + ((List) list).size());
        found = true;
        break;
      }
    }
    if (!found) {
      listener.getLogger().println(String.format("ERROR: Specified parameter '%s' was not found!", name));
      performrun.setResult(Result.FAILURE);
      return;
    }
  }

  @Symbol("getParameterValue")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    /**
     * Checks job name.
     * @param value Job name to use.
     * @return Result as FormValidation.
     * @throws IOException Possible exception1.
     * @throws ServletException Possible exception2.
     */
    public FormValidation doCheckJob(@QueryParameter String value)
        throws IOException, ServletException {
      if (value.length() == 0) {
        return FormValidation.error(Messages.SetParameterValueBuilder_DescriptorImpl_errors_missingJobName());
      }
      return FormValidation.ok();
    }

    /**
     * Checks parameter name.
     * @param value Parameter name to use.
     * @return Result as FormValidation.
     * @throws IOException Possible exception1.
     * @throws ServletException Possible exception2.
     */
    public FormValidation doCheckName(@QueryParameter String value)
        throws IOException, ServletException {
      if (value.length() == 0) {
        return FormValidation.error(Messages.SetParameterValueBuilder_DescriptorImpl_errors_missingParameterName());
      }
      return FormValidation.ok();
    }

    /**
     * Checks job run identifier to be numeric.
     * @param value Run identifier to use.
     * @return Result as FormValidation.
     * @throws IOException Possible exception1.
     * @throws ServletException Possible exception2.
     */
    public FormValidation doCheckRun(@QueryParameter String value)
        throws IOException, ServletException {
      if (value.length() == 0) {
        return FormValidation.error(Messages.SetParameterValueBuilder_DescriptorImpl_errors_missingRunID());
      }
      try {
        Integer.parseInt(value);
      } catch (NumberFormatException nfe) {
        return FormValidation.error(Messages.SetParameterValueBuilder_DescriptorImpl_errors_nonNumericRunID());
      }
      return FormValidation.ok();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return Messages.SetParameterValueBuilder_DescriptorImpl_DisplayName();
    }
  }
}
