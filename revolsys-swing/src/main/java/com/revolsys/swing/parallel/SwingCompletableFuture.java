package com.revolsys.swing.parallel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SwingCompletableFuture<T> extends CompletableFuture<T> {

  public static <U> SwingCompletableFuture<U> completed(final U value) {
    final var future = new SwingCompletableFuture<U>();
    future.complete(value); // Handles null to NIL
    return future;
  }

  public static SwingCompletableFuture<Void> runAsync(final Runnable runnable) {
    final var future = new SwingCompletableFuture<Void>();
    future.completeAsync(() -> {
      runnable.run();
      return null;
    });
    return future;
  }

  public static SwingCompletableFuture<Void> runAsync(final Runnable runnable,
    final Executor executor) {
    final var future = new SwingCompletableFuture<Void>();
    future.completeAsync(() -> {
      runnable.run();
      return null;
    }, executor);
    return future;
  }

  public static <O> SwingCompletableFuture<O> ui(final Consumer<CompletableFuture<O>> action) {
    final var future = new SwingCompletableFuture<O>();
    Invoke.later(() -> action.accept(future));
    return future;
  }

  public static SwingCompletableFuture<Void> ui(final Runnable action) {
    final var future = new SwingCompletableFuture<Void>();
    Invoke.later(() -> {
      action.run();
      future.complete(null);
    });
    return future;
  }

  public static <O> SwingCompletableFuture<O> ui(final Supplier<O> action) {
    final var future = new SwingCompletableFuture<O>();
    Invoke.later(() -> {
      final var result = action.get();
      future.complete(result);
    });
    return future;
  }

  @Override
  public <U> SwingCompletableFuture<U> newIncompleteFuture() {
    return new SwingCompletableFuture<>();
  }

  @Override
  public <U> SwingCompletableFuture<U> thenCompose(
    final Function<? super T, ? extends CompletionStage<U>> fn) {
    return (SwingCompletableFuture<U>)super.thenCompose(fn);
  }

  @Override
  public SwingCompletableFuture<Void> thenRun(final Runnable action) {
    return (SwingCompletableFuture<Void>)super.thenRun(action);
  }

  public <O> Future<O> thenUiAccept(final Consumer<T> action) {
    return thenCompose((v) -> {
      final var future = new SwingCompletableFuture<O>();
      Invoke.later(() -> {
        action.accept(v);
        future.complete(null);
      });
      return future;
    });
  }

  public <O> SwingCompletableFuture<O> thenUiApply(final Function<T, O> action) {
    return thenCompose((v) -> {
      final var future = new SwingCompletableFuture<O>();
      Invoke.later(() -> {
        final var result = action.apply(v);
        future.complete(result);
      });
      return future;
    });
  }

  public <O> SwingCompletableFuture<O> thenUiComplete(
    final BiConsumer<T, CompletableFuture<O>> action) {
    return thenCompose((v) -> {
      final var future = new SwingCompletableFuture<O>();
      Invoke.later(() -> action.accept(v, future));
      return future;
    });
  }

  public <O> SwingCompletableFuture<O> thenUiComplete(final Consumer<CompletableFuture<O>> action) {
    return thenCompose((v) -> {
      final var future = new SwingCompletableFuture<O>();
      Invoke.later(() -> action.accept(future));
      return future;
    });
  }

  public SwingCompletableFuture<Void> thenUiRun(final Runnable action) {
    return thenCompose((v) -> {
      final var future = new SwingCompletableFuture<Void>();
      Invoke.later(() -> {
        action.run();
        future.complete(null);
      });
      return future;
    });
  }
}
