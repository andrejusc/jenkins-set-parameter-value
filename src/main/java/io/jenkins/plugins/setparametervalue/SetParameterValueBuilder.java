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
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

/**
 * Builder to expose functionality into pipeline scripting.
 * 
 * @author Andrejus Chaliapinas
 *
 */
public class SetParameterValueBuilder extends Builder implements SimpleBuildStep {

  @SuppressWarnings({"checkstyle:membername"})
  private final String _class;
  private final String name;
  private final String value;
  private final String job;
  private final String run;

  /**
   * Default ctor.
   * @param _class Class name.
   * @param name Parameter name.
   * @param value Parameter value.
   * @param job Job.
   * @param run Run.
   */
  @SuppressWarnings({"checkstyle:parametername"})
  @DataBoundConstructor
  public SetParameterValueBuilder(String _class, String name, String value, String job, String run) {
    this._class = _class;
    this.name = name;
    this.value = value;
    this.job = job;
    this.run = run;
  }

  public String get_class() {
    return _class;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String getJob() {
    return job;
  }

  public String getRun() {
    return run;
  }

  @Override
  public void perform(Run<?, ?> performrun, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {
    listener.getLogger().println("SetParameterValue with parameter: " + name + ", job: " + job
        + ", and job's run: " + run);
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
    ParameterValue pv = new StringParameterValue(name, value);
    Action actionParams = new ParametersAction(pv);
    runObj.addOrReplaceAction(actionParams);
    runObj.save();
    
    List<ParametersAction> l = runObj.getActions(ParametersAction.class);
    for (ParametersAction p : l) {
      listener.getLogger().println("p: " + p.getParameter(name));
    }
  }

  @Symbol("setParameterValue")
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
