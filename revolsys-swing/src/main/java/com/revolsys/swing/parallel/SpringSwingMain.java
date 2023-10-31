package com.revolsys.swing.parallel;

import java.awt.Image;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jeometry.common.exception.Exceptions;
import org.jeometry.common.logging.Logs;
import org.springframework.beans.factory.InitializingBean;

import com.revolsys.log.LogbackUtil;
import com.revolsys.swing.desktop.DesktopInitializer;
import com.revolsys.swing.logging.ListLoggingAppender;
import com.revolsys.swing.logging.LoggingEventPanel;
import com.revolsys.util.Property;
import com.revolsys.util.Strings;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Appender;

public class SpringSwingMain implements UncaughtExceptionHandler, InitializingBean {

  protected Set<File> initialFiles = new LinkedHashSet<>();

  private String lookAndFeelName;

  private final String name;

  public SpringSwingMain(final String name) {
    this.name = name;
    Thread.setDefaultUncaughtExceptionHandler(this);
  }

  @Override
  public void afterPropertiesSet() {
    new Thread(() -> {
      try {
        if (swingBefore()) {
          SwingUtilities.invokeAndWait(() -> {
            swingInit();
            swingAfter();
          });
          if (appBefore()) {
            SwingUtilities.invokeAndWait(() -> {
              appSwingInit();
            });
          }
        }
      } catch (final InvocationTargetException e) {
        Logs.error(this, e.getCause());
      } catch (final InterruptedException e) {
      } catch (final RuntimeException e) {
        Logs.error(this, e.getCause());
      }
    }).start();
  }

  protected boolean appBefore() {
    return true;
  }

  protected void appSwingInit() {
  }

  public void logError(final Throwable e) {
    try {
      final Instant timestamp = Instant.now();
      final String threadName = Thread.currentThread().getName();
      final String stackTrace = e.getMessage() + "\n" + Strings.toString("\n", e.getStackTrace());
      LoggingEventPanel.showDialog(timestamp, Level.ERROR, getClass().getName(),
        "Unable to start application:" + e.getMessage(), threadName, stackTrace);
    } finally {
      Logs.error(this, "Unable to start application " + this.name, e);
    }
  }

  public void setLookAndFeelName(final String lookAndFeelName) {
    this.lookAndFeelName = lookAndFeelName;
  }

  protected void setMacDockIcon(final Image image) {
    try {
      DesktopInitializer.initialize(image, this.initialFiles);
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  protected void swingAfter() {
  }

  protected boolean swingBefore() {
    return true;
  }

  private void swingInit() {
    try {
      boolean lookSet = false;
      if (Property.hasValue(this.lookAndFeelName)) {
        final LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
        for (final LookAndFeelInfo lookAndFeelInfo : installedLookAndFeels) {
          final String name = lookAndFeelInfo.getName();
          if (this.lookAndFeelName.equals(name)) {
            try {
              final String className = lookAndFeelInfo.getClassName();
              UIManager.setLookAndFeel(className);
              lookSet = true;
            } catch (final Throwable e) {
            }
          }
        }
      }
      if (!lookSet) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);
      ToolTipManager.sharedInstance().setInitialDelay(100);
    } catch (final Exception e) {
      Exceptions.throwCauseException(e);
    }
  }

  @Override
  public void uncaughtException(final Thread t, final Throwable e) {
    final Class<? extends SpringSwingMain> logClass = getClass();
    String message = e.getMessage();
    if (!Property.hasValue(message)) {
      if (e instanceof NullPointerException) {
        message = "Null pointer";
      } else {
        message = "Unknown error";
      }
    }
    Logs.error(logClass, message, e);
    final Logger rootLogger = LogbackUtil.getRootLogger();
    for (final Iterator<Appender<ILoggingEvent>> iterator = rootLogger
      .iteratorForAppenders(); iterator.hasNext();) {
      final Appender<ILoggingEvent> appender = iterator.next();
      if (appender instanceof ListLoggingAppender) {
        return;
      }
    }
    final Logger logger = LogbackUtil.getLogger(logClass);
    final String name = logger.getClass().getName();
    final LoggingEvent event = new LoggingEvent();
    event.setLoggerName(name);
    event.setLevel(Level.INFO);
    event.setMessage(message.toString());
    event.setThrowableProxy(new ThrowableProxy(e));

    LoggingEventPanel.showDialog(event);
  }
}
