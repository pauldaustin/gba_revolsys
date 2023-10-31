package com.revolsys.parallel.process;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.BeanNameAware;

public interface Process extends Runnable, BeanNameAware {
  static ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

  default void close() {
  }

  String getBeanName();

  ProcessNetwork getProcessNetwork();

  default void initialize() {
  }

  void setProcessNetwork(final ProcessNetwork processNetwork);
}
