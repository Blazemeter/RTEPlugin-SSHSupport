package com.blazemeter.jmeter.rte.protocols.vt420;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.rte.JMeterTestUtils;
import com.blazemeter.jmeter.rte.core.AttentionKey;
import com.blazemeter.jmeter.rte.core.NavigationInput;
import com.blazemeter.jmeter.rte.core.Position;
import com.blazemeter.jmeter.rte.core.Segment;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.blazemeter.jmeter.rte.core.exceptions.RteIOException;
import com.blazemeter.jmeter.rte.core.listener.ExceptionHandler;
import com.blazemeter.jmeter.rte.core.ssl.SSLContextFactory;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.core.wait.Area;
import com.blazemeter.jmeter.rte.core.wait.CursorWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.DisconnectWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.SyncWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.TextWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.WaitCondition;
import com.blazemeter.jmeter.rte.protocols.RteProtocolClientIT;
import com.blazemeter.jmeter.rte.sampler.NavigationType;
import com.blazemeter.jmeter.rte.sampler.RTESampler;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import nl.lxtreme.jvt220.terminal.ScreenChangeListener;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

public class Vt420ClientIT extends RteProtocolClientIT<Vt420Client> {

  private static final String USER_ID = "tt";
  private static final String DATA = "123456";
  private static final String PASSWORD = "passwd";
  private static final NavigationInput USER_ID_INPUT = new NavigationInput(0, NavigationType.TAB,
      USER_ID);
  private static final NavigationInput DATA_INPUT = new NavigationInput(0, NavigationType.TAB,
      DATA);
  private static final NavigationInput USER_PASSWORD_INPUT = new NavigationInput(0,
      NavigationType.TAB, PASSWORD);
  private static final Position USER_ID_CURSOR_POSITION = new Position(12, 42);
  private static final Position WELCOME_SCREEN_CURSOR_POSITION = new Position(12, 27);
  private static final String ARROW_NAVIGATION_SCREEN_HTML = "arrow-navigation-screen.html";

  private ScreenChangeListener listener;
  private CountDownLatch latch;


  @Rule
  public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

  @BeforeClass
  public static void setupClass() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Override
  protected Vt420Client buildClient() {
    return new Vt420Client();
  }

  @Override
  protected TerminalType getDefaultTerminalType() {
    return new TerminalType("VT420-7", new Dimension(80, 24));
  }

  @Override
  public void setup() throws Exception {
    super.setup();
    RTESampler.setCharacterTimeout(5000);
  }

  @Override
  protected List<Segment> buildExpectedFields() {
    return null;
  }

  private void loadLoginFlow() throws FileNotFoundException {
    loadFlow("login.yml");
  }

