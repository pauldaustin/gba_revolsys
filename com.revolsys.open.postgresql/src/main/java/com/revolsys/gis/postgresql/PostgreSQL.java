package com.revolsys.gis.postgresql;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.postgresql.Driver;

import com.revolsys.collection.map.Maps;
import com.revolsys.jdbc.io.JdbcDatabaseFactory;
import com.revolsys.jdbc.io.JdbcRecordStore;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordStore;
import com.revolsys.util.Property;
import com.revolsys.util.Strings;
import com.revolsys.util.UrlUtil;

public class PostgreSQL implements JdbcDatabaseFactory {
  private static final String REGEX_NAME = "\\p{IsAlphabetic}[\\p{IsAlphabetic}0-9_\\$]*";

  private static final Pattern PATTERN_URL = Pattern.compile("jdbc:postgresql:(?:" // Prefix
    + "(?://" // Optional Start Host and port
    + "([a-zA-Z-0-9][a-zA-Z-0-9\\.\\-]*)" // Host
    + "(?::(\\d+))?" // Optional port
    + "/)?" // separator
    + "(" + REGEX_NAME + ")" // Database Name
    + ")?(?:\\?(" + REGEX_NAME + "=.*(?:&" + REGEX_NAME + "=.*)*))?" // Parameters
  );

  private static final List<FieldDefinition> CONNECTION_FIELD_DEFINITIONS = Arrays.asList();

  @Override
  public List<FieldDefinition> getConnectionFieldDefinitions() {
    return CONNECTION_FIELD_DEFINITIONS;
  }

  @Override
  public String getDriverClassName() {
    return Driver.class.getName();
  }

  @Override
  public String getName() {
    return "PostgreSQL/PostGIS Database";
  }

  @Override
  public String getProductName() {
    return "PostgreSQL";
  }

  @Override
  public List<String> getRecordStoreFileExtensions() {
    return Collections.emptyList();
  }

  @Override
  public Class<? extends RecordStore> getRecordStoreInterfaceClass(
    final Map<String, ? extends Object> connectionProperties) {
    return JdbcRecordStore.class;
  }

  @Override
  public String getVendorName() {
    return "postgresql";
  }

  @Override
  public JdbcRecordStore newRecordStore(final DataSource dataSource) {
    return new PostgreSQLRecordStore(dataSource);
  }

  @Override
  public JdbcRecordStore newRecordStore(final Map<String, ? extends Object> connectionProperties) {
    return new PostgreSQLRecordStore(this, connectionProperties);
  }

  @Override
  public Map<String, Object> parseUrl(final String url) {
    if (url != null && url.startsWith("jdbc:postgresql")) {
      final Matcher hostMatcher = PATTERN_URL.matcher(url);
      final Map<String, Object> parameters = new LinkedHashMap<>();
      if (hostMatcher.matches()) {
        parameters.put("recordStoreType", getName());
        final Map<String, Object> urlParameters = UrlUtil.getQueryStringMap(hostMatcher.group(4));
        parameters.putAll(urlParameters);

        final String host = hostMatcher.group(1);
        parameters.put("host", Strings.lowerCase(host));
        final String port = hostMatcher.group(2);
        parameters.put("port", port);
        final String database = hostMatcher.group(3);
        parameters.put("database", database);
        parameters.put("namedConnection", null);
        return parameters;
      }
    }
    return Collections.emptyMap();
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public String toUrl(final Map<String, Object> urlParameters) {
    final StringBuilder url = new StringBuilder("jdbc:postgresql:");
    final String host = Maps.getString(urlParameters, "host");
    final Integer port = Maps.getInteger(urlParameters, "port");
    final String database = Maps.getString(urlParameters, "database");

    final boolean hasHost = Property.hasValue(host);
    final boolean hasPort = port != null;
    if (hasHost || hasPort) {
      url.append("//");
      if (hasHost) {
        url.append(host);
      }
      if (hasPort) {
        url.append(':');
        url.append(port);
      }
      url.append('/');
    }
    if (Property.hasValue(database)) {
      url.append(database);
    }
    return url.toString();
  }
}
