package com.revolsys.record.code;

import java.util.Collections;
import java.util.List;

import org.jeometry.common.data.identifier.Identifier;

public class SingleValueCodeTableEntry implements CodeTableEntry {
  private final Identifier identifier;

  private final Object value;

  public SingleValueCodeTableEntry(final Identifier identifier, final Object value) {
    this.identifier = identifier;
    this.value = value;
  }

  @Override
  public Identifier getIdentifier() {
    return this.identifier;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue() {
    return (V)this.value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Object> getValues() {
    if (this.value instanceof final List list) {
      return list;
    } else {
      return Collections.singletonList(this.value);
    }
  }

  @Override
  public boolean isEmpty() {
    if (this.value instanceof final List list) {
      return list.isEmpty();
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return this.identifier.toString();
  }
}
