package com.revolsys.net.http.exception;

public class AuthorizationException extends SecurityException {

  private static final long serialVersionUID = 1L;

  public AuthorizationException(final String message) {
    super(message);
  }

  public AuthorizationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public AuthorizationException(final Throwable exception) {
    super(exception);
  }

}
