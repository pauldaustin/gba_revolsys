package com.revolsys.web.listener;import java.beans.Introspector;import java.io.IOException;import java.lang.management.ManagementFactory;import java.net.MalformedURLException;import java.net.URL;import java.sql.Driver;import java.sql.DriverManager;import java.util.Enumeration;import javax.management.InstanceNotFoundException;import javax.management.ObjectName;import jakarta.servlet.ServletContextEvent;import jakarta.servlet.ServletContextListener;import org.jeometry.common.logging.Logs;public class DriverManagerCleanupListener implements ServletContextListener {  public static void cleanupDrivers() {    final ClassLoader classLoader = DriverManagerCleanupListener.class.getClassLoader();    try {      final Enumeration<Driver> drivers = DriverManager.getDrivers();      while (drivers.hasMoreElements()) {        try {          final Driver driver = drivers.nextElement();          final Class<? extends Driver> driverClass = driver.getClass();          if (driverClass.getClassLoader() == classLoader) {            DriverManager.deregisterDriver(driver);          }          try {            // Cleanup Oracle MBean            final ObjectName objectname = new ObjectName(              "com.oracle.jdbc:type=diagnosability,name=" + classLoader.getClass().getName() + "@"                + Integer.toHexString(classLoader.hashCode()));            ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectname);          } catch (final InstanceNotFoundException e) {          }        } catch (final Throwable e) {          Logs.error(DriverManagerCleanupListener.class, "Failed to cleanup ClassLoader for webapp",            e);        }      }    } catch (final Throwable e) {      Logs.error(DriverManagerCleanupListener.class, "Failed to cleanup ClassLoader for webapp", e);    } finally {      Introspector.flushCaches();    }  }  @Override  public void contextDestroyed(final ServletContextEvent event) {    cleanupDrivers();  }  @Override  public void contextInitialized(final ServletContextEvent event) {    try {      new URL("file:///").openConnection().setDefaultUseCaches(false);    } catch (final MalformedURLException e) {      // TODO Auto-generated catch block      e.printStackTrace();    } catch (final IOException e) {      // TODO Auto-generated catch block      e.printStackTrace();    }  }}