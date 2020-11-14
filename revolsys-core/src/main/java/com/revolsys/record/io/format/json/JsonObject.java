package com.revolsys.record.io.format.json;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jeometry.common.exception.Exceptions;

import com.revolsys.collection.map.MapEx;
import com.revolsys.util.Property;

public interface JsonObject extends MapEx, JsonType {
  JsonObject EMPTY = new JsonObject() {
    @Override
    public JsonObject clone() {
      return this;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      return MapEx.EMPTY.entrySet();
    }

    @Override
    public boolean equals(final Object object) {
      if (object instanceof Map<?, ?>) {
        final Map<?, ?> map = (Map<?, ?>)object;
        return map.isEmpty();
      } else {
        return false;
      }
    }

    @Override
    public boolean equals(final Object object,
      final Collection<? extends CharSequence> excludeFieldNames) {
      return equals(object);
    }

    @Override
    public String toString() {
      return "{}";
    }
  };

  static JsonObject hash() {
    return new JsonObjectHash();
  }

  static JsonObject hash(final Map<? extends String, ? extends Object> m) {
    return new JsonObjectHash(m);
  }

  static JsonObject hash(final String key, final Object value) {
    return new JsonObjectHash(key, value);
  }

  static JsonObject newItems(final List<?> items) {
    return new JsonObjectHash("items", items);
  }

  static JsonObject tree() {
    return new JsonObjectTree();
  }

  static JsonObject tree(final Map<? extends String, ? extends Object> m) {
    return new JsonObjectTree(m);
  }

  static JsonObject tree(final String key, final Object value) {
    return new JsonObjectTree(key, value);
  }

  @Override
  default JsonObject add(final String key, final Object value) {
    MapEx.super.add(key, value);
    return this;
  }

  @Override
  default JsonObject addFieldValue(final String key, final Map<String, Object> source) {
    final Object value = source.get(key);
    if (value == null || containsKey(key)) {
      addValue(key, value);
    }
    return this;
  }

  @Override
  default JsonObject addFieldValue(final String key, final Map<String, Object> source,
    final String sourceKey) {
    final Object value = source.get(sourceKey);
    if (value != null || containsKey(key)) {
      addValue(key, value);
    }
    return this;
  }

  default JsonObject addNotEmpty(final String key, final Object value) {
    if (Property.hasValue(value)) {
      addValue(key, value);
    }
    return this;
  }

  @Override
  default JsonObject addValue(final String key, final Object value) {
    MapEx.super.addValue(key, value);
    return this;
  }

  default JsonObject addValueClone(final String key, Object value) {
    value = JsonType.toJsonClone(value);
    return addValue(key, value);
  }

  default JsonObject addValuesClone(final MapEx values) {
    for (final String name : values.keySet()) {
      Object value = values.getValue(name);
      if (value != null) {
        value = JsonType.toJsonClone(value);
      }
      addValue(name, value);
    }
    return this;
  }

  @Override
  default Appendable appendJson(final Appendable appendable) {
    try {
      appendable.append('{');
      boolean first = true;
      for (final String key : keySet()) {
        if (first) {
          first = false;
        } else {
          appendable.append(',');
        }
        final Object value = get(key);
        appendable.append('"');
        JsonAppender.appendCharacters(appendable, key);
        appendable.append("\":");
        JsonAppender.appendValue(appendable, value);
      }
      appendable.append('}');
      return appendable;
    } catch (final IOException e) {
      throw Exceptions.wrap(e);
    }
  }

  @Override
  default JsonObject asJson() {
    return (JsonObject)JsonType.super.asJson();
  }

  @Override
  JsonObject clone();

  default Object getByPath(final String[] names) {
    return getByPath(names, 0);
  }

  default Object getByPath(final String[] names, final int offset) {
    if (offset == names.length) {
      return this;
    } else if (offset < names.length) {
      final String name = names[offset];
      Object value;
      if ("$".equals(name)) {
        value = this;
      } else {
        value = getValue(name);
      }
      if (offset + 1 == names.length) {
        return value;
      } else if (value instanceof JsonObject) {
        final JsonObject object = (JsonObject)value;
        return object.getByPath(names, offset + 1);
      }
    }
    return null;
  }

  default JsonObject getJsonObject(final CharSequence name) {
    return getValue(name, Json.JSON_OBJECT);
  }

  default JsonObject getJsonObject(final CharSequence name, final JsonObject defaultValue) {
    final JsonObject value = getJsonObject(name);
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  @Override
  default JsonObject toJson() {
    return (JsonObject)JsonType.super.toJson();
  }

  default String toJsonString(final boolean indent) {
    return Json.toString(this, indent);
  }
}
