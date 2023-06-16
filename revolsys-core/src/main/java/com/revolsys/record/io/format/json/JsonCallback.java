package com.revolsys.record.io.format.json;

import java.math.BigDecimal;

public interface JsonCallback {
  default void booleanValue(final boolean value) {

  }

  default void comma() {
  }

  default void endArray() {
  }

  default void endDocument() {
  }

  default void endObject() {
  }

  default void label(final String label) {
  }

  default void nullValue() {
  }

  default void number(final BigDecimal number) {
  }

  default void startArray() {
  }

  default void startDocument() {
  }

  default void startObject() {
  }

  default void string(final String string) {
  }

}
