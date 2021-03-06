package com.blazemeter.jmeter.rte.core;

import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.sampler.Action;
import com.helger.commons.annotation.VisibleForTesting;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.jmeter.samplers.SampleResult;

/*
We use this class instead of a custom SampleResult to avoid forward incompatibilities when trying
to upload JTL files generated with newer versions of the plugin on jmeter installations with older
versions of the plugin. This case may happen in BlazeMeter. Additionally, by using plain
SampleResult, we keep serialization simple and easily mapped to what can be seen in JMeter
interface. 
 */
public class RteSampleResultBuilder {

  public static final String FIELD_POSITION_SEPARATOR = ", ";
  public static final String HEADERS_TERMINAL_TYPE = "Terminal-type: ";
  public static final String CURSOR_POSITION_HEADER = "Cursor-position: ";
  public static final String FIELDS_POSITION_HEADER = "Field-positions: ";
  public static final String HEADERS_SEPARATOR = "\n";
  public static final String SEGMENTS = "Segments: ";
  private static final String INPUT_INHIBITED = "Input-inhibited";
  private SampleResult result;
  private String server;
  private int port;
  private Protocol protocol;
  private TerminalType terminalType;
  private SSLType sslType;
  private Action action;
  private Boolean inputInhibitedRequest;
  private List<Input> inputs;
  private AttentionKey attentionKey;
  private boolean inputInhibitedResponse;
  private Position cursorPosition;
  private boolean soundedAlarm;
  private Screen screen;
  private String responseHeaders;

  public RteSampleResultBuilder() {
    result = new SampleResult();
    result.sampleStart();
  }

  @VisibleForTesting
  public RteSampleResultBuilder(Position cursorPosition, Screen screen, String responseHeaders,
      TerminalType terminalType) {
    result = new SampleResult();
    result.sampleStart();
    this.screen = screen;
    this.cursorPosition = cursorPosition;
    this.responseHeaders = responseHeaders;
    this.terminalType = terminalType;
  }

  public long getCurrentTimeInMillis() {
    return result.currentTimeInMillis();
  }

  public boolean hasFailure() {
    return !result.getResponseCode().isEmpty();
  }

  public RteSampleResultBuilder withLabel(String label) {
    result.setSampleLabel(label);
    return this;
  }

  public RteSampleResultBuilder withConnectEndNow() {
    result.connectEnd();
    return this;
  }

  public RteSampleResultBuilder withLatencyEndNow() {
    result.latencyEnd();
    return this;
  }

  public RteSampleResultBuilder withEndTime(long endTime) {
    result.setEndTime(endTime);
    return this;
  }

  public RteSampleResultBuilder withSuccessResponse(RteProtocolClient client) {
    result.setSuccessful(true);
    if (client != null) {
      updateResponseFromClient(client);
    }
    if (result.getEndTime() == 0) {
      result.sampleEnd();
    }
    return this;
  }

  private void updateResponseFromClient(RteProtocolClient client) {
    cursorPosition = client.getCursorPosition().orElse(null);
    soundedAlarm = client.isAlarmOn();
    inputInhibitedResponse = client.isInputInhibited().orElse(false);
    screen = client.getScreen();
  }

  public RteSampleResultBuilder withFailure(Throwable e) {
    result.setSuccessful(false);
    result.setResponseCode(e.getClass().getName());
    result.setResponseMessage(e.getMessage());
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    result.setResponseData(sw.toString(), SampleResult.DEFAULT_HTTP_ENCODING);
    return this;
  }

  public RteSampleResultBuilder withTimeoutFailure(Throwable e, RteProtocolClient client) {
    result.setSuccessful(false);
    result.setResponseCode(e.getClass().getName());
    result.setResponseMessage(e.getMessage());
    if (client != null) {
      updateResponseFromClient(client);
    }
    return this;
  }

  public RteSampleResultBuilder withServer(String server) {
    this.server = server;
    return this;
  }

  public RteSampleResultBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  public RteSampleResultBuilder withProtocol(Protocol protocol) {
    this.protocol = protocol;
    return this;
  }

  public RteSampleResultBuilder withTerminalType(TerminalType terminalType) {
    this.terminalType = terminalType;
    return this;
  }

  public RteSampleResultBuilder withSslType(SSLType sslType) {
    this.sslType = sslType;
    return this;
  }

  public RteSampleResultBuilder withAction(Action action) {
    this.action = action;
    return this;
  }

  public RteSampleResultBuilder withInputInhibitedRequest(boolean inputInhibitedRequest) {
    this.inputInhibitedRequest = inputInhibitedRequest;
    return this;
  }

  public RteSampleResultBuilder withInputs(List<Input> inputs) {
    this.inputs = inputs;
    return this;
  }

  public RteSampleResultBuilder withAttentionKey(AttentionKey attentionKey) {
    this.attentionKey = attentionKey;
    return this;
  }

  public SampleResult build() {
    result.setRequestHeaders(buildRequestHeaders());
    result.setSamplerData(buildSamplerData());
    responseHeaders = responseHeaders == null ? buildResponseHeaders() : responseHeaders;
    result.setResponseHeaders(responseHeaders);
    result.setDataType(SampleResult.TEXT);
    if (result.getResponseDataAsString().isEmpty()) {
      result.setResponseData(screen != null ? screen.getText() : "", StandardCharsets.UTF_8.name());
    }
    return result;
  }

  private String buildRequestHeaders() {
    return "Server: " + server + "\n" +
            "Port: " + port + "\n" +
            "Protocol: " + protocol + "\n" +
            HEADERS_TERMINAL_TYPE + terminalType + "\n" +
            "Security: " + sslType + "\n" +
            "Action: " + action + "\n" +
            (inputInhibitedRequest != null ? "Input-inhibited: " + inputInhibitedRequest + 
                    "\n" : "");
  }

  private String buildSamplerData() {
    if (attentionKey == null) {
      return "";
    }
    return "AttentionKey: " + attentionKey + "\n"
        + "Inputs:\n"
        + inputs.stream()
        .map(Input::getCsv)
        .collect(Collectors.joining("\n"))
        + "\n";
  }

  private String buildResponseHeaders() {
    if (action == Action.DISCONNECT) {
      return "";
    }

    String fieldsPositions = getFieldsPositions();
    String segmentsAttributes = getSegmentsAttributes();
    
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Deprecated-headers", "Field-positions");
    headers.put("Sound-Alarm", Boolean.toString(soundedAlarm));
    headers.put(CURSOR_POSITION_HEADER, cursorPosition == null ? "" : cursorPosition.toString());
    headers.put("Segments", segmentsAttributes);
    headers.put(INPUT_INHIBITED, Boolean.toString(inputInhibitedResponse));
    //Field positions is deprecated and will be removed in future releases
    headers.put(FIELDS_POSITION_HEADER, fieldsPositions);
    
    return headers.entrySet().stream()
            .map(h -> h.getKey() + (h.getKey().contains(": ") ? "" : ": ") + h.getValue())
            .collect(Collectors.joining(HEADERS_SEPARATOR));
  }
  
  private String getFieldsPositions() {
    if (screen == null) {
      return "";
    }
    
    return screen.getSegments().stream()
        .filter(Segment::isEditable)
        .map(s -> s.getPositionRange().toString())
        .collect(Collectors.joining(", "));

  }

  private String getSegmentsAttributes() {
    if (screen == null) {
      return "";
    }

    return "[" + screen.getSegments().stream()
            .map(Segment::toJSON)
            .collect(Collectors.joining(", ")) + "]";
  }
}
