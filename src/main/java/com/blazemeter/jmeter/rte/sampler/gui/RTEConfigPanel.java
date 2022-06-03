package com.blazemeter.jmeter.rte.sampler.gui;

import com.blazemeter.jmeter.commons.PlaceHolderPassword;
import com.blazemeter.jmeter.commons.PlaceHolderTextField;
import com.blazemeter.jmeter.commons.SwingBuilders;
import com.blazemeter.jmeter.rte.core.Protocol;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.blazemeter.jmeter.rte.core.ssh.SSHCredentialType;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.sampler.RTESampler;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import org.apache.jmeter.util.JMeterUtils;

public class RTEConfigPanel extends JPanel {

  private static final long serialVersionUID = -3671411083800369578L;

  private static final DefaultComboBoxModel<TerminalType> TN5250_TERMINAL_TYPES =
      buildTerminalTypesComboBoxModel(Protocol.TN5250);
  private static final DefaultComboBoxModel<TerminalType> TN3270_TERMINAL_TYPES =
      buildTerminalTypesComboBoxModel(Protocol.TN3270);
  private static final DefaultComboBoxModel<TerminalType> VT420_TERMINAL_TYPES = 
      buildTerminalTypesComboBoxModel(Protocol.VT420);
  private final ButtonGroup sslTypeGroup = new ButtonGroup();
  private final Map<SSLType, JRadioButton> sslTypeRadios = new EnumMap<>(SSLType.class);
  private final SwingBuilders.ComponentBuilder textFieldBuilder
      = new SwingBuilders.ComponentBuilder();
  private final JTextField serverField = textFieldBuilder.withName("serverField")
      .buildJTextField();
  private final JTextField portField = textFieldBuilder.withName("portField")
      .buildJTextField();
  private final JTextField connectionTimeout = textFieldBuilder.withName("connectionTimeout")
      .buildJTextField();
  private final JComboBox<TerminalType> terminalTypeComboBox = SwingUtils
      .createComponent("terminalTypeComboBox", new JComboBox<>(TN5250_TERMINAL_TYPES));
  private final JComboBox<SSHCredentialType> sshCredentialType = SwingUtils
          .createComponent("sshCredentialType", new JComboBox<>());
  private final JCheckBox useSsh = SwingUtils.createComponent("useSsh",
      new JCheckBox("Use SSH", false));
  private JComboBox<Protocol> protocolComboBox;
  private PlaceHolderTextField usernameInput;
  private PlaceHolderPassword passwordInput;
  private JPanel sshPanel;

