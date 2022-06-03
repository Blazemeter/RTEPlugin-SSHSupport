package com.blazemeter.jmeter.rte.sampler.gui;

import com.blazemeter.jmeter.rte.SwingTestRunner;
import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.assertj.swing.timing.Pause.pause;

import com.blazemeter.jmeter.rte.JMeterTestUtils;
import com.blazemeter.jmeter.rte.core.Protocol;
import com.blazemeter.jmeter.rte.core.TerminalType;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;

import com.blazemeter.jmeter.rte.core.ssh.SSHCredentialType;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.timing.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SwingTestRunner.class)
public class RTEConfigPanelIT {

  private FrameFixture frame;

  @BeforeClass
  public static void setUpOnce() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Before
  public void setup() {
    RTEConfigPanel panel = new RTEConfigPanel();
    frame = showInFrame(panel);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldChangeTheValuesOfTheTerminalComboBoxWhenChangeProtocolComboBox() {
    JComboBox<TerminalType> terminalCombo = frame.comboBox("terminalTypeComboBox").target();
    selectProtocol(Protocol.TN3270.name());
    pause(new Condition("TN3270 terminal type listed in terminal combo") {
      @Override
      public boolean test() {
        return Protocol.TN3270.createProtocolClient().getSupportedTerminalTypes()
            .containsAll(getComboValues(terminalCombo));
      }
    });
  }

  private <T> List<T> getComboValues(JComboBox<T> combo) {
    List<T> ret = new ArrayList<>();
    for (int i = 0; i < combo.getItemCount(); i++) {
      ret.add(combo.getItemAt(i));
    }
    return ret;
  }

  @Test
  public void shouldDisplaySshFieldsWhenVT420Selected() {
    selectVT420();
    pause(new Condition("Use SSH checkbox is visible") {
      @Override
      public boolean test() {
        return frame.checkBox("useSsh").target().isVisible();
      }
    });
  }

  private void selectVT420() {
    selectProtocol(Protocol.VT420.name());
  }

  private void selectProtocol(String protocol) {
    JComboBoxFixture protocolComboBox = frame.comboBox("protocolComboBox");
    protocolComboBox.selectItem(protocol);
  }

  @Test
  public void shouldDisplaySshConfigurationFieldWhenUseSshChecked() {
    selectVT420();
    checkUseSsh();

    pause(new Condition("Use SSH checkbox is visible") {
      @Override
      public boolean test() {
        return getSshCredentialTypeCombo().isVisible();
      }
    });
  }

  private void checkUseSsh() {
    frame.checkBox("useSsh").check();
  }

  private JComboBox<SSHCredentialType> getSshCredentialTypeCombo() {
    return frame.comboBox("sshCredentialType").target();
  }
}
