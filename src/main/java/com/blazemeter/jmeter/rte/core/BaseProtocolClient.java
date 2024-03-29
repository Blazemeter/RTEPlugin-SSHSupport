package com.blazemeter.jmeter.rte.core;

import com.blazemeter.jmeter.rte.core.exceptions.ConnectionClosedException;
import com.blazemeter.jmeter.rte.core.exceptions.RteIOException;
import com.blazemeter.jmeter.rte.core.listener.ExceptionHandler;
import com.blazemeter.jmeter.rte.core.ssl.SSLContextFactory;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.core.wait.ConditionWaiter;
import com.blazemeter.jmeter.rte.core.wait.WaitCondition;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.net.SocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseProtocolClient implements RteProtocolClient {

  protected static final ThreadFactory NAMED_THREAD_FACTORY = (runnable) -> new Thread(runnable,
      "STABLE-TIMEOUT-EXECUTOR");
  private static final Logger LOG = LoggerFactory.getLogger(BaseProtocolClient.class);
  protected ExceptionHandler exceptionHandler;
  protected ScheduledExecutorService stableTimeoutExecutor;
  private ServerDisconnectHandler serverDisconnectHandler;

  protected SocketFactory getSocketFactory(SSLType sslType, String server) throws RteIOException {
    if (sslType != null && sslType != SSLType.NONE) {
      try {
        return SSLContextFactory.buildSSLContext(sslType).getSocketFactory();
      } catch (IOException | GeneralSecurityException e) {
        throw new RteIOException(e, server);
      }
    } else {
      return SocketFactory.getDefault();
    }
  }

  @Override
  public void send(List<Input> input, AttentionKey attentionKey, long echoTimeoutMillis)
      throws RteIOException {
    LOG.info("Sending the following inputs: {}.", input);
    exceptionHandler.throwAnyPendingError();
    input.forEach(i -> setField(i, echoTimeoutMillis));
    sendAttentionKey(attentionKey);
    exceptionHandler.throwAnyPendingError();
  }

  protected abstract void setField(Input input, long echoTimeoutMillis);

  protected abstract void sendAttentionKey(AttentionKey attentionKey);

  @Override
  public void await(List<WaitCondition> waitConditions)
      throws InterruptedException, TimeoutException, RteIOException {
    List<ConditionWaiter<?>> listeners = waitConditions.stream()
        .map(this::buildWaiter)
        .collect(Collectors.toList());
    try {
      for (ConditionWaiter<?> listener : listeners) {
        listener.await();
      }
    } finally {
      listeners.forEach(ConditionWaiter::stop);
    }
  }

  protected abstract ConditionWaiter<?> buildWaiter(WaitCondition waitCondition);

  @Override
  public void disconnect() throws RteIOException {
    if (stableTimeoutExecutor == null) {
      return;
    }
    doDisconnect();
    try {
      exceptionHandler.throwAnyPendingError();
    } catch (RteIOException e) {
      if (e.getCause() instanceof ConnectionClosedException) {
        LOG.trace("Ignoring connection closed exception when disconnecting", e);
      } else {
        throw e;
      }
    }
  }

  protected abstract void doDisconnect();

  @Override
  public void setDisconnectionHandler(ServerDisconnectHandler serverDisconnectHandler) {
    this.serverDisconnectHandler = serverDisconnectHandler;
  }

  @Override
  public boolean isServerDisconnected() {
    if (serverDisconnectHandler != null) {
      return serverDisconnectHandler.isExpectedDisconnection();
    }
    exceptionHandler
        .setPendingError(new UnsupportedOperationException("No disconnection handler set"));
    return false;
  }

  protected void handleServerDisconnection() {
    if (serverDisconnectHandler != null) {
      serverDisconnectHandler.onDisconnection(exceptionHandler);
      return;
    }
    exceptionHandler
        .setPendingError(new UnsupportedOperationException("No disconnection handler set"));
  }
}
