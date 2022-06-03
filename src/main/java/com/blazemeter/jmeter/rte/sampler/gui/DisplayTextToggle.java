package com.blazemeter.jmeter.rte.sampler.gui;

import com.blazemeter.jmeter.commons.PlaceHolderPassword;
import com.blazemeter.jmeter.commons.ThemedIcon;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JToggleButton;

//TODO: After functional tests, move this class to bzm-jmeter-commons
public class DisplayTextToggle extends JToggleButton {

  private final Icon displayedCredentialsIcon =
      ThemedIcon.fromResourceName("visible-credentials.png");
  private final Icon hiddenCredentialsIcon =
      ThemedIcon.fromResourceName("not-visible-credentials.png");

  private final PlaceHolderPassword field;

  public DisplayTextToggle(String name, PlaceHolderPassword field) {
    this.field = field;
    init(name);
  }

  private void init(String name) {
    setName(name);
    setBorderPainted(false);
    setContentAreaFilled(false);
    setIcon(displayedCredentialsIcon);
    setRequestFocusEnabled(false);
    setMargin(new Insets(getMargin().top, 0, getMargin().bottom, 0));

    addItemListener(e -> {
      if (isSelected()) {
        field.setEchoChar((char) 0);
        setIcon(hiddenCredentialsIcon);
      } else {
        field.setEchoChar('*');
        setIcon(displayedCredentialsIcon);
      }
    });
  }
}
