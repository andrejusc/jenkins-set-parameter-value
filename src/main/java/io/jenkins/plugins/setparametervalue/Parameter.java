package io.jenkins.plugins.setparametervalue;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Parameter code to reflect possible cases of value setting when received as JSON.
 * 
 * @author Andrejus Chaliapinas
 */
public class Parameter {
  @SuppressWarnings({"checkstyle:membername"})
  String _class;
  String name;
  String value;

  /**
   * Default ctor.
   * @param _class Class of supplied parameter.
   * @param name Parameter name.
   * @param value Parameter value.
   */
  @SuppressWarnings({"checkstyle:parametername"})
  @DataBoundConstructor
  public Parameter(String _class, String name, String value) {
    System.out.println("--- Parameter ctor name: " + name);
    this._class = _class;
    this.name = name;
    this.value = value;
  }

  public String get_class() {
    return _class;
  }

  @SuppressWarnings({"checkstyle:parametername"})
  public void set_class(String _class) {
    this._class = _class;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
