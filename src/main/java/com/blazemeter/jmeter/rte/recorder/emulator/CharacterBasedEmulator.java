package com.blazemeter.jmeter.rte.recorder.emulator;

import com.blazemeter.jmeter.rte.core.AttentionKey;
import com.blazemeter.jmeter.rte.core.CharacterBasedProtocolClient;
import com.blazemeter.jmeter.rte.core.Input;
import com.blazemeter.jmeter.rte.core.NavigationInput;
import com.blazemeter.jmeter.rte.core.NavigationInput.NavigationInputBuilder;
import com.blazemeter.jmeter.rte.core.Position;
import com.blazemeter.jmeter.rte.core.Screen;
import com.blazemeter.jmeter.rte.core.Segment;
import com.blazemeter.jmeter.rte.protocols.vt420.Vt420Client;
import com.blazemeter.jmeter.rte.sampler.NavigationType;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import nl.lxtreme.jvt220.terminal.ScreenChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterBasedEmulator extends
    XI5250CrtBase<CharacterBasedProtocolClient> implements ScreenChangeListener {

  private static final Logger LOG = LoggerFactory.getLogger(CharacterBasedEmulator.class);
  private Position lastCursorPosition;
  private StringBuilder inputBuffer = new StringBuilder();
  private final List<Input> inputs = new ArrayList<>();
  private int repetition;
  private Screen lastTerminalScreen;
  private boolean isAreaSelected;
  private Screen currentScreen;
  private SwingWorker<Object, Object> currentSwingWorker;
  private int pastingCharactersCount = 0;
  private int pastedCharactersCount = 0;
  private NavigationInputBuilder currentInput = new NavigationInputBuilder();

  public CharacterBasedEmulator() {
    attributeTranslator = new AttributeTranslator(getCrtBuffer()) {
      @Override
      public int calculateAttrFrom(Segment segment) {
        return segment.getColor().equals(Screen.DEFAULT_COLOR) ? DEFAULT_ATTR
            : SECRET_CREDENTIAL_ATTR;
      }
    };
  }

  @Override
  public synchronized void processKeyEvent(KeyEvent currentKeyEvent) {
    Optional<AttentionKey> attentionKey = getAttentionKeyFromKeyEvent(currentKeyEvent);
    LOG.debug("Received KeyEvent=[{}]", currentKeyEvent);
    switch (currentKeyEvent.getID()) {
      case KeyEvent.KEY_TYPED:
        if (attentionKey.isPresent() || isNavigationKey(currentKeyEvent)) {
          return;
        }
        LOG.debug("Processing key=[{}]", currentKeyEvent);
        handleTriggeredKey(currentKeyEvent);
        break;
      case KeyEvent.KEY_RELEASED:
        break;
      case KeyEvent.KEY_PRESSED:
        if (attentionKey.isPresent() || isNavigationKey(currentKeyEvent)) {
          LOG.debug("Processing key=[{}]", currentKeyEvent);
          handleTriggeredKey(currentKeyEvent);
        }
        break;
      default:
        throw new UnsupportedOperationException(
            String.format("KeyEvent with id [%s] not supported yet",
                currentKeyEvent.getID()));
    }
  }

  private Optional<AttentionKey> getAttentionKeyFromKeyEvent(KeyEvent currentKeyEvent) {
    return Optional.ofNullable(KEY_EVENTS.get(new KeyEventMap(currentKeyEvent.getModifiers(),
        currentKeyEvent.getExtendedKeyCode())));
  }

  private void handleTriggeredKey(KeyEvent e) {
    Optional<AttentionKey> attentionKey = getAttentionKeyFromKeyEvent(e);
    if (!attentionKey.isPresent() && !locked) {
      sendToTheServer(e);
      lockEmulator(false, "After sending the key to the server = " + e);
    } else if (attentionKey.isPresent()) {
      handleAttentionKey(e, attentionKey.get());
    } else {
      handleForcedChar(e);
    }
  }

  private void sendToTheServer(KeyEvent e) {
    lockEmulator(true, "Before sending key");
    currentSwingWorker = sendKeyEvent(e);
    currentSwingWorker.execute();
  }

  private void handleForcedChar(KeyEvent e) {
    currentSwingWorker = sendKeyEvent(e);
    currentSwingWorker.execute();
    lockEmulator(false, "Forced");
    e.consume();
  }

  private void handleAttentionKey(KeyEvent e, AttentionKey attentionKey) {
    if (isAttentionKeyValid(attentionKey)) {
      lastCursorPosition = getCursorPosition();
      lockEmulator(true, "Processing attention key " + attentionKey);
      processAttentionKey(e, attentionKey);
      lastTerminalScreen = new Screen(currentScreen);
    } else if (attentionKey == AttentionKey.RESET) {
      e.consume();
    } else {
      e.consume();
    }
  }

  @Override
  protected void processAttentionKey(KeyEvent e, AttentionKey attentionKey) {
    List<Input> fields = getPendingFields();
    for (TerminalEmulatorListener listener : terminalEmulatorListeners) {
      setKeyboardLock(true);
      listener.onAttentionKey(attentionKey, fields, sampleName);
    }
  }

  private boolean isNavigationKey(KeyEvent e) {
    List<Integer> navigationKeys = Arrays.asList(
        KeyEvent.VK_TAB, KeyEvent.VK_DOWN,
        KeyEvent.VK_UP, KeyEvent.VK_LEFT,
        KeyEvent.VK_RIGHT);
    return navigationKeys.contains(e.getKeyCode())
        || navigationKeys.contains(e.getExtendedKeyCode());
  }

  @Override
  protected List<Input> getPendingFields() {
    if (inputBuffer.length() > 0 || repetition != 0) {
      buildDefaultInputWhenNoNavigationType();
      insertCurrentInput();
    }
    List<Input> inputs = new ArrayList<>(this.inputs);
    this.inputs.clear();
    return inputs;
  }

  private SwingWorker<Object, Object> sendKeyEvent(KeyEvent e) {
    return new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() {
        recordInput(getKeyString(e));
        return null;
      }
    };
  }

  @Override
  public void setKeyboardLock(boolean lock) {
    locked = lock;
    statusPanel.setKeyboardStatus(lock);
  }

  @Override
  public synchronized void makePaste() {
    String value;
    try {
      value = getClipboardContent();
    } catch (IOException | UnsupportedFlavorException e) {
      LOG.warn("Error while trying to get clipboard content", e);
      return;
    }
    List<String> sequencesInClipboard = CharacterSequenceScaper.getSequencesIn(value);

    if (!sequencesInClipboard.isEmpty()) {
      String chunkAppearances = CharacterSequenceScaper.getSequenceChunkAppearancesIn(value);
      JOptionPane.showMessageDialog(this, "Clipboard content '" + String.join(", ",
              sequencesInClipboard) + "' is not "
              + "supported when pasting. \nAppearances of sequences near to: "
              + chunkAppearances, "Paste error",
          JOptionPane.INFORMATION_MESSAGE);
      LOG.error("Clipboard content contains unsupported ANSI sequence. RTE-Plugin may support "
              + "that/those sequence/s ({}) as an attention key or as a navigation input. "
              + "\nAppearances of sequences: {} ", sequencesInClipboard,
          chunkAppearances);
      LOG.info(
          "Check this page to understand what those characters means: https://en.wikipedia"
              + ".org/wiki/List_of_Unicode_characters#Control_codes");
      return;
    }
    pastingCharactersCount = value.length();
    pastedCharactersCount = 0;
    lockEmulator(true, "Line 190. Prior making paste.");
    currentSwingWorker = new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() {
        LOG.info("[PASTING]: '{}'", value);
        Arrays.stream(value.split(""))
            .forEach(c -> recordInput(c));
        return null;
      }
    };
    currentSwingWorker.execute();
  }

  private void lockEmulator(boolean isLock, String source) {
    LOG.info("LockEmulator {}. Source {}", isLock, source);
    setKeyboardLock(isLock);
    setCursorVisible(!isLock);
    pasteConsumer.accept(!isLock);
  }

  private String getClipboardContent() throws IOException, UnsupportedFlavorException {
    Clipboard clipboard = this.getToolkit().getSystemClipboard();
    return (String) clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor);
  }

  private void recordInput(String value) {
    Position cursorPosition = getCursorPosition();
    terminalClient.send(value);
    boolean validInput = validInput(cursorPosition);

    if (validInput) {
      Optional<NavigationType> navigationKey = getNavigationType(value);
      if (navigationKey.isPresent()) {
        if (currentInput.isVoid()) {
          buildNavigationInput(navigationKey.get());
        } else {
          insertCurrentInput();
          buildNavigationInput(navigationKey.get());
        }
      } else {
        if (lastCursorPosition != null
            && !lastCursorPosition.isConsecutiveWith(cursorPosition)
            && inputBuffer.length() != 0) {
          buildDefaultInputWhenNoNavigationType();
          insertCurrentInput();
          currentInput.withNavigationType(NavigationType.TAB);
        }
        inputBuffer.append(value);
      }
    }
    lastCursorPosition = new Position(cursorPosition);
    lastTerminalScreen = new Screen(currentScreen);
  }

  private Optional<NavigationType> getNavigationType(String finalValue) {
    Optional<NavigationType> navigationKey = Arrays.stream(NavigationType.values())
        .filter(v -> Vt420Client.NAVIGATION_KEYS.get(v).equals(finalValue))
        .findFirst();
    return navigationKey;
  }

  private void buildNavigationInput(NavigationType type) {
    if (inputBuffer.length() > 0 || (repetition != 0 && !type
        .equals(currentInput.getNavigationType()))) {
      buildDefaultInputWhenNoNavigationType();
      insertCurrentInput();
    }
    if (currentInput.getNavigationType() == null) {
      currentInput.withNavigationType(type);
    }

    currentInput.withRepeat(++repetition);
  }

  private void buildDefaultInputWhenNoNavigationType() {
    if (currentInput.getNavigationType() == null) {
      currentInput = new NavigationInputBuilder()
          .withRepeat(repetition)
          .withNavigationType(NavigationType.TAB);
    }
  }

  private void insertCurrentInput() {
    String val = inputBuffer.toString();
    if (CommandUtils.isControlCode(inputBuffer.toString())) {
      val = CommandUtils.getUnicodeString(inputBuffer.toString());
    }

    NavigationInput build = currentInput.withInput(val).build();
    inputs.add(build);
    currentInput = new NavigationInputBuilder();
    inputBuffer = new StringBuilder();
    repetition = 0;
  }

  private boolean validInput(Position positionBeforeSend) {
    if (currentScreen.equals(lastTerminalScreen) && getCursorPosition()
        .equals(lastCursorPosition)) {
      if (!positionBeforeSend.equals(lastCursorPosition)) {
        //in order to notice a difference when moving backwards like: LEFT
        lastCursorPosition = positionBeforeSend;
        return true;
      }
      return false;
    }
    return true;
  }

  private Position getCursorPosition() {
    return terminalClient.getCursorPosition().orElse(Position.DEFAULT_POSITION);
  }

  private String getKeyString(KeyEvent e) {
    int keyCode = e.getKeyCode();
    switch (keyCode) {
      case KeyEvent.VK_TAB:
        return Vt420Client.NAVIGATION_KEYS.get(NavigationType.TAB);
      case KeyEvent.VK_LEFT:
        return Vt420Client.NAVIGATION_KEYS.get(NavigationType.LEFT);
      case KeyEvent.VK_RIGHT:
        return Vt420Client.NAVIGATION_KEYS.get(NavigationType.RIGHT);
      case KeyEvent.VK_UP:
        return Vt420Client.NAVIGATION_KEYS.get(NavigationType.UP);
      case KeyEvent.VK_DOWN:
        return Vt420Client.NAVIGATION_KEYS.get(NavigationType.DOWN);
      case KeyEvent.VK_SPACE:
        return " ";
      case KeyEvent.VK_BACK_SPACE:
        return Vt420Client.ATTENTION_KEYS.get(AttentionKey.BACKSPACE);
      case KeyEvent.VK_HOME:
        return Vt420Client.ATTENTION_KEYS.get(AttentionKey.HOME);
      case KeyEvent.VK_END:
        return Vt420Client.ATTENTION_KEYS.get(AttentionKey.END);
      case KeyEvent.VK_INSERT:
        return Vt420Client.ATTENTION_KEYS.get(AttentionKey.INSERT);
      default:
        return String.valueOf(e.getKeyChar());
    }
  }

  @Override
  public synchronized void screenChanged(String s) {
    currentScreen = Screen.buildScreenFromText(s, new Dimension(80, 24));
    if (pastingCharactersCount == 0) {
      lockEmulator(false, "screenChanged. Pasting characters count is 0");
    } else if (++pastedCharactersCount == (pastingCharactersCount - 1)) {
      lockEmulator(false, "screenChanged. Pasted characters count is " + pastedCharactersCount);
      pastingCharactersCount = 0;
    }
  }

  @VisibleForTesting
  public void setKeyboardStatus(boolean isLock) {
    locked = isLock;
    statusPanel.setKeyboardStatus(isLock);
  }

  @Override
  public synchronized void teardown() {
    if (!currentInput.isVoid()) {
      insertCurrentInput();
    }
    currentInput = new NavigationInputBuilder();
    lastTerminalScreen = null;
    lastCursorPosition = null;
    currentScreen = null;
    terminalClient.removeScreenChangeListener(this);
    if (currentSwingWorker != null) {
      currentSwingWorker.cancel(true);
    }
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_CLICKED) {
      if (isAreaSelected) {
        super.setSelectedArea(null);
        isAreaSelected = false;
      } else {
        statusPanel.blinkBlockedCursor();
        this.requestFocus();
      }
    } else if (e.getID() == MouseEvent.MOUSE_PRESSED) {
      super.setIvMousePressed(true);
      super.setIvStartDragging(e);
    } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
      super.setIvMousePressed(false);
    }
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
      isAreaSelected = true;
    }
    super.processMouseMotionEvent(e);
  }
}
