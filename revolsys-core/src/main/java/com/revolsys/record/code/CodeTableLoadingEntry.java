package com.revolsys.record.code;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.revolsys.parallel.process.Process;
import com.revolsys.util.Debug;

class CodeTableLoadingEntry {

  private final AbstractLoadingCodeTable codeTable;

  private final long expiry = System.currentTimeMillis() + 60 * 1000;

  private final Object value;

  private final List<Consumer<CodeTableEntry>> loadingCallbacks = new ArrayList<>();

  private final Future<?> subscription;

  public CodeTableLoadingEntry(final AbstractLoadingCodeTable codeTable, final Object value) {
    this.codeTable = codeTable;
    this.value = value;
    this.subscription = Process.EXECUTOR.submit(() -> {
      this.codeTable.loadValueDo(this.value);
      final var data = this.codeTable.removeLoadingEntry(this);
      final var entry = data.getEntry(this.value);
      fireCallbacks(entry);
    });
  }

  public void addCallback(final Consumer<CodeTableEntry> callback) {
    if (callback != null) {
      this.loadingCallbacks.add(callback);
    } else {
      Debug.noOp();
    }
  }

  public void cancel() {
    this.subscription.cancel(true);
    fireCallbacks(null);
  }

  public void fireCallbacks(final CodeTableEntry entry) {
    for (final var callback : this.loadingCallbacks) {
      Process.EXECUTOR.execute(() -> callback.accept(entry));
    }

  }

  public Object getValue() {
    return this.value;
  }

  public boolean isExpired() {
    return this.expiry < System.currentTimeMillis();
  }

}