  @Test
  public void shouldGetWelcomeScreenWhenConnect() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    assertThat(client.getScreen())
        .isEqualTo(buildScreenFromHtmlFile("user-welcome-screen.html"));
  }

  @Test(expected = RteIOException.class)
  public void shouldThrowRteIOExceptionWhenConnectWithInvalidPort() throws RteIOException {
      configureClientAndConnect(SSLType.NONE, 1);
  }

  private void configureClientAndConnect(SSLType type, int port) throws RteIOException {
    TerminalType terminalType = getDefaultTerminalType();
    client.configureClient(terminalType, type, VIRTUAL_SERVER_HOST);
    connectSshServer(VIRTUAL_SERVER_HOST, port, type, terminalType, TIMEOUT_MILLIS);
  }

  private void waitForCursorPosition(Position position)
      throws InterruptedException, TimeoutException, RteIOException {
    client.await(Collections.singletonList(
        new CursorWaitCondition(position, TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS)));
  }
  /**
   * Since keymapping was modified for JPMC (@VT420Client.loadCustomAttKeysMapping()) mismatch
   * yaml flow server. Therefore, ignored.
   */
  @Ignore
  @Test
  public void shouldGetUserMenuScreenWhenSendUsername() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    waitForCursorPosition(WELCOME_SCREEN_CURSOR_POSITION);
    sendEnterAttentionKey();
    waitForCursorPosition(USER_ID_CURSOR_POSITION);
    assertThat(client.getScreen().withInvisibleCharsToSpaces())
        .isEqualTo(buildScreenFromHtmlFile("user-menu-screen.html"));
  }

  private void sendEnterAttentionKey() throws RteIOException {
    client.send(Collections.emptyList(), AttentionKey.ENTER, TIMEOUT_MILLIS);
  }
  /**
   * Since keymapping was modified for JPMC (@VT420Client.loadCustomAttKeysMapping()) mismatch
   * yaml flow server. Therefore, ignored.
   */
  @Ignore
  @Test
  public void shouldGetArrowNavigationScreenWhenSendCorrectCredentialsByTabulator()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    waitForCursorPosition(WELCOME_SCREEN_CURSOR_POSITION);
    sendEnterAttentionKey();
    waitForCursorPosition(USER_ID_CURSOR_POSITION);
    sendCredentialsByTabulator();
    awaitSync();
    assertThat(client.getScreen().withInvisibleCharsToSpaces())
        .isEqualTo(buildScreenFromHtmlFile(ARROW_NAVIGATION_SCREEN_HTML));

  }

  private void sendCredentialsByTabulator()
      throws RteIOException {
    client.send(Arrays.asList(USER_ID_INPUT, DATA_INPUT, USER_PASSWORD_INPUT), AttentionKey.ENTER,
        TIMEOUT_MILLIS);
  }

  private void awaitSync() throws InterruptedException, TimeoutException, RteIOException {
    client.await(Collections.singletonList(new SyncWaitCondition(3000, STABLE_TIMEOUT_MILLIS)));
  }

  @Test(expected = RteIOException.class)
  public void shouldThrowRteIOExceptionWhenSendAndServerDown() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    server.stop(SERVER_STOP_TIMEOUT);
    sendEnterAttentionKey();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowUnsupportedOperationExceptionWhenAwaitWithUndefinedCondition()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    List<WaitCondition> conditions = Collections
        .singletonList(new WaitCondition(TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS) {
          @Override
          public String getDescription() {
            return "test";
          }
        });
    client.await(conditions);
  }

  @Test(expected = TimeoutException.class)
  public void shouldThrowTimeoutExceptionWhenCursorWaitAndNotExpectedCursorPosition()
      throws Exception {
    Position failingCursorPosition = new Position(1, 1);
    loadLoginFlow();
    connectToVirtualService();
    waitForCursorPosition(WELCOME_SCREEN_CURSOR_POSITION);
    sendEnterAttentionKey();
    waitForCursorPosition(failingCursorPosition);
  }

  @Test(expected = TimeoutException.class)
  public void shouldThrowTimeoutExceptionWhenTextWaitWithNoMatchingRegex()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    waitForCursorPosition(WELCOME_SCREEN_CURSOR_POSITION);
    sendEnterAttentionKey();
    client.await(Collections
        .singletonList(new TextWaitCondition(new Perl5Compiler().compile("NOT-IN-SCREEN"),
            new Perl5Matcher(),
            Area.fromTopLeftBottomRight(1, 1, Position.UNSPECIFIED_INDEX,
                Position.UNSPECIFIED_INDEX),
            TIMEOUT_MILLIS,
            STABLE_TIMEOUT_MILLIS)));
  }

  @Test
  public void shouldNotThrowExceptionWhenDisconnectAndServerDown() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    server.stop(SERVER_STOP_TIMEOUT);
    client.disconnect();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowUnsupportedOperationExceptionWhenSelectAttentionKeyUnsupported()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    client.send(Collections.emptyList(), AttentionKey.RESET, 0);
  }
  /**
   * Since last changes for JPMC, there is no worry about sending inputs weven thought the server
   * haven't responded yet or the keyboard is locked.
   * Therefore, ignored for the moment.
   */
  @Ignore
  @Test(expected = RteIOException.class)
  public void shouldThrowTimeoutExceptionWhenNoScreenChangesAndSendText() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    ExceptionHandler exceptionHandler = new ExceptionHandler("");
    client.setExceptionHandler(exceptionHandler);
    waitForCursorPosition(WELCOME_SCREEN_CURSOR_POSITION);
    client.send("E");
    exceptionHandler.throwAnyPendingError();
  }

  /**
   * Since keymapping was modified for JPMC (@VT420Client.loadCustomAttKeysMapping()) mismatch
   * yaml flow server. Therefore, ignored.
   */
  @Ignore
  @Test
  public void shouldGetSuccessScreenWhenSendingInputsByNavigation() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    waitForCursorPosition(WELCOME_SCREEN_CURSOR_POSITION);
    sendEnterAttentionKey();
    waitForCursorPosition(USER_ID_CURSOR_POSITION);
    sendCredentialsByTabulator();
    awaitSync();
    waitForCursorPosition(new Position(1, 54));
    sendArrowMovementsScreenDataWithSyncWaitCondition();
    assertThat(client.getScreen().withInvisibleCharsToSpaces())
        .isEqualTo(buildScreenFromHtmlFile("login-success-screen.html"));
  }

  private void sendArrowMovementsScreenDataWithSyncWaitCondition()
      throws RteIOException, TimeoutException, InterruptedException {
    client.send(Arrays.asList(
        new NavigationInput(1, NavigationType.DOWN, "00"),
        new NavigationInput(2, NavigationType.LEFT, "dev"),
        new NavigationInput(1, NavigationType.UP, "test"),
        new NavigationInput(1, NavigationType.RIGHT, "root")),
        AttentionKey.ENTER,
        TIMEOUT_MILLIS);
    awaitSync();
  }

  @Test
  public void shouldGetWelcomeScreenWhenConnectWithSsl() throws Exception {
    server.stop(SERVER_STOP_TIMEOUT);
    loadLoginFlow();
    SSLContextFactory.setKeyStore(findResource("/.keystore").getFile());
    SSLContextFactory.setKeyStorePassword("changeit");
    server.setSslEnabled(true);
    server.start();
    configureClientAndConnect(SSLType.TLS, server.getPort());
    awaitSync();
    assertThat(client.getScreen().withInvisibleCharsToSpaces())
        .isEqualTo(buildScreenFromHtmlFile("user-welcome-screen.html"));
  }

  /**
   * Since keymapping was modified for JPMC (@VT420Client.loadCustomAttKeysMapping()) mismatch
   * yaml flow server. Therefore, ignored.
   */
  @Ignore
  @Test
  public void shouldWaitForDisconnectWhenServerDisconnects() throws Exception {
    loadFlow("login-and-disconnect.yml");
    connectToVirtualService();
    waitForCursorPosition(WELCOME_SCREEN_CURSOR_POSITION);
    sendEnterAttentionKey();
    waitForCursorPosition(USER_ID_CURSOR_POSITION);
    sendCredentialsByTabulator();
    isDisconnectionExpected.update(true);
    client.await(Collections.singletonList(new DisconnectWaitCondition(5000)));
  }

  @Test
  public void shouldReturnTrueWhenAlarmSounds() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    assertThat(client.isAlarmOn()).isTrue();
  }

  @Test
  public void shouldReturnFalseWhenAlarmDoesNotSound() throws Exception{
    loadLoginFlow();
    connectToVirtualService();
    client.resetAlarm();
    sendEnterAttentionKey();
    awaitSync();
    assertThat(client.isAlarmOn()).isFalse();
  }

  @Test
  public void shouldVerifyAlarmStatusWhenResetAlarmIsUsed() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    boolean alarmStatus = client.isAlarmOn();
    client.resetAlarm();
    assertThat(client.isAlarmOn()).isNotEqualTo(alarmStatus);
  }

  private void connectSshServer(String s, int i, SSLType none, TerminalType sshSupport, int i2) throws RteIOException {
    client.connect(s, i, none, sshSupport, i2);
  }

  public void setupListener() {
    latch = new CountDownLatch(1);
    listener = screen -> latch.countDown();
  }

  public void awaitForScreen() throws InterruptedException {
    latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
  }
}

