package com.blazemeter.jmeter.rte.recorder;

import com.blazemeter.jmeter.rte.core.exceptions.RteIOException;

public interface RecordingStateListener {

  void onRecordingStart() throws RteIOException;

  void onRecordingStop();
  
  void onRecordingException(Exception e);

}