  public RTEConfigPanel() {
    GroupLayout layout = new GroupLayout(this);
    layout.setAutoCreateGaps(true);
    this.setLayout(layout);

    JPanel connectionPanel = buildConnectionPanel();

    layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
        .addComponent(connectionPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
            Short.MAX_VALUE));
    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(connectionPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(ComponentPlacement.RELATED));
  }

  private JPanel buildConnectionPanel() {
    JPanel panel = new SwingBuilders.ComponentBuilder().withName("connectionPanel")
        .withTitle("Connection")
        .buildPanel();

    GroupLayout layout = new GroupLayout(panel);
    layout.setAutoCreateContainerGaps(true);
    panel.setLayout(layout);
    sshPanel = buildSshPanel();
    JLabel serverLabel = SwingUtils.createComponent("serverLabel", new JLabel("Server: "));
    JLabel portLabel = SwingUtils.createComponent("portLabel", new JLabel("Port: "));
    JLabel protocolLabel = SwingUtils.createComponent("protocolLabel", new JLabel("Protocol: "));
    protocolComboBox = buildProtocolComboBox();
    JLabel terminalTypeLabel = SwingUtils
        .createComponent("terminalTypeLabel", new JLabel("Terminal Type:"));
    JPanel sslPanel = buildSslPanel();
    JPanel timeOutPanel = buildTimeoutPanel();
    layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addComponent(serverLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(serverField, GroupLayout.PREFERRED_SIZE, 500, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.UNRELATED)
            .addComponent(portLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(portField, GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.UNRELATED))
        .addGroup(layout.createSequentialGroup()
            .addComponent(protocolLabel)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(protocolComboBox, 0, 1, Short.MAX_VALUE)
            .addPreferredGap(ComponentPlacement.UNRELATED))
        .addGroup(
            layout.createSequentialGroup()
                .addComponent(terminalTypeLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(terminalTypeComboBox, 0, 1, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.UNRELATED))
        .addGroup(
                layout.createSequentialGroup()
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(connectionTimeout, GroupLayout.PREFERRED_SIZE, 150,
                GroupLayout.PREFERRED_SIZE))
        .addGroup(layout.createSequentialGroup()
                .addComponent(sslPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                .addComponent(sshPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                        GroupLayout.PREFERRED_SIZE)
        )
        .addGroup(
            layout.createSequentialGroup()
            .addComponent(timeOutPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                          GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(ComponentPlacement.RELATED)));
    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(serverLabel)
            .addComponent(serverField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE)
            .addComponent(portLabel)
            .addComponent(portField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(protocolLabel)
            .addComponent(protocolComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(terminalTypeLabel)
            .addComponent(terminalTypeComboBox, GroupLayout.PREFERRED_SIZE,
                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
            .addComponent(connectionTimeout, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                    GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup()
                            .addComponent(sslPanel)
                            .addComponent(sshPanel)
                    )
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                  .addComponent(timeOutPanel)
            ));

    layout.linkSize(SwingConstants.VERTICAL, sslPanel, timeOutPanel);
    layout.linkSize(SwingConstants.HORIZONTAL, sslPanel, timeOutPanel);

    return panel;
  }

  private JComboBox<Protocol> buildProtocolComboBox() {
    JComboBox<Protocol> comboBox = SwingUtils
        .createComponent("protocolComboBox", new JComboBox<>(Protocol.values()));
    comboBox.addItemListener(e -> {
      if (e.getStateChange() != ItemEvent.SELECTED) {
        return;
      }
      sshPanel.setVisible(false);
      Protocol protocolEnum = (Protocol) e.getItem();
      if (protocolEnum.equals(Protocol.TN5250)) {
        terminalTypeComboBox.setModel(TN5250_TERMINAL_TYPES);
      } else if (protocolEnum.equals(Protocol.TN3270)) {
        terminalTypeComboBox.setModel(TN3270_TERMINAL_TYPES);
      } else if (protocolEnum.equals(Protocol.VT420)) {
        terminalTypeComboBox.setModel(VT420_TERMINAL_TYPES);
        sshPanel.setVisible(true);
      }
      validate();
      repaint();
    });
    return comboBox;
  }

  private static DefaultComboBoxModel<TerminalType> buildTerminalTypesComboBoxModel(
      Protocol protocol) {
    return new DefaultComboBoxModel<>(
        protocol.createProtocolClient().getSupportedTerminalTypes()
            .toArray(new TerminalType[0]));
  }

  private JPanel buildSslPanel() {
    JPanel panel = new SwingBuilders.ComponentBuilder().withName("sslPanel")
        .withTitle("SSL Type")
        .isVisible(true)
        .buildPanel();

    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    GroupLayout.SequentialGroup horizontal = layout.createSequentialGroup();
    GroupLayout.ParallelGroup vertical = layout.createParallelGroup(Alignment.BASELINE);
    Arrays.stream(SSLType.values()).forEach(s -> {
      JRadioButton r = SwingUtils.createComponent(s.toString(), new JRadioButton(s.toString()));
      r.setActionCommand(s.name());
      horizontal.addComponent(r);
      vertical.addComponent(r);
      sslTypeRadios.put(s, r);
      sslTypeGroup.add(r);
    });

    layout.setHorizontalGroup(horizontal);
    layout.setVerticalGroup(vertical);

    return panel;
  }

  private JPanel buildSshPanel() {
    SwingBuilders.ComponentBuilder labelBuilder = new SwingBuilders.ComponentBuilder().notVisible();

    JLabel byPropertiesLabel = labelBuilder.withName("byProperties")
        .withText("Setup rte.ssh.username and rte.ssh.password in jmeter.properties.")
        .withItalicFont()
        .buildLabel();

    JLabel sshInfo = labelBuilder.withName("sshInfo")
        .withText("Select what method will be used to provide the SSH credentials")
        .buildLabel();

    JLabel byFieldsLabel = labelBuilder.withName("byFieldsLabel")
        .withText("Setup the credentials in the fields")
        .buildLabel();

    JLabel sshInstructionsLabel = labelBuilder.withName("credInfo")
        .withText("Select how you want to provide the credentials")
        .buildLabel();

    SwingBuilders.ComponentBuilder componentsBuilder = new SwingBuilders.ComponentBuilder()
        .notVisible()
        .withPreferredSize(new Dimension(150, 30));

    DisplayTextToggle displayPassword = (DisplayTextToggle) componentsBuilder
        .forComponent(new DisplayTextToggle("displayPassword", passwordInput))
        .build();

    usernameInput = componentsBuilder.forComponent(new PlaceHolderTextField())
        .withToolTip("Username for the SSH authentication")
        .withName("usernameInput")
        .withPlaceholder("Username")
        .buildPlaceHolderTextField();

    passwordInput = componentsBuilder.forComponent(new PlaceHolderPassword(""))
        .withToolTip("Password for the SSH authentication")
        .withName("passwordInput")
        .withPlaceholder("Password")
        .buildPlaceHolderPassword();

    sshCredentialType.setVisible(false);
    sshCredentialType.addItem(SSHCredentialType.BY_FIELDS);
    sshCredentialType.addItem(SSHCredentialType.BY_PROPERTIES);
    sshCredentialType.addActionListener(e -> {
      sshCredentialType.getModel().getSelectedItem();
      boolean usingFields = isFieldSelected();
      byPropertiesLabel.setVisible(!usingFields);
      byFieldsLabel.setVisible(usingFields);
      usernameInput.setVisible(usingFields);
      passwordInput.setVisible(usingFields);
      displayPassword.setVisible(usingFields);
    });

    useSsh.addItemListener(e -> {
      boolean isChecked = e.getStateChange() == ItemEvent.SELECTED;
      sshInfo.setVisible(isChecked);
      byFieldsLabel.setVisible(isChecked);
      byPropertiesLabel.setVisible(isChecked && !isFieldSelected());
      sshInstructionsLabel.setVisible(isChecked);
      sshCredentialType.setVisible(isChecked);
      usernameInput.setVisible(isChecked);
      passwordInput.setVisible(isChecked);
      displayPassword.setVisible(isChecked);
    });

    JPanel panel = new SwingBuilders.ComponentBuilder().withName("sshCredentials")
        .withTitle("SSH Config")
        .notVisible()
        .buildPanel();

    GroupLayout sshLayout = new GroupLayout(panel);
    panel.setLayout(sshLayout);
    sshLayout.setHorizontalGroup(sshLayout.createSequentialGroup()
            .addComponent(useSsh)
            .addGroup(sshLayout.createParallelGroup((GroupLayout.Alignment.LEADING))
                    .addGroup(sshLayout.createSequentialGroup()
                            .addComponent(sshInstructionsLabel)
                            .addComponent(sshCredentialType)
                    )
                    .addComponent(byPropertiesLabel)
                    .addComponent(usernameInput)
                    .addGroup(sshLayout.createSequentialGroup()
                            .addComponent(passwordInput)
                            .addComponent(displayPassword)
                    )
                    .addComponent(byFieldsLabel)
            )
    );

    sshLayout.setVerticalGroup(sshLayout.createSequentialGroup()
            .addGroup(sshLayout.createParallelGroup((GroupLayout.Alignment.BASELINE))
                    .addComponent(useSsh)
                    .addComponent(sshInstructionsLabel)
                    .addComponent(sshCredentialType)
            )
            .addComponent(byPropertiesLabel)
            .addComponent(usernameInput)
            .addGroup(sshLayout.createParallelGroup()
                    .addComponent(passwordInput)
                    .addComponent(displayPassword)
            )
            .addComponent(byFieldsLabel)
    );

    sshLayout.linkSize(passwordInput, usernameInput);

    return panel;
  }

  private boolean isFieldSelected() {
    return SSHCredentialType.BY_FIELDS.equals(sshCredentialType.getModel().getSelectedItem());
  }

  private JPanel buildTimeoutPanel() {
    JPanel panel = new SwingBuilders.ComponentBuilder().withName("timeoutPanel")
        .withTitle(JMeterUtils.getResString("timeout_title"))
        .buildPanel();

    GroupLayout layout = new GroupLayout(panel);
    layout.setAutoCreateContainerGaps(true);
    panel.setLayout(layout);

    JLabel connectTimeoutLabel = SwingUtils.createComponent("connectTimeoutLabel",
        new JLabel(JMeterUtils.getResString("web_server_timeout_connect")));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addComponent(connectTimeoutLabel)
        .addPreferredGap(ComponentPlacement.RELATED)
        .addComponent(connectionTimeout, GroupLayout.PREFERRED_SIZE, 150,
            GroupLayout.PREFERRED_SIZE));
    layout.setVerticalGroup(layout.createParallelGroup(Alignment.BASELINE)
        .addComponent(connectTimeoutLabel)
        .addComponent(connectionTimeout, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
            GroupLayout.PREFERRED_SIZE));
    return panel;
  }

  public String getServer() {
    return serverField.getText();
  }

  public void setServer(String serverAddressParam) {
    serverField.setText(serverAddressParam);
  }

  public String getPort() {
    return portField.getText();
  }

  public void setPort(String portParam) {
    portField.setText(portParam);
  }

  public SSLType getSSLType() {
    String sslType = sslTypeGroup.getSelection().getActionCommand();
    return SSLType.valueOf(sslType);
  }

  public void setSSLType(SSLType ssl) {
    if (sslTypeRadios.containsKey(ssl)) {
      sslTypeRadios.get(ssl).setSelected(true);
    } else {
      sslTypeRadios.get(RTESampler.DEFAULT_SSL_TYPE).setSelected(true);
    }
  }

  public Protocol getProtocol() {
    return (Protocol) protocolComboBox.getSelectedItem();
  }

  public void setProtocol(Protocol protocol) {
    protocolComboBox.setSelectedItem(protocol);
  }

  public TerminalType getTerminalType() {
    return (TerminalType) terminalTypeComboBox.getSelectedItem();
  }

  public void setTerminalType(TerminalType terminal) {
    terminalTypeComboBox.setSelectedItem(terminal);
  }

  public String getConnectionTimeout() {
    return connectionTimeout.getText();
  }

  public void setConnectionTimeout(String timeout) {
    connectionTimeout.setText(timeout);
  }

  public String getSshCredentialType() {
    return String.valueOf(sshCredentialType.getSelectedItem());
  }

  public void setSshCredentialType(String sshCredentialType) {
    this.sshCredentialType.setSelectedItem(sshCredentialType);
  }

  public String getUsername() {
    return usernameInput.getText();
  }

  public boolean useSsh() {
    return useSsh.isSelected();
  }

  public void setUseSsh(boolean useSsh) {
    this.useSsh.setSelected(useSsh);
  }

  public void setUsername(String username) {
    this.usernameInput.setText(username);
  }

  public String getPassword() {
    return String.valueOf(passwordInput.getPassword());
  }

  public void setPassword(String password) {
    this.passwordInput.setText(password);
  }
}
