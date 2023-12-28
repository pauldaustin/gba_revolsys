package com.revolsys.record.code;

import java.util.List;
import java.util.function.Consumer;

import org.jeometry.common.data.identifier.Identifier;

public class CodeTableEntryEmpty implements CodeTableEntry {

  public static CodeTableEntryEmpty INSTANCE = new CodeTableEntryEmpty();

  private CodeTableEntryEmpty() {
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public void withIdentifier(Consumer<Identifier> action) {
  }

  @Override
  public <V> void withValue(Consumer<V> action) {
  }

  @Override
  public <V> void withValues(Consumer<List<Object>> action) {
  }

}
