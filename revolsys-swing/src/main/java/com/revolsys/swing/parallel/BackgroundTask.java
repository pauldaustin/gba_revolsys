package com.revolsys.swing.parallel;

import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.util.Cancellable;

public interface BackgroundTask extends Cancellable {

  static BackgroundTask fromWorker(SwingWorker<? extends Object, ? extends Object> worker) {
    if (worker instanceof final BackgroundTask task) {
      return task;
    } else {
      return new SwingWorkerBackgroundTask(worker);
    }
  }

  static BackgroundTask startRunnable(final String taskTitle, final Runnable runnable) {
    return runnable(taskTitle, runnable)//
      .start();
  }

  static RunnableBackgroundTask runnable(final String taskTitle, final Runnable runnable) {
    return new RunnableBackgroundTask(taskTitle, runnable);
  }

  @Override
  default void cancel() {
  }

  default void execute() {
  }

  default MenuFactory getMenu() {
    return null;
  }

  default String getTaskMessage() {
    return null;
  }

  StateValue getTaskStatus();

  String getTaskThreadName();

  default long getTaskTime() {
    return -1;
  }

  String getTaskTitle();

  default boolean isTaskCancelled() {
    return false;
  }

  default boolean isTaskClosed() {
    return false;
  }
}
