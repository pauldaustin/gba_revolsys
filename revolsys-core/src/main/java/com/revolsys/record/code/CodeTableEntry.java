package com.revolsys.record.code;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jeometry.common.data.identifier.Code;
import org.jeometry.common.data.identifier.Identifier;
import org.jeometry.common.data.type.DataType;
import org.jeometry.common.data.type.DataTypes;

import com.revolsys.util.Property;
import com.revolsys.util.Strings;

public interface CodeTableEntry {
  static CodeTableEntry EMPTY = CodeTableEntryEmpty.INSTANCE;

  static CodeTableEntry create(final Identifier identifier, final Object value) {
    return new CodeTableEntrySingleValue(identifier, value);
  }

  static Identifier getIdentifier(final CodeTableEntry entry) {
    if (entry == null) {
      return null;
    } else {
      return entry.getIdentifier();
    }
  }

  @SuppressWarnings("unchecked")
  static <V> V getValue(final CodeTableEntry entry) {
    if (entry == null) {
      return null;
    } else {
      return (V)entry.getValue();
    }
  }

  static List<Object> getValues(final CodeTableEntry entry) {
    if (entry == null) {
      return null;
    } else {
      return entry.getValues();
    }
  }

  static int maxLength(final Iterable<CodeTableEntry> entries) {
    int length = 0;
    for (final CodeTableEntry entry : entries) {
      final int valueLength = entry.getLength();
      if (valueLength > length) {
        length = valueLength;
      }
    }
    return length;
  }

  default CodeTableEntry addCallback(Consumer<CodeTableEntry> action) {
    return this;
  }

  default boolean equalsValue(final Object value) {
    return DataType.equal(value, getValue());
  }

  default Identifier getIdentifier() {
    return null;
  }

  default int getLength() {
    return getValue().toString().length();
  }

  default <V> V getValue() {
    return null;
  }

  default List<Object> getValues() {
    return Collections.emptyList();
  }

  default Identifier identifierOrDefault(Identifier identifier) {
    if (isLoaded()) {
      return getIdentifier();
    } else {
      return identifier;
    }
  }

  default Identifier identifierOrDefault(Object identifier) {
    if (isLoaded()) {
      return getIdentifier();
    } else {
      return Identifier.newIdentifier(identifier);
    }
  }

  default boolean isEmpty() {
    return false;
  }

  default boolean isLoaded() {
    return true;
  }

  default String toCodeString() {
    final List<Object> values = getValues();
    if (values == null || values.isEmpty()) {
      return null;
    } else if (values.size() == 1) {
      final Object codeValue = values.get(0);
      if (codeValue instanceof final Code code) {
        return code.getDescription();
      } else if (codeValue instanceof final String string) {
        if (Property.hasValue(string)) {
          return string;
        } else {
          return null;
        }
      } else {
        return DataTypes.toString(codeValue);
      }
    } else {
      return Strings.toString(values);
    }
  }

  default <V> V valueOrDefault(V defaultValue) {
    if (isLoaded()) {
      return getValue();
    } else {
      return defaultValue;
    }
  }

  default List<Object> valuesOrDefault(List<Object> defaultValue) {
    if (isLoaded()) {
      return getValues();
    } else {
      return defaultValue;
    }
  }

  default void withEntry(Consumer<CodeTableEntry> action) {
    action.accept(this);
  }

  default void withIdentifier(Consumer<Identifier> action) {
    final Identifier identifier = getIdentifier();
    action.accept(identifier);
  }

  default <V> void withValue(Consumer<V> action) {
    final V value = getValue();
    action.accept(value);
  }

  default <V> void withValues(Consumer<List<Object>> action) {
    final List<Object> values = getValues();
    action.accept(values);
  }
}
