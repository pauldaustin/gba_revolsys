package com.revolsys.record.query;

import java.io.IOException;

import org.jeometry.common.exception.Exceptions;

public interface TableReferenceProxy {
  default void appendColumnPrefix(final Appendable string) {
    final String alias = getTableAlias();
    if (alias != null) {
      try {
        string.append('"');
        string.append(alias);
        string.append('"');
        string.append('.');
      } catch (final IOException e) {
        throw Exceptions.wrap(e);
      }
    }
  }

  default ColumnReference getColumn(final CharSequence name) {
    return getTableReference().getColumn(name);
  }

  default String getTableAlias() {
    final TableReference table = getTableReference();
    if (table == null) {
      return null;
    } else {
      return table.getTableAlias();
    }
  }

  TableReference getTableReference();
}
