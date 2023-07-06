package com.revolsys.net.http.exception;

public class AuthenticationException extends SecurityException {

  private static final long serialVersionUID = 1L;

  public AuthenticationException(final String message) {
    super(message);
  }

  public AuthenticationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public AuthenticationException(final Throwable exception) {
    super(exception);
  }

}
