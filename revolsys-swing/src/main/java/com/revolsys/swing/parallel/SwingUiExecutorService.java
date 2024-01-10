package com.revolsys.swing.parallel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

public class SwingUiExecutorService extends AbstractExecutorService {

  public SwingUiExecutorService() {
  }

  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit)
    throws InterruptedException {
    return false;
  }

  @Override
  public void execute(final Runnable command) {
    SwingUtilities.invokeLater(command);
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public void shutdown() {
  }

  @Override
  public List<Runnable> shutdownNow() {
    return Collections.emptyList();
  }

}
