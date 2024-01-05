package com.revolsys.swing.parallel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class SwingUiExecutorService extends AbstractExecutorService {

  public static final SwingUiExecutorService EXECUTOR = new SwingUiExecutorService();

  private SwingUiExecutorService() {
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return false;
  }

  @Override
  public void execute(Runnable command) {
    Invoke.later(command);
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
