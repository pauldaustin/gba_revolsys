package com.revolsys.io;

import java.io.Closeable;
import java.util.function.Consumer;

import org.jeometry.common.exception.Exceptions;

@FunctionalInterface
public interface BaseCloseable extends Closeable {

  static Consumer<AutoCloseable> CLOSER = (resource) -> {
    try {
      resource.close();
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  };

  static <C extends BaseCloseable> Consumer<? super C> closer() {
    return CLOSER;
  }

  @Override
  void close();

  default BaseCloseable wrap() {
    return new CloseableWrapper(this);
  }
}
