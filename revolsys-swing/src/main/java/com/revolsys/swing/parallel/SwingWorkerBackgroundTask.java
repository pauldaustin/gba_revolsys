package com.revolsys.swing.parallel;

import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

public class SwingWorkerBackgroundTask implements BackgroundTask {

  private final SwingWorker<?, ?> worker;

  public SwingWorkerBackgroundTask(final SwingWorker<?, ?> worker) {
    this.worker = worker;
  }

  @Override
  public void cancel() {
    this.worker.cancel(true);
  }

  @Override
  public void execute() {
    this.worker.execute();
  }

  @Override
  public StateValue getTaskStatus() {
    return this.worker.getState();
  }

  @Override
  public String getTaskThreadName() {
    return null;
  }

  @Override
  public String getTaskTitle() {
    return this.worker.toString();
  }

  @Override
  public boolean isCancelled() {
    return this.worker.isCancelled();
  }

  @Override
  public boolean isTaskCancelled() {
    return this.worker.isCancelled();
  }

  @Override
  public boolean isTaskClosed() {
    return this.worker.isDone();
  }
}
