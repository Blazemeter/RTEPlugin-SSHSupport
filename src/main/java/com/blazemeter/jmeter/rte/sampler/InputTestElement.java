package com.blazemeter.jmeter.rte.sampler;

import com.blazemeter.jmeter.rte.core.Input;
import com.blazemeter.jmeter.rte.recorder.emulator.CommandUtils;
import java.io.Serializable;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.property.StringProperty;

public abstract class InputTestElement extends AbstractTestElement implements Serializable {

  private static final String INPUT = "Input.input";

  public InputTestElement() {
  }

  public InputTestElement(String input) {
    setInput(input);
  }

  public String getInput() {
    /*
    We keep support for old property name to be backwards compatible with .jmx of previous plugin
    versions
     */
    String val = getPropertyAsString(INPUT, "");
    // If the input is a control character, we parse the string representation into a character
    if (!val.isEmpty() && CommandUtils.isControlUnicode(val)) {
      val = CommandUtils.getCharFromUnicodeString(val) + "";
      System.out.println(">>>> ITS CONTROL CODE=" + CommandUtils.getUnicodeString(val));
    }

    System.out.println(">>> InputTestElement.getInput() val = " + val);
    return val == null ? getPropertyAsString("CoordInputRowGUI.input") : val;
  }

  public void setInput(String input) {
    setProperty(new StringProperty(INPUT, input));
  }

  public abstract Input toInput();

  public abstract void copyOf(InputTestElement cellValue);

}
