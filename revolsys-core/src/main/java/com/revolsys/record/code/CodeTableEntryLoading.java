package com.revolsys.record.code;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.jeometry.common.data.identifier.Identifier;
import org.jeometry.common.exception.Exceptions;
import org.jeometry.common.logging.Logs;

import com.revolsys.parallel.process.Process;
import com.revolsys.util.Pair;

class CodeTableEntryLoading implements CodeTableEntry {

  private final AbstractLoadingCodeTable codeTable;

  private final Object value;

  private final List<Pair<Consumer<CodeTableEntry>, Long>> loadingCallbacks = new ArrayList<>();

  private final Future<CodeTableEntry> subscription;

  private CodeTableEntry entry;

  public CodeTableEntryLoading(final AbstractLoadingCodeTable codeTable, final Object value) {
    this.codeTable = codeTable;
    this.value = value;
    this.subscription = Process.EXECUTOR.submit(() -> {
      this.codeTable.loadValueDo(this.value);
      this.entry = this.codeTable.entryLoaded(this);
      fireCallbacks(this.entry);
      return this.entry;
    });
  }

  public CodeTableEntryLoading(final AbstractLoadingCodeTable codeTable, final Object value,
    Runnable loader) {
    this.codeTable = codeTable;
    this.value = value;
    this.subscription = Process.EXECUTOR.submit(() -> {
      loader.run();
      this.entry = this.codeTable.entryLoaded(this);
      fireCallbacks(this.entry);
      return this.entry;
    });
  }

  @Override
  public CodeTableEntry addCallback(final Consumer<CodeTableEntry> callback) {
    if (callback != null) {
      this.codeTable.lock.lock();
      CodeTableEntry entry;
      try {
        if (this.entry == null) {
          final long expiry = System.currentTimeMillis() + 60 * 1000;
          this.loadingCallbacks.add(Pair.newPair(callback, expiry));
          return this;
        } else {
          entry = this.entry;
        }
      } finally {
        this.codeTable.lock.unlock();
      }
      callback.accept(entry);
    }
    return this;
  }

  void fireCallbacks(final CodeTableEntry entry) {
    for (final var e : this.loadingCallbacks) {
      final var expiry = e.getValue2();
      if (expiry > System.currentTimeMillis()) {
        Process.EXECUTOR.execute(() -> e.getValue1().accept(entry));
      }
    }

  }

  public CodeTableEntry getEntry() {
    if (this.entry == null) {
      if (SwingUtilities.isEventDispatchThread()) {
        Logs.error(this, "Cannot load from code table without callback in swing thread");
      }
      try {
        return this.subscription.get();
      } catch (final InterruptedException e) {
        Exceptions.throwUncheckedException(e);
      } catch (final ExecutionException e) {
        Exceptions.throwCauseException(e);
      }
    }
    return this.entry;
  }

  @Override
  public Identifier getIdentifier() {
    return getEntry().getIdentifier();
  }

  Object getLoadingValue() {
    return this.value;
  }

  @Override
  public <V> V getValue() {
    return getEntry().getValue();
  }

  @Override
  public List<Object> getValues() {
    return getEntry().getValues();
  }

  @Override
  public boolean isEmpty() {
    if (this.entry == null) {
      return false;
    } else {
      return this.entry.isEmpty();
    }
  }

  @Override
  public boolean isLoaded() {
    return this.entry != null;
  }

  @Override
  public void withEntry(Consumer<CodeTableEntry> action) {
    addCallback(e -> withEntry(action));
  }

  @Override
  public void withIdentifier(Consumer<Identifier> action) {
    addCallback(e -> withIdentifier(action));
  }

  @Override
  public <V> void withValue(Consumer<V> action) {
    addCallback(e -> withValue(action));
  }

  @Override
  public <V> void withValues(Consumer<List<Object>> action) {
    addCallback(e -> withValues(action));
  }

}
