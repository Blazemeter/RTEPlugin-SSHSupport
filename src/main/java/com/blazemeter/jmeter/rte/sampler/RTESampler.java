package com.blazemeter.jmeter.rte.sampler;

import com.blazemeter.jmeter.rte.core.AttentionKey;
import com.blazemeter.jmeter.rte.core.CoordInput;
import com.blazemeter.jmeter.rte.core.Input;
import com.blazemeter.jmeter.rte.core.LabelInput;
import com.blazemeter.jmeter.rte.core.NavigationInput;
import com.blazemeter.jmeter.rte.core.Position;
import com.blazemeter.jmeter.rte.core.Protocol;
import com.blazemeter.jmeter.rte.core.RteProtocolClient;
import com.blazemeter.jmeter.rte.core.RteSampleResultBuilder;
import com.blazemeter.jmeter.rte.core.ServerDisconnectHandler;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.blazemeter.jmeter.rte.core.exceptions.ConnectionClosedException;
import com.blazemeter.jmeter.rte.core.exceptions.RteIOException;
import com.blazemeter.jmeter.rte.core.listener.ExceptionHandler;
import com.blazemeter.jmeter.rte.core.listener.RequestListener;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.core.wait.Area;
import com.blazemeter.jmeter.rte.core.wait.CursorWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.DisconnectWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.SilentWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.SyncWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.TextWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.WaitCondition;
import com.helger.commons.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTESampler extends AbstractSampler implements ThreadListener,
    LoopIterationListener {

  public static final String CONFIG_PORT = "RTEConnectionConfig.port";
  public static final String CONFIG_SERVER = "RTEConnectionConfig.server";
  public static final String CONFIG_PROTOCOL = "RTEConnectionConfig.protocol";
  public static final String CONFIG_SSL_TYPE = "RTEConnectionConfig.sslType";
  public static final String CONFIG_CONNECTION_TIMEOUT = "RTEConnectionConfig.connectTimeout";
  public static final String CONFIG_TERMINAL_TYPE = "RTEConnectionConfig.terminalType";
  public static final String CONFIG_USE_SSH = "RTEConnectionConfig.useSsh";
  public static final String CONFIG_SSH_CREDENTIALS_TYPE = "RTEConnectionConfig.sshCredentialType";
  public static final String CONFIG_SSH_USERNAME = "RTEConnectionConfig.sshUsername";
  public static final String CONFIG_SSH_PASSWORD = "RTEConnectionConfig.sshPassword";
  public static final int DEFAULT_PORT = 23;
  public static final long DEFAULT_CONNECTION_TIMEOUT_MILLIS = 60000;
  public static final Action DEFAULT_ACTION = Action.SEND_INPUT;
  public static final AttentionKey DEFAULT_ATTENTION_KEY = AttentionKey.ENTER;
  public static final Protocol DEFAULT_PROTOCOL = Protocol.TN5250;
  public static final TerminalType DEFAULT_TERMINAL_TYPE = DEFAULT_PROTOCOL.createProtocolClient()
      .getDefaultTerminalType();
  public static final SSLType DEFAULT_SSL_TYPE = SSLType.NONE;

  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  public static final String ACTION_PROPERTY = "RTESampler.action";
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  public static final String WAIT_SYNC_TIMEOUT_PROPERTY = "RTESampler.waitSyncTimeout";
  @VisibleForTesting
  protected static final long DEFAULT_STABLE_TIMEOUT_MILLIS = 1000;
  @VisibleForTesting
  protected static final long DEFAULT_WAIT_SYNC_TIMEOUT_MILLIS = 60000;
  @VisibleForTesting
  protected static final long DEFAULT_WAIT_SILENT_TIME_MILLIS = 1000;
  @VisibleForTesting
  protected static final long DEFAULT_WAIT_SILENT_TIMEOUT_MILLIS = 60000;
  @VisibleForTesting
  protected static final long DEFAULT_WAIT_TEXT_TIMEOUT_MILLIS = 30000;
  @VisibleForTesting
  protected static final long DEFAULT_WAIT_CURSOR_TIMEOUT_MILLIS = 30000;

  //If users wants to change Stable Timeout value it should be specified in
  // jmeter.properties by adding a line like ths one:
  // "RTEConnectionConfig.stableTimeoutMillis=value"
  private static final String CONFIG_STABLE_TIMEOUT = "RTEConnectionConfig.stableTimeoutMillis";
  private static final String REUSE_CONNECTIONS_PROPERTY = "RTESampler.reuseConnections";
  private static final String ATTENTION_KEY_PROPERTY = "RTESampler.attentionKey";
  private static final String WAIT_SYNC_PROPERTY = "RTESampler.waitSync";
  private static final String WAIT_CURSOR_PROPERTY = "RTESampler.waitCursor";
  private static final String WAIT_CURSOR_ROW_PROPERTY = "RTESampler.waitCursorRow";
  private static final String WAIT_CURSOR_COLUMN_PROPERTY = "RTESampler.waitCursorColumn";
  private static final String WAIT_CURSOR_TIMEOUT_PROPERTY = "RTESampler.waitCursorTimeout";
  private static final String WAIT_SILENT_PROPERTY = "RTESampler.waitSilent";
  private static final String WAIT_SILENT_TIME_PROPERTY = "RTESampler.waitSilentTime";
  private static final String WAIT_SILENT_TIMEOUT_PROPERTY = "RTESampler.waitSilentTimeout";
  private static final String WAIT_TEXT_PROPERTY = "RTESampler.waitText";
  private static final String WAIT_TEXT_REGEX_PROPERTY = "RTESampler.waitTextRegex";
  private static final String WAIT_TEXT_AREA_TOP_PROPERTY = "RTESampler.waitTextAreaTop";
  private static final String WAIT_TEXT_AREA_LEFT_PROPERTY = "RTESampler.waitTextAreaLeft";
  private static final String WAIT_TEXT_AREA_BOTTOM_PROPERTY = "RTESampler.waitTextAreaBottom";
  private static final String WAIT_TEXT_AREA_RIGHT_PROPERTY = "RTESampler.waitTextAreaRight";
  private static final String WAIT_TEXT_TIMEOUT_PROPERTY = "RTESampler.waitTextTimeout";
  private static final String CONFIG_CHARACTER_TIMEOUT = "RTEConnectionConfig"
      + ".characterTimeoutMillis";
  private static final String WAIT_DISCONNECT_PROPERTY = "RTESampler.waitDisconnect";
  private static final String WAIT_DISCONNECT_TIMEOUT_PROPERTY = "RTESampler.waitDisconnectTimeout";

  private static final Logger LOG = LoggerFactory.getLogger(RTESampler.class);
  private static final long DEFAULT_CHARACTER_TIMEOUT_MILLIS = 60000;
  private static final long DEFAULT_DISCONNECT_TIMEOUT_MILLIS = 10000;
  private static final ThreadLocal<Map<String, RteProtocolClient>> CONNECTIONS = ThreadLocal
      .withInitial(HashMap::new);

  private final transient Function<Protocol, RteProtocolClient> protocolFactory;

  public RTESampler() {
    this(Protocol::createProtocolClient);
  }

  public RTESampler(Function<Protocol, RteProtocolClient> protocolFactory) {
    setName("RTE");
    this.protocolFactory = protocolFactory;
  }

  public static long getStableTimeout() {
    return JMeterUtils.getPropDefault(CONFIG_STABLE_TIMEOUT, DEFAULT_STABLE_TIMEOUT_MILLIS);
  }

  @VisibleForTesting
  protected void setStableTimeout(Long timeoutMillis) {
    if (timeoutMillis == null) {
      JMeterUtils.getJMeterProperties().remove(CONFIG_STABLE_TIMEOUT);
    } else {
      JMeterUtils.setProperty(CONFIG_STABLE_TIMEOUT, String.valueOf(timeoutMillis));
    }
  }

  @Override
  public String getName() {
    return getPropertyAsString(TestElement.NAME);
  }

  @Override
  public void setName(String name) {
    if (name != null) {
      setProperty(TestElement.NAME, name);
    }
  }

  private Protocol getProtocol() {
    return Protocol.valueOf(getPropertyAsString(CONFIG_PROTOCOL));
  }

  private String getServer() {
    return getPropertyAsString(CONFIG_SERVER);
  }

  private int getPort() {
    return getIntProperty(CONFIG_PORT, DEFAULT_PORT);
  }

  /*
  JMeter properties method which receive a default (e.g: getPropertyAsInt(String, int)) apply such
  default only when the property is null, but if the property is invalid or has been specified to
  an empty string for example it returns 0. Since this is not the expected behavior for property
  defaults (expected that if the value is not valid in any way then use default one) we use this
  method and getLongProperty.
   */
  private int getIntProperty(String propertyName, int defaultValue) {
    int prop = getPropertyAsInt(propertyName);
    return prop == 0 ? defaultValue : prop;
  }

  private TerminalType getTerminalType() {
    return getProtocol().createProtocolClient()
        .getTerminalTypeById(getPropertyAsString(CONFIG_TERMINAL_TYPE));
  }

  private long getConnectionTimeout() {
    return getLongProperty(CONFIG_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_MILLIS);
  }

  /*
  check at getIntProperty for an explanation why we use this instead of
  getPropertyAsLong(String, long)
   */
  private long getLongProperty(String propertyName, long defaultValue) {
    long prop = getPropertyAsLong(propertyName);
    return prop == 0L ? defaultValue : prop;
  }

  private boolean isReuseConnections() {
    return JMeterUtils.getPropDefault(REUSE_CONNECTIONS_PROPERTY, false);
  }

  @VisibleForTesting
  protected void setReuseConnections(boolean doReuse) {
    JMeterUtils.setProperty(REUSE_CONNECTIONS_PROPERTY, Boolean.toString(doReuse));
  }

  private SSLType getSSLType() {
    return SSLType.valueOf(getPropertyAsString(CONFIG_SSL_TYPE));
  }

  @VisibleForTesting
  protected void setSslType(SSLType sslType) {
    setProperty(CONFIG_SSL_TYPE, sslType.name());
  }

  protected void setUseSsh(boolean useSsh) {
    setProperty(CONFIG_USE_SSH, useSsh);
  }

  protected boolean useSsh() {
    return getPropertyAsBoolean(CONFIG_USE_SSH, false);
  }

  protected void setSshUsername(String username) {
    setProperty(CONFIG_SSH_USERNAME, username);
  }

  protected String getSshUsername() {
    return getPropertyAsString(CONFIG_SSH_USERNAME);
  }

  protected void setSshPassword(String password) {
    setProperty(CONFIG_SSH_PASSWORD, password);
  }

  protected String getSshPassword() {
    return getPropertyAsString(CONFIG_SSH_PASSWORD);
  }

  public Action getAction() {
    if (getPropertyAsString(ACTION_PROPERTY).isEmpty()) {
      return DEFAULT_ACTION;
    }
    return Action.valueOf(getPropertyAsString(ACTION_PROPERTY));
  }

  public void setAction(Action action) {
    setProperty(ACTION_PROPERTY, action.name());
  }

  public void setPayload(Inputs payload) {
    setProperty(new TestElementProperty(Inputs.INPUTS_PROPERTY, payload));
  }

  public AttentionKey getAttentionKey() {
    if (getPropertyAsString(ATTENTION_KEY_PROPERTY).isEmpty()) {
      return DEFAULT_ATTENTION_KEY;
    }
    return AttentionKey.valueOf(getPropertyAsString(ATTENTION_KEY_PROPERTY));
  }

  public void setAttentionKey(AttentionKey attentionKey) {
    setProperty(ATTENTION_KEY_PROPERTY, attentionKey.name());
  }

  public boolean getWaitSync() {
    return getPropertyAsBoolean(WAIT_SYNC_PROPERTY, true);
  }

  public void setWaitSync(boolean waitSync) {
    setProperty(WAIT_SYNC_PROPERTY, waitSync);
  }

  public String getWaitSyncTimeout() {
    return getPropertyAsString(WAIT_SYNC_TIMEOUT_PROPERTY, "" + DEFAULT_WAIT_SYNC_TIMEOUT_MILLIS);
  }

  public void setWaitSyncTimeout(String waitTimeoutSync) {
    setProperty(WAIT_SYNC_TIMEOUT_PROPERTY, waitTimeoutSync);
  }

  private long getWaitSyncTimeoutValue() {
    return getLongProperty(WAIT_SYNC_TIMEOUT_PROPERTY, DEFAULT_WAIT_SYNC_TIMEOUT_MILLIS);
  }

  public boolean getWaitCursor() {
    return getPropertyAsBoolean(WAIT_CURSOR_PROPERTY);
  }

  public void setWaitCursor(boolean waitCursor) {
    setProperty(WAIT_CURSOR_PROPERTY, waitCursor);
  }

  public String getWaitCursorTimeout() {
    return getPropertyAsString(WAIT_CURSOR_TIMEOUT_PROPERTY,
        String.valueOf(DEFAULT_WAIT_CURSOR_TIMEOUT_MILLIS));
  }

  public void setWaitCursorTimeout(String waitTimeoutCursor) {
    setProperty(WAIT_CURSOR_TIMEOUT_PROPERTY, waitTimeoutCursor);
  }

  private long getWaitCursorTimeoutValue() {
    return getLongProperty(WAIT_CURSOR_TIMEOUT_PROPERTY, DEFAULT_WAIT_CURSOR_TIMEOUT_MILLIS);
  }

  public String getWaitCursorRow() {
    return getPropertyAsString(WAIT_CURSOR_ROW_PROPERTY, String.valueOf(1));
  }

  public void setWaitCursorRow(String row) {
    setProperty(WAIT_CURSOR_ROW_PROPERTY, row);
  }

  private int getWaitCursorRowValue() {
    return getIntProperty(WAIT_CURSOR_ROW_PROPERTY, 1);
  }

  public String getWaitCursorColumn() {
    return getPropertyAsString(WAIT_CURSOR_COLUMN_PROPERTY, String.valueOf(1));
  }

  public void setWaitCursorColumn(String row) {
    setProperty(WAIT_CURSOR_COLUMN_PROPERTY, row);
  }

  private int getWaitCursorColumnValue() {
    return getIntProperty(WAIT_CURSOR_COLUMN_PROPERTY, 1);
  }

  public boolean getWaitSilent() {
    return getPropertyAsBoolean(WAIT_SILENT_PROPERTY);
  }

  public void setWaitSilent(boolean waitSilent) {
    setProperty(WAIT_SILENT_PROPERTY, waitSilent);
  }

  public String getWaitSilentTime() {
    return getPropertyAsString(WAIT_SILENT_TIME_PROPERTY,
        String.valueOf(DEFAULT_WAIT_SILENT_TIME_MILLIS));
  }

  public void setWaitSilentTime(String waitSilentTime) {
    setProperty(WAIT_SILENT_TIME_PROPERTY, waitSilentTime);
  }

  private long getWaitSilentTimeValue() {
    return getLongProperty(WAIT_SILENT_TIME_PROPERTY, DEFAULT_WAIT_SILENT_TIME_MILLIS);
  }

  public String getWaitSilentTimeout() {
    return getPropertyAsString(WAIT_SILENT_TIMEOUT_PROPERTY,
        String.valueOf(DEFAULT_WAIT_SILENT_TIMEOUT_MILLIS));
  }

  public void setWaitSilentTimeout(String waitSilentTimeout) {
    setProperty(WAIT_SILENT_TIMEOUT_PROPERTY, waitSilentTimeout);
  }

  private long getWaitSilentTimeoutValue() {
    return getLongProperty(WAIT_SILENT_TIMEOUT_PROPERTY, DEFAULT_WAIT_SILENT_TIMEOUT_MILLIS);
  }

  public boolean getWaitText() {
    return getPropertyAsBoolean(WAIT_TEXT_PROPERTY);
  }

  public void setWaitText(boolean waitText) {
    setProperty(WAIT_TEXT_PROPERTY, waitText);
  }

  public String getWaitTextRegex() {
    return getPropertyAsString(WAIT_TEXT_REGEX_PROPERTY);
  }

  public void setWaitTextRegex(String regex) {
    setProperty(WAIT_TEXT_REGEX_PROPERTY, regex);
  }

  public String getWaitTextAreaTop() {
    return getPropertyAsString(WAIT_TEXT_AREA_TOP_PROPERTY, String.valueOf(1));
  }

  public void setWaitTextAreaTop(String row) {
    setProperty(WAIT_TEXT_AREA_TOP_PROPERTY, row);
  }

  private int getWaitTextAreaTopValue() {
    return getIntProperty(WAIT_TEXT_AREA_TOP_PROPERTY, 1);
  }

  public String getWaitTextAreaLeft() {
    return getPropertyAsString(WAIT_TEXT_AREA_LEFT_PROPERTY, String.valueOf(1));
  }

  public void setWaitTextAreaLeft(String column) {
    setProperty(WAIT_TEXT_AREA_LEFT_PROPERTY, column);
  }

  private int getWaitTextAreaLeftValue() {
    return getIntProperty(WAIT_TEXT_AREA_LEFT_PROPERTY, 1);
  }

  public String getWaitTextAreaBottom() {
    return getPropertyAsString(WAIT_TEXT_AREA_BOTTOM_PROPERTY);
  }

  public void setWaitTextAreaBottom(String row) {
    setProperty(WAIT_TEXT_AREA_BOTTOM_PROPERTY, row);
  }

  private int getWaitTextAreaBottomValue() {
    return getIntProperty(WAIT_TEXT_AREA_BOTTOM_PROPERTY, Position.UNSPECIFIED_INDEX);
  }

  public String getWaitTextAreaRight() {
    return getPropertyAsString(WAIT_TEXT_AREA_RIGHT_PROPERTY);
  }

  public void setWaitTextAreaRight(String column) {
    setProperty(WAIT_TEXT_AREA_RIGHT_PROPERTY, column);
  }

  private int getWaitTextAreaRightValue() {
    return getIntProperty(WAIT_TEXT_AREA_RIGHT_PROPERTY, Position.UNSPECIFIED_INDEX);
  }

  public String getWaitTextTimeout() {
    return getPropertyAsString(WAIT_TEXT_TIMEOUT_PROPERTY,
        String.valueOf(DEFAULT_WAIT_TEXT_TIMEOUT_MILLIS));
  }

  public void setWaitTextTimeout(String timeout) {
    setProperty(WAIT_TEXT_TIMEOUT_PROPERTY, timeout);
  }

  private long getWaitTextTimeoutValue() {
    return getLongProperty(WAIT_TEXT_TIMEOUT_PROPERTY, DEFAULT_WAIT_TEXT_TIMEOUT_MILLIS);
  }

  public boolean getWaitDisconnect() {
    return getPropertyAsBoolean(WAIT_DISCONNECT_PROPERTY, false);
  }

  public void setWaitDisconnect(boolean waitForDisconnect) {
    setProperty(WAIT_DISCONNECT_PROPERTY, waitForDisconnect);
  }

  public String getWaitDisconnectTimeout() {
    return getPropertyAsString(WAIT_DISCONNECT_TIMEOUT_PROPERTY,
        "" + DEFAULT_DISCONNECT_TIMEOUT_MILLIS);
  }

  public void setWaitDisconnectTimeout(String timeout) {
    setProperty(WAIT_DISCONNECT_TIMEOUT_PROPERTY, timeout);
  }

  public void setWaitConditions(List<WaitCondition> waitConditions) {
    setWaitSync(false);
    for (WaitCondition waitCondition : waitConditions) {
      addWaitCondition(waitCondition);
    }
  }

  private void addWaitCondition(WaitCondition condition) {
    if (condition instanceof SyncWaitCondition) {
      setWaitSync(true);
      setWaitSyncTimeout(String.valueOf(condition.getTimeoutMillis()));
    } else if (condition instanceof TextWaitCondition) {
      TextWaitCondition textWait = (TextWaitCondition) condition;
      setWaitText(true);
      setWaitTextRegex(textWait.getRegex());
      Area searchArea = textWait.getSearchArea();
      setWaitTextAreaTop(String.valueOf(searchArea.getTop()));
      setWaitTextAreaLeft(String.valueOf(searchArea.getLeft()));
      setWaitTextAreaBottom(String.valueOf(searchArea.getBottom()));
      setWaitTextAreaRight(String.valueOf(searchArea.getRight()));
      setWaitTextTimeout(String.valueOf(condition.getTimeoutMillis()));
    } else if (condition instanceof CursorWaitCondition) {
      CursorWaitCondition cursorWait = (CursorWaitCondition) condition;
      setWaitCursor(true);
      Position cursorPosition = cursorWait.getPosition();
      setWaitCursorRow(String.valueOf(cursorPosition.getRow()));
      setWaitCursorColumn(String.valueOf(cursorPosition.getRow()));
      setWaitCursorTimeout(String.valueOf(condition.getTimeoutMillis()));
    } else if (condition instanceof SilentWaitCondition) {
      setWaitSilent(true);
      setWaitSilentTime(String.valueOf(condition.getStableTimeoutMillis()));
      setWaitSilentTimeout(String.valueOf(condition.getTimeoutMillis()));
    } else if (condition instanceof DisconnectWaitCondition) {
      setWaitDisconnect(true);
      setWaitDisconnectTimeout(String.valueOf(condition.getTimeoutMillis()));
    } else {
      throw new IllegalArgumentException("Unsupported condition type " + condition.getClass());
    }
  }

  private InputTestElement buildInputTestElement(Input input) {
    if (input instanceof CoordInput) {
      CoordInput coordInput = (CoordInput) input;
      CoordInputRowGUI ret = new CoordInputRowGUI();
      Position position = coordInput.getPosition();
      ret.setColumn(String.valueOf(position.getColumn()));
      ret.setRow(String.valueOf(position.getRow()));
      ret.setInput(coordInput.getInput());
      return ret;
    } else if (input instanceof LabelInput) {
      LabelInput labelInput = (LabelInput) input;
      LabelInputRowGUI ret = new LabelInputRowGUI();
      ret.setLabel(labelInput.getLabel());
      ret.setInput(labelInput.getInput());
      return ret;
    } else if (input instanceof NavigationInput) {
      NavigationInput navInput = (NavigationInput) input;
      NavigationInputRowGui ret = new NavigationInputRowGui();
      ret.setRepeated(String.valueOf(navInput.getRepeat()));
      ret.setInput(navInput.getInput());
      ret.setType(navInput.getNavigationType().getLabel());
      return ret;
    } else {
      throw new IllegalArgumentException("Unsupported input type " + input.getClass());
    }
  }

  @Override
  public SampleResult sample(Entry entry) {
    RteSampleResultBuilder resultBuilder = buildSampleResultBuilder();
    RteProtocolClient client = null;

    try {
      client = getClient();
      configureWaitForDisconnect(client);
      if (getAction() == Action.DISCONNECT) {
        if (client != null) {
          disconnect(client);
        }
        return resultBuilder.withSuccessResponse(null)
            .build();
      }
      if (client == null) {
        client = buildClient();
        resultBuilder.withConnectEndNow();
        if (getAction() == Action.SEND_INPUT) {
          client.await(Collections
              .singletonList(new SyncWaitCondition(getConnectionTimeout(), getStableTimeout())));
        }
      }
      RequestListener<RteProtocolClient> requestListener = new RequestListener<>(resultBuilder,
          client);
      client.addTerminalStateListener(requestListener);

      try {
        if (getAction() == Action.SEND_INPUT) {
          resultBuilder.withInputInhibitedRequest(client.isInputInhibited().orElse(false))
              .withAttentionKey(getAttentionKey())
              .withInputs(getInputs());
          client.send(getInputs(), getAttentionKey(), getCharacterTimeout());
        }
        List<WaitCondition> waiters = getWaitersList();
        if (!waiters.isEmpty()) {
          client.await(waiters);
        }
        resultBuilder.withSuccessResponse(client);
      } finally {
        client.resetAlarm();
        requestListener.stop();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.error("The sampling has been interrupted", e);
      return resultBuilder.withFailure(e)
          .build();
    } catch (TimeoutException e) {
      LOG.warn("Timeout error", e);
      return resultBuilder.withTimeoutFailure(e, client)
          .build();
    } catch (Exception e) {
      LOG.error("Error while sampling the remote terminal", e);
      return resultBuilder.withFailure(e)
          .build();
    }
    return resultBuilder.build();
  }

  private void configureWaitForDisconnect(RteProtocolClient client) {
    if (client == null) {
      return;
    }
    boolean expectedDisconnection =
        getWaitersList().stream().anyMatch(l -> l instanceof DisconnectWaitCondition);

    ServerDisconnectHandler serverDisconnectHandler = new ServerDisconnectHandler(
        expectedDisconnection) {
      @Override
      public void onDisconnection(ExceptionHandler exceptionHandler) {
        if (!this.isExpectedDisconnection) {
          exceptionHandler.setPendingError(new ConnectionClosedException());
        }
      }
    };
    client.setDisconnectionHandler(serverDisconnectHandler);
  }

  public static long getCharacterTimeout() {
    return JMeterUtils.getPropDefault(CONFIG_CHARACTER_TIMEOUT, DEFAULT_CHARACTER_TIMEOUT_MILLIS);
  }

  @VisibleForTesting
  public static void setCharacterTimeout(long timeoutMillis) {
    JMeterUtils.setProperty(CONFIG_CHARACTER_TIMEOUT, "" + timeoutMillis);
  }

  private RteSampleResultBuilder buildSampleResultBuilder() {
    return new RteSampleResultBuilder()
        .withLabel(getName())
        .withServer(getServer())
        .withPort(getPort())
        .withProtocol(getProtocol())
        .withTerminalType(getTerminalType())
        .withSslType(getSSLType())
        .withAction(getAction());
  }

  private RteProtocolClient getClient() {
    String clientId = buildConnectionId();
    Map<String, RteProtocolClient> clients = CONNECTIONS.get();
    return clients.get(clientId);
  }

  private String buildConnectionId() {
    return getServer() + ":" + getPort();
  }

  private void disconnect(RteProtocolClient client) throws RteIOException {
    CONNECTIONS.get().remove(buildConnectionId());
    client.disconnect();
  }

  private RteProtocolClient buildClient()
      throws RteIOException, InterruptedException, TimeoutException {
    RteProtocolClient client = protocolFactory.apply(getProtocol());
    configureClient(client);
    client.connect(getServer(), getPort(), getSSLType(), getTerminalType(), getConnectionTimeout());
    CONNECTIONS.get().put(buildConnectionId(), client);
    return client;
  }

  private void configureClient(RteProtocolClient client) throws RteIOException {
    if (client == null) {
      return;
    }
    if (useSsh()) {
      client.configureSecureClient(getTerminalType(), getSshUsername(), getSshPassword(),
          getServer());
    } else {
      client.configureClient(getTerminalType(), getSSLType(), getServer());
    }
  }

  private List<Input> getInputs() {
    List<Input> inputs = new ArrayList<>();
    for (JMeterProperty p : getInputsTestElement()) {
      InputTestElement c = (InputTestElement) p.getObjectValue();
      inputs.add(c.toInput());
    }
    return inputs;
  }

  public void setInputs(List<Input> inputs) {
    Inputs testElement = new Inputs();
    for (Input input : inputs) {
      testElement.addInput(buildInputTestElement(input));
    }
    setPayload(testElement);
  }

  public Inputs getInputsTestElement() {
    return (Inputs) getProperty(Inputs.INPUTS_PROPERTY).getObjectValue();
  }

  private List<WaitCondition> getWaitersList() {
    List<WaitCondition> waiters = new ArrayList<>();
    if (getWaitSync()) {
      waiters.add(new SyncWaitCondition(getWaitSyncTimeoutValue(), getStableTimeout()));
    }
    if (getWaitCursor()) {
      waiters.add(buildCursorWaitCondition());
    }
    if (getWaitSilent()) {
      waiters.add(new SilentWaitCondition(getWaitSilentTimeoutValue(), getWaitSilentTimeValue()));
    }
    if (getWaitText()) {
      waiters.add(buildTextWaitCondition());
    }
    if (getWaitDisconnect()) {
      waiters.add(new DisconnectWaitCondition(Long.parseLong(getWaitDisconnectTimeout())));
    }
    waiters.sort(Comparator.comparing(WaitCondition::getTimeoutMillis));
    return waiters;
  }

  private CursorWaitCondition buildCursorWaitCondition() {
    return new CursorWaitCondition(
        new Position(getWaitCursorRowValue(), getWaitCursorColumnValue()),
        getWaitCursorTimeoutValue(), getStableTimeout());
  }

  private TextWaitCondition buildTextWaitCondition() {
    return new TextWaitCondition(
        JMeterUtils.getPattern(getWaitTextRegex()),
        JMeterUtils.getMatcher(),
        Area.fromTopLeftBottomRight(getWaitTextAreaTopValue(), getWaitTextAreaLeftValue(),
            getWaitTextAreaBottomValue(), getWaitTextAreaRightValue()),
        getWaitTextTimeoutValue(),
        getStableTimeout());
  }

  @Override
  public void threadStarted() {
  }

  @Override
  public void threadFinished() {
    closeConnections();
  }

  private void closeConnections() {
    CONNECTIONS.get().values().forEach(c -> {
      try {
        c.disconnect();
      } catch (Exception e) {
        LOG.error("Problem while closing RTE connection", e);
      }
    });
    CONNECTIONS.get().clear();
  }

  @Override
  public void iterationStart(LoopIterationEvent loopIterationEvent) {
    if (!isReuseConnections() && isFirstRteSamplerInLoop()) {
      closeConnections();
    }
  }

  private boolean isFirstRteSamplerInLoop() {
    JMeterVariables vars = getThreadContext().getVariables();
    Integer currentThreadIteration = vars.getIteration();
    String rteIterationVarName = "RTESampler.iteration";
    Integer lastRteSamplerIteration = (Integer) vars.getObject(rteIterationVarName);
    if (!currentThreadIteration.equals(lastRteSamplerIteration)) {
      vars.putObject(rteIterationVarName, currentThreadIteration);
      return true;
    }
    return false;
  }
}
