package com.revolsys.collection.iterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.revolsys.io.Reader;

public class ReaderMerger<V> {

  private final Comparator<V> comparator;

  private Function<V, V> added = v -> v;

  private BiFunction<V, V, V> matched = (v1, v2) -> v2;

  private Function<V, V> removed = v -> v;

  private final Reader<V> source;

  private final Reader<V> target;

  public ReaderMerger(final Reader<V> source, final Reader<V> target,
    final Comparator<V> comparator) {
    this.source = source;
    this.target = target;
    this.comparator = comparator;
  }

  /**
   * Action to perform when the value is in the source but not the target.
   *
   * @param updateAction The action;
   * @return this
   */
  public ReaderMerger<V> added(final Function<V, V> added) {
    this.added = added;
    return this;
  }

  /**
   * Action to perform when the value is in both the source and target.
   *
   * @param matched
  * @return this
    */
  public ReaderMerger<V> matched(final BiFunction<V, V, V> matched) {
    this.matched = matched;
    return this;
  }

  public void processRemaining(final Iterator<V> iterator, V value, final Function<V, V> action) {
    if (value == null && iterator.hasNext()) {
      value = iterator.next();
    }
    while (value != null) {
      action.apply(value);
      if (iterator.hasNext()) {
        value = iterator.next();
      } else {
        value = null;
      }
    }
  }

  /**
   * Action to perform when the value is in the target but not the source.
   *
   * @param updateAction The action;
   * @return this
   */
  public ReaderMerger<V> removed(final Function<V, V> removed) {
    this.removed = removed;
    return this;
  }

  public void run() {
    try (
      Reader<V> sourceReader = this.source;
      Reader<V> targetReader = this.target) {

      final Iterator<V> sourceIterator = sourceReader.iterator();
      final Iterator<V> targetIterator = targetReader.iterator();

      V sourceValue = null;
      V targetValue = null;

      while ((targetIterator.hasNext() || targetValue != null)
        && (sourceIterator.hasNext() || sourceValue != null)) {
        if (sourceValue == null) {
          sourceValue = sourceIterator.next();
        }
        if (targetValue == null) {
          targetValue = targetIterator.next();
        }

        final int compare = this.comparator.compare(sourceValue, targetValue);
        if (compare > 0) {
          this.added.apply(sourceValue);
          sourceValue = null;
        } else if (compare < 0) {
          this.removed.apply(targetValue);
          targetValue = null;
        } else {
          this.matched.apply(sourceValue, targetValue);
          sourceValue = null;
          targetValue = null;
        }
      }
      processRemaining(sourceIterator, sourceValue, this.added);
      processRemaining(targetIterator, targetValue, this.removed);
    }
  }

  @Override
  public String toString() {
    return this.source + "\n" + this.target;
  }
}
