package com.revolsys.io.page;

import java.util.ArrayList;
import java.util.List;

public class DataPagePageValueManager<T> implements PageValueManager<T> {

  public static <T> PageValueManager<T> create(
    final PageManager pageManager,
    final PageValueManager<T> valueSerializer) {
    return new DataPagePageValueManager<T>(pageManager, valueSerializer);
  }

  private final PageManager pageManager;

  private final PageValueManager<T> valueSerializer;

  public DataPagePageValueManager(final PageManager pageManager,
    final PageValueManager<T> valueSerializer) {
    this.pageManager = pageManager;
    this.valueSerializer = valueSerializer;
  }

  public <V extends T> V readFromByteArray(final byte[] bytes) {
    throw new UnsupportedOperationException(
      "Cannot read data page from a byte array");
  }

  public <V extends T> V readFromPage(final Page page) {
    final int pageIndex = page.readInt();
    Page dataPage = pageManager.getPage(pageIndex);
    dataPage.setOffset(0);
    byte pageType = dataPage.readByte();
    List<byte[]> pageBytes = new ArrayList<byte[]>();
    int size = 0;
    while (pageType == BPlusTree.EXTENDED) {
      int numBytes = dataPage.readShort() - 7;
      int nextPageIndex = dataPage.readInt();
      byte[] bytes = dataPage.readBytes(numBytes);
      pageBytes.add(bytes);
      size += bytes.length;
      dataPage = pageManager.getPage(nextPageIndex);
      dataPage.setOffset(0);
      pageType = dataPage.readByte();
    }
    if (pageType == BPlusTree.DATA) {
      final int numBytes = dataPage.readShort() - 3;
      final byte[] bytes = dataPage.readBytes(numBytes);
      pageBytes.add(bytes);
      size += bytes.length;

    } else {
      throw new IllegalArgumentException("Expecting a data page "
        + BPlusTree.DATA + " not " + pageType);
    }
    byte[] valueBytes = new byte[size];
    int offset = 0;
    for (byte[] bytes : pageBytes) {
      System.arraycopy(bytes, 0, valueBytes, offset, bytes.length);
      offset += bytes.length;
    }
    return valueSerializer.readFromByteArray(valueBytes);
  }

  public <V extends T> V removeFromPage(Page page) {
    final int pageIndex = page.readInt();
    Page dataPage = pageManager.getPage(pageIndex);
    dataPage.setOffset(0);
    byte pageType = dataPage.readByte();
    List<byte[]> pageBytes = new ArrayList<byte[]>();
    int size = 0;
    while (pageType == BPlusTree.EXTENDED) {
      int numBytes = dataPage.readShort() - 7;
      int nextPageIndex = dataPage.readInt();
      byte[] bytes = dataPage.readBytes(numBytes);
      pageBytes.add(bytes);
      size += bytes.length;
      pageManager.removePage(dataPage);
      dataPage = pageManager.getPage(nextPageIndex);
      dataPage.setOffset(0);
      pageType = dataPage.readByte();
    }
    if (pageType == BPlusTree.DATA) {
      final int numBytes = dataPage.readShort() - 3;
      final byte[] bytes = dataPage.readBytes(numBytes);
      pageBytes.add(bytes);
      size += bytes.length;
      pageManager.removePage(dataPage);
    } else {
      throw new IllegalArgumentException("Expecting a data page "
        + BPlusTree.DATA + " not " + pageType);
    }
    byte[] valueBytes = new byte[size];
    int offset = 0;
    for (byte[] bytes : pageBytes) {
      System.arraycopy(bytes, 0, valueBytes, offset, bytes.length);
      offset += bytes.length;
    }
    return valueSerializer.readFromByteArray(valueBytes);
  }

  public byte[] writeToByteArray(final T value) {
    final byte[] valueBytes = valueSerializer.writeToByteArray(value);

    int offset = 0;
    final int pageSize = pageManager.getPageSize();
    Page page = pageManager.createPage();
    final int pageIndex = page.getIndex();
    while (valueBytes.length + 3 > offset + pageSize) {
      Page nextPage = pageManager.createPage();
      BPlusTree.writePageHeader(page, BPlusTree.EXTENDED);
      page.writeInt(nextPage.getIndex());
      page.writeBytes(valueBytes, offset, pageSize - 7);
      BPlusTree.setNumBytes(page);
      page.flush();
      page = nextPage;
      offset += pageSize - 7;
    }

    BPlusTree.writePageHeader(page, BPlusTree.DATA);
    page.writeBytes(valueBytes, offset, valueBytes.length - offset);
    BPlusTree.setNumBytes(page);
    page.flush();

    return MethodPageValueManager.INT.writeToByteArray(pageIndex);
  }

}
