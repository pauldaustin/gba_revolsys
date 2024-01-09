package com.revolsys.record.code;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jeometry.common.exception.Exceptions;

import com.revolsys.io.BaseCloseable;
import com.revolsys.parallel.process.Process;

public abstract class AbstractLoadingCodeTable extends AbstractCodeTable
  implements BaseCloseable, Cloneable {

  private final Map<Object, CodeTableEntryLoading> loadingByValue = new HashMap<>();

  private boolean loadMissingCodes = true;

  private boolean loadAll = true;

  private final boolean loadingAll = false;

  private Future<CodeTableData> refreshSubscription;

  public AbstractLoadingCodeTable() {
  }

  public void clearCaches() {
    getData().clearCaches();
  }

  @Override
  public AbstractLoadingCodeTable clone() {
    return (AbstractLoadingCodeTable)super.clone();
  }

  CodeTableEntry entryLoaded(final CodeTableEntryLoading loadingEntry) {
    this.lock.lock();
    try {
      final Object loadingValue = loadingEntry.getLoadingValue();
      if (loadingEntry == this.loadingByValue.get(loadingValue)) {
        this.loadingByValue.remove(loadingValue);
      }
      return this.data.getEntry(loadingValue);
    } finally {
      this.lock.unlock();
    }
  }

  protected CodeTableData getDataAll() {
    refreshIfNeeded();
    return super.getData();
  }

  @Override
  public CodeTableEntry getEntry(final Object idOrValue) {
    final CodeTableEntry entry = super.getEntry(idOrValue);
    if (entry.isEmpty()) {
      return loadValue(idOrValue);
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
    final var subscription = this.refreshSubscription;
    if (subscription == null) {
      return false;
    } else {
      return !subscription.isDone();
    }
  }

  public boolean isLoadMissingCodes() {
    return this.loadMissingCodes;
  }

  protected abstract CodeTableData loadAll();

  private CodeTableEntry loadValue(final Object value) {
    this.lock.lock();
    try {
      if (!isLoaded() && isLoadAll()) {
        var loading = this.loadingByValue.get(value);
        if (loading == null) {
          final Runnable loader = this::refresh;
          loading = new CodeTableEntryLoading(this, value, loader);
          this.loadingByValue.put(value, loading);
        }
        return loading;

      } else if (isLoadMissingCodes()) {
        var loading = this.loadingByValue.get(value);
        if (loading == null) {
          final Runnable loader = () -> loadValueDo(value);
          loading = new CodeTableEntryLoading(this, value, loader);
          this.loadingByValue.put(value, loading);
        }
        return loading;
      } else {
        return CodeTableEntry.EMPTY;
      }
    } finally {
      this.lock.unlock();
    }
  }

  protected abstract boolean loadValueDo(Object idOrValue);

  @Override
  public void refresh() {
    Future<CodeTableData> subscription;
    this.lock.lock();
    try {
      if (this.refreshSubscription == null || this.refreshSubscription.isDone()) {
        this.refreshSubscription = subscription = Process.EXECUTOR.submit(this::refreshDo);
      } else {
        subscription = this.refreshSubscription;
      }
    } finally {
      this.lock.unlock();
    }
    try {
      subscription.get();
      this.lock.lock();
      try {
        this.refreshSubscription = null;
      } finally {
        this.lock.unlock();
      }
    } catch (final InterruptedException e) {
      Exceptions.throwUncheckedException(e);
    } catch (final ExecutionException e) {
      Exceptions.throwCauseException(e);
    }
  }

  protected CodeTableData refreshDo() {
    final var data = loadAll();
    if (data != null) {
      this.lock.lock();
      final var loadingByValue = new LinkedHashMap<>(this.loadingByValue);
      this.loadingByValue.clear();
      try {
        if (data.isAfter(this.data)) {
          data.setAllLoaded(true);
          this.data = data;
        }
      } finally {
        this.lock.unlock();
      }
      for (final var e : loadingByValue.entrySet()) {
        final var value = e.getKey();
        final var callback = e.getValue();
        Process.EXECUTOR.execute(() -> {
          final var entry = data.getEntry(value);
          callback.fireCallbacks(entry);
        });
      }
    }
    return this.data;
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
