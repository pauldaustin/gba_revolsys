package com.revolsys.io;

import java.util.Map;

public class DelegatingWriter<T> extends AbstractWriter<T> {
  private final Writer<T> writer;

  public DelegatingWriter(
    final Writer<T> writer) {
    this.writer = writer;
  }

  @Override
  public void close() {
    writer.close();
  }

  @Override
  public void flush() {
    writer.flush();
  }

  @Override
  public Map<String, Object> getProperties() {
    return writer.getProperties();
  }

  @Override
  public <C> C getProperty(
    final String name) {
    return (C)writer.getProperty(name);
  }

  @Override
  public void setProperty(
    final String name,
    final Object value) {
    writer.setProperty(name, value);
  }

  public void write(
    final T object) {
    writer.write(object);
  }
}
