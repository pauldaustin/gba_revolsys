package com.revolsys.record.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.jeometry.common.exception.Exceptions;
import org.jeometry.common.logging.Logs;

import com.revolsys.io.BaseCloseable;
import com.revolsys.parallel.process.Process;

public abstract class AbstractLoadingCodeTable extends AbstractCodeTable
  implements BaseCloseable, Cloneable {

  private class LatchCallback implements Consumer<CodeTableEntry> {
    private final CountDownLatch latch = new CountDownLatch(1);

    private CodeTableEntry entry;

    @Override
    public void accept(final CodeTableEntry entry) {
      this.latch.countDown();
      this.entry = entry;
    }

    public CodeTableEntry getEntry() {
      try {
        this.latch.await();
      } catch (final InterruptedException e) {
        Exceptions.throwUncheckedException(e);
      }
      return this.entry;
    }
  }

  private final Map<Object, CodeTableLoadingEntry> loadingByValue = new HashMap<>();

  private boolean loadMissingCodes = true;

  private boolean loadAll = true;

  private final List<Consumer<CodeTableData>> loadingCallbacks = new ArrayList<>();

  private Future<CodeTableData> dataFuture;

  private final boolean loadingAll = false;

  public AbstractLoadingCodeTable() {
  }

  private void addLoadingCallback(final Consumer<CodeTableData> action) {
    this.lock.lock();
    try {
      this.loadingCallbacks.add(action);
    } finally {
      this.lock.unlock();
    }
  }

  protected void clearCaches() {
  }

  @Override
  public AbstractLoadingCodeTable clone() {
    return (AbstractLoadingCodeTable)super.clone();
  }

  @Override
  public CodeTableEntry getEntry(final Consumer<CodeTableEntry> callback, final Object idOrValue) {
    CodeTableEntry entry = super.getEntry(callback, idOrValue);
    if (entry == null) {
      if (callback == null) {
        if (SwingUtilities.isEventDispatchThread()) {
          Logs.error(this, "Cannot load from code table without callback in swing thread");
        } else {
          final var awaitCallback = new LatchCallback();
          loadValue(idOrValue, awaitCallback);
          entry = awaitCallback.getEntry();
        }
      } else {
        loadValue(idOrValue, callback);
      }
    }
    return entry;
  }

  @Override
  public boolean isLoadAll() {
    return this.loadAll;
  }

  @Override
  public boolean isLoaded() {
    return getData().isAllLoaded();
  }

  @Override
  public boolean isLoading() {
    this.lock.lock();
    try {
      if (this.dataFuture != null) {
        if (this.dataFuture.isDone()) {
          this.dataFuture = null;
        } else {
          return true;
        }
      }
      return false;
    } finally {
      this.lock.unlock();
    }
  }

  public boolean isLoadMissingCodes() {
    return this.loadMissingCodes;
  }

  protected abstract CodeTableData loadAll();

  private void loadValue(final Object value, final Consumer<CodeTableEntry> callback) {
    if (!isLoaded() && isLoadAll()) {
      if (callback != null) {
        addLoadingCallback((data) -> {
          CodeTableEntry entry = null;
          if (data != null) {
            entry = data.getEntry(value);
          }
          callback.accept(entry);
        });
      }
      refresh();
    } else if (isLoadMissingCodes()) {
      this.lock.lock();
      try {
        var loading = this.loadingByValue.get(value);
        if (loading == null || loading.isExpired()) {
          if (loading != null) {
            loading.cancel();
          }
          loading = new CodeTableLoadingEntry(this, value);
          this.loadingByValue.put(value, loading);
        }
        loading.addCallback(callback);
      } finally {
        this.lock.unlock();
      }
    } else {
      if (callback != null) {
        callback.accept(null);
      }
    }
  }

  protected abstract boolean loadValueDo(Object idOrValue);

  protected CodeTableData newData() {
    return new CodeTableData(this);
  }

  @Override
  public void refresh() {
    final var data = loadAll();
    if (data != null) {
      this.lock.lock();
      try {
        if (data.isAfter(this.data)) {
          data.setAllLoaded(true);
          this.data = data;
          clearCaches();
          for (final var callback : this.loadingCallbacks) {
            Process.EXECUTOR.execute(() -> callback.accept(data));
          }
          this.loadingCallbacks.clear();
        }
      } finally {
        this.lock.unlock();
      }
    }
  }

  @Override
  public boolean refreshIfNeeded() {
    if (isLoadAll() && !(isLoaded() || this.loadingAll)) {
      refresh();
      return true;
    } else {
      return false;
    }
  }

  public CodeTableData removeLoadingEntry(final CodeTableLoadingEntry codeTableLoadingEntry) {
    this.lock.lock();
    try {
      if (codeTableLoadingEntry == this.loadingByValue.get(codeTableLoadingEntry.getValue())) {
        this.loadingByValue.remove(codeTableLoadingEntry.getValue());
      }
      return getData();
    } finally {
      this.lock.unlock();
    }
  }

  public AbstractLoadingCodeTable setLoadAll(final boolean loadAll) {
    this.loadAll = loadAll;
    return this;
  }

  @Override
  public AbstractLoadingCodeTable setLoadMissingCodes(final boolean loadMissingCodes) {
    this.loadMissingCodes = loadMissingCodes;
    return this;
  }
}
