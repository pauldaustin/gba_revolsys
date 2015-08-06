package com.revolsys.spring.resource;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamResource extends AbstractResource {

  private String filename;

  private long length = -1;

  private final InputStream inputStream;

  private final String description;

  private boolean read = false;

  /**
   * Create a new InputStreamResource.
   * @param inputStream the InputStream to use
   */
  public InputStreamResource(final InputStream inputStream) {
    this(inputStream, "resource loaded through InputStream");
  }

  /**
   * Create a new InputStreamResource.
   * @param inputStream the InputStream to use
   * @param description where the InputStream comes from
   */
  public InputStreamResource(final InputStream inputStream, final String description) {
    if (inputStream == null) {
      throw new IllegalArgumentException("InputStream must not be null");
    }
    this.inputStream = inputStream;
    this.description = description != null ? description : "";
  }

  public InputStreamResource(final String filename, final InputStream inputStream) {
    this(inputStream);
    this.filename = filename;
  }

  public InputStreamResource(final String filename, final InputStream inputStream,
    final long length) {
    this(inputStream);
    this.filename = filename;
    this.length = length;
  }

  public InputStreamResource(final String filename, final InputStream inputStream,
    final String description) {
    this(inputStream, description);
    this.filename = filename;
  }

  @Override
  public long contentLength() throws IOException {
    if (this.length >= 0) {
      return this.length;
    } else {
      return super.contentLength();
    }
  }

  @Override
  public Resource createRelative(final String relativePath) {
    return null;
  }

  /**
   * This implementation compares the underlying InputStream.
   */
  @Override
  public boolean equals(final Object obj) {
    return obj == this || obj instanceof InputStreamResource
      && ((InputStreamResource)obj).inputStream.equals(this.inputStream);
  }

  /**
   * This implementation always returns <code>true</code>.
   */
  @Override
  public boolean exists() {
    return true;
  }

  /**
   * This implementation returns the passed-in description, if any.
   */
  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String getFilename() throws IllegalStateException {
    return this.filename;
  }

  /**
   * This implementation throws IllegalStateException if attempting to
   * read the underlying stream multiple times.
   */
  @Override
  public InputStream getInputStream() throws IOException, IllegalStateException {
    if (this.read) {
      throw new IllegalStateException("InputStream has already been read - "
        + "do not use InputStreamResource if a stream needs to be read multiple times");
    }
    this.read = true;
    return this.inputStream;
  }

  /**
   * This implementation returns the hash code of the underlying InputStream.
   */
  @Override
  public int hashCode() {
    return this.inputStream.hashCode();
  }

  /**
   * This implementation always returns <code>true</code>.
   */
  @Override
  public boolean isOpen() {
    return true;
  }
}
