package com.revolsys.swing.parallel;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jeometry.common.exception.Exceptions;

import com.revolsys.beans.PropertyChangeSupport;
import com.revolsys.collection.map.Maps;
import com.revolsys.parallel.ThreadInterruptedException;
import com.revolsys.util.Property;

public class Invoke {

  private static final PropertyChangeSupport PROPERTY_CHANGE_SUPPORT = new PropertyChangeSupport(
    Invoke.class);

  private static final List<BackgroundTask> TASKS = new LinkedList<>();

  private static final Map<String, List<BackgroundTask>> WAITING_TASKS = new HashMap<>();

  private static final Map<String, Integer> RUNNING_COUNTS = new HashMap<>();

  private static final Map<Object, BackgroundTask> SOURCE_TO_TASK = new HashMap<>();

  private static final ReentrantLock LOCK = new ReentrantLock();

  public static Instant lastTaskTime = Instant.now();

  public static <V> V andWait(final Callable<V> callable) {
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        return callable.call();
      } else {
        final RunnableCallable<V> runnable = new RunnableCallable<>(callable);
        SwingUtilities.invokeAndWait(runnable);
        return runnable.getResult();
      }
    } catch (final Exception e) {
      return Exceptions.throwUncheckedException(e);
    }
  }

  public static void andWait(final Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      try {
        SwingUtilities.invokeAndWait(runnable);
      } catch (final InterruptedException e) {
        throw new ThreadInterruptedException(e);
      } catch (final InvocationTargetException e) {
        Exceptions.throwCauseException(e);
      }
    }
  }

  public static <V> SwingWorker<V, Void> background(final String key, final int maxThreads,
    final String description, final Supplier<V> backgroundTask, final Consumer<V> doneTask) {
    if (backgroundTask != null) {
      if (SwingUtilities.isEventDispatchThread()) {
        final SwingWorker<V, Void> worker = new SupplierConsumerMaxThreadsSwingWorker<>(key,
          maxThreads, description, backgroundTask, doneTask);
        worker(worker);
        return worker;
      } else {
        try {
          final V result = backgroundTask.get();
          later(() -> doneTask.accept(result));
        } catch (final Exception e) {
          Exceptions.throwUncheckedException(e);
        }
      }
    }
    return null;
  }

  public static SwingWorker<?, ?> background(final String description,
    final Runnable backgroundTask) {
    if (backgroundTask != null) {
      if (SwingUtilities.isEventDispatchThread()) {
        final SwingWorker<?, ?> worker = new SupplierConsumerSwingWorker<>(description, () -> {
          backgroundTask.run();
          return null;
        });
        worker(worker);
        return worker;
      } else {
        backgroundTask.run();
      }
    }
    return null;
  }

  public static <V> SwingWorker<V, Void> background(final String description,
    final Supplier<V> backgroundTask, final Consumer<V> doneTask) {
    if (backgroundTask != null) {
      if (SwingUtilities.isEventDispatchThread()) {
        final SwingWorker<V, Void> worker = new SupplierConsumerSwingWorker<>(description,
          backgroundTask, doneTask);
        worker(worker);
        return worker;
      } else {
        final V result = backgroundTask.get();
        later(() -> doneTask.accept(result));
      }
    }
    return null;
  }

  private static void fireChanged() {
    final var lastTaskTime = Invoke.lastTaskTime;
    Invoke.lastTaskTime = Instant.now();
    PROPERTY_CHANGE_SUPPORT.firePropertyChange("taskTime", lastTaskTime, Invoke.lastTaskTime);
  }

  public static PropertyChangeSupport getPropertyChangeSupport() {
    return PROPERTY_CHANGE_SUPPORT;
  }

  public static List<BackgroundTask> getTasks() {
    LOCK.lock();
    try {
      final var tasks = new ArrayList<BackgroundTask>();
      for (final var task : TASKS) {
        if (!task.isTaskClosed()) {
          tasks.add(task);
        }
      }
      return tasks;
    } finally {
      LOCK.unlock();
    }
  }

  public static boolean hasTasks() {
    return !TASKS.isEmpty();
  }

  public static void later(final Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  public static void laterQueue(final Runnable runnable) {
    SwingUtilities.invokeLater(runnable);
  }

  public static <V> boolean swingThread(final Consumer<V> action, final V arg) {
    if (SwingUtilities.isEventDispatchThread()) {
      return true;
    } else {
      SwingUtilities.invokeLater(() -> action.accept(arg));
      return false;
    }
  }

  public static boolean swingThread(final Runnable action) {
    if (SwingUtilities.isEventDispatchThread()) {
      return true;
    } else {
      SwingUtilities.invokeLater(action);
      return false;
    }
  }

  public static <I, O> Function<I, CompletableFuture<O>> uiFuture(
    final BiConsumer<I, CompletableFuture<O>> action) {
    return v -> uiFuture(v, action);
  }

  public static <O> CompletableFuture<O> uiFuture(final Consumer<CompletableFuture<O>> action) {
    final var future = new CompletableFuture<O>();
    later(() -> action.accept(future));
    return future;
  }

  public static <O, I> CompletableFuture<O> uiFuture(I v,
    final BiConsumer<I, CompletableFuture<O>> action) {
    final var future = new CompletableFuture<O>();
    later(() -> action.accept(v, future));
    return future;
  }

  public static void uiThenBackground(final Runnable runnable, final String task,
    final Runnable backgroundTask) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
      background(task, backgroundTask);
    } else {
      SwingUtilities.invokeLater(() -> uiThenBackground(runnable, task, backgroundTask));
    }
  }

  public static void worker(final SwingWorker<? extends Object, ? extends Object> worker) {
    boolean execute = true;
    BackgroundTask task;
    LOCK.lock();
    try {
      if (SOURCE_TO_TASK.containsKey(worker)) {
        return;
      }
      task = BackgroundTask.fromWorker(worker);
      SOURCE_TO_TASK.put(worker, task);
      TASKS.add(task);
      if (worker instanceof final MaxThreadsSwingWorker maxThreadsWorker) {
        final String workerKey = maxThreadsWorker.getWorkerKey();
        final int maxThreads = maxThreadsWorker.getMaxThreads();
        final int threads = Maps.getCount(RUNNING_COUNTS, workerKey);
        if (threads >= maxThreads) {
          execute = false;
          Maps.addToList(WAITING_TASKS, workerKey, BackgroundTask.fromWorker(worker));
        } else {
          Maps.addCount(RUNNING_COUNTS, workerKey);
        }
      }
    } finally {
      LOCK.unlock();
    }
    worker.addPropertyChangeListener(event -> {
      if (task.isTaskCancelled() || task.isTaskClosed()) {
        LOCK.lock();
        try {
          SOURCE_TO_TASK.remove(worker);
          TASKS.remove(task);
          if (worker instanceof final MaxThreadsSwingWorker maxThreadsWorker) {
            final String taskKey = maxThreadsWorker.getWorkerKey();
            final int maxThreads = maxThreadsWorker.getMaxThreads();
            int runningCount = Maps.decrementCount(RUNNING_COUNTS, taskKey);
            final var waitingTasks = WAITING_TASKS.get(taskKey);
            while (Property.hasValue(waitingTasks) && runningCount < maxThreads) {
              final var nextWorker = waitingTasks.remove(0);
              Maps.addCount(RUNNING_COUNTS, taskKey);
              nextWorker.execute();
              runningCount++;
            }
          }
          for (final var iterator = TASKS.iterator(); iterator.hasNext();) {
            final var otherTask = iterator.next();
            if (otherTask.isTaskClosed()) {
              iterator.remove();
            }
          }
        } finally {
          LOCK.unlock();
        }
        fireChanged();
      }
    });
    fireChanged();
    if (execute) {
      worker.execute();
    }
  }

  /**
   * Use a swing worker to make sure the task is done later in the UI thread.
   *
   * @param description
   * @param doneTask
   */
  public static void workerDone(final String description, final Runnable doneTask) {
    if (doneTask != null) {
      final SwingWorker<Void, Void> worker = new SupplierConsumerSwingWorker<>(description, null,
        result -> doneTask.run());
      worker(worker);
    }
  }

  private Invoke() {
  }
}
