package com.revolsys.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeometry.common.data.identifier.Identifier;
import org.jeometry.common.exception.Exceptions;
import org.jeometry.common.io.PathName;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.revolsys.io.IoConstants;
import com.revolsys.io.IoFactory;
import com.revolsys.record.Record;
import com.revolsys.record.io.RecordReader;
import com.revolsys.record.io.RecordWriterFactory;
import com.revolsys.record.io.format.csv.Csv;
import com.revolsys.record.io.format.csv.CsvRecordWriter;
import com.revolsys.record.io.format.json.Json;
import com.revolsys.record.io.format.json.JsonObject;
import com.revolsys.record.io.format.json.JsonParser;
import com.revolsys.record.io.format.json.JsonRecordWriter;
import com.revolsys.record.io.format.json.JsonWriter;
import com.revolsys.record.query.CollectionValue;
import com.revolsys.record.query.Q;
import com.revolsys.record.query.Query;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.TableRecordStoreConnection;
import com.revolsys.transaction.Transaction;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.Property;

public class AbstractRecordRestController {

  private static final String UTF_8 = StandardCharsets.UTF_8.toString();

  protected final PathName tablePath;

  protected final String typeName;

  public AbstractRecordRestController(final PathName tablePath) {
    this.tablePath = tablePath;
    this.typeName = tablePath.getName();
  }

  protected void handleGetRecord(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, final Query query)
    throws IOException {
    final int offset = HttpServletUtils.getIntParameter(request, "$skip", 0);
    if (offset > 0) {
      query.setOffset(offset);
    }

    int limit = HttpServletUtils.getIntParameter(request, "$skip", query.getLimit());
    if (limit > 0) {
      limit = Math.min(limit, query.getLimit());
      query.setLimit(limit);
    }

    responseRecordJson(connection, request, response, query);
  }

  protected void handleGetRecords(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, final Query query)
    throws IOException {
    final int offset = HttpServletUtils.getIntParameter(request, "$skip", 0);
    if (offset > 0) {
      query.setOffset(offset);
    }

    int limit = HttpServletUtils.getIntParameter(request, "$limit", query.getLimit());
    if (limit > 0) {
      limit = Math.min(limit, query.getLimit());
      query.setLimit(limit);
    }
    final boolean returnCount = HttpServletUtils.getBooleanParameter(request, "$count");
    try (
      Transaction transaction = connection.newTransaction();
      final RecordReader records = connection.getRecordReader(query)) {
      Long count = null;
      if (returnCount) {
        count = connection.getRecordCount(query);
      }
      responseRecords(connection, request, response, records, count);
    }
  }

  protected void handleInsertRecord(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final JsonObject json = readJsonBody(request);
    final Record record = connection.newRecord(this.tablePath, json);
    handleInsertRecordDo(connection, request, response, record);
  }

  protected void handleInsertRecordDo(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, Record record)
    throws IOException {
    record = connection.insertRecord(record);
    responseRecordJson(connection, request, response, record);
  }

  protected void handleUpdateRecordDo(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, final Identifier id,
    final Consumer<Record> updateAction) throws IOException {
    final Record record = connection.updateRecord(this.tablePath, id, updateAction);
    responseRecordJson(connection, request, response, record);
  }

  protected void handleUpdateRecordDo(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, final Identifier id,
    final JsonObject values) throws IOException {
    final Record record = connection.updateRecord(this.tablePath, id, values);
    responseRecordJson(connection, request, response, record);
  }

  protected boolean isUpdateable(final TableRecordStoreConnection connection, final Identifier id) {
    return true;
  }

  protected Query newQuery(final TableRecordStoreConnection connection,
    final HttpServletRequest request) {
    return newQuery(connection, request, Integer.MAX_VALUE);
  }

  protected Query newQuery(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final int maxRecords) {
    final Query query = connection.newQuery(this.tablePath);
    final String select = request.getParameter("$select");
    newQuerySelect(connection, query, select);

    final int offset = HttpServletUtils.getIntParameter(request, "$skip", 0);
    if (offset < 0) {
      throw new IllegalArgumentException("$skip must be > 0: " + offset);
    }
    if (offset > 0) {
      query.setOffset(offset);
    }

    int limit = HttpServletUtils.getIntParameter(request, "$top", maxRecords);
    if (limit <= 0) {
      throw new IllegalArgumentException("$top must be > 1: " + limit);
    }
    limit = Math.min(limit, maxRecords);
    query.setLimit(limit);
    newQueryFilterCondition(connection, query, request);
    final String orderBy = request.getParameter("$orderby");
    newQueryOrderBy(connection, query, orderBy);
    return query;
  }

  protected void newQueryFilterCondition(final Query query, final HttpServletRequest request,
    String filterFieldName, final Object value) {

    if (filterFieldName != null) {
      if (filterFieldName.startsWith("t.")) {
        filterFieldName = filterFieldName.substring(2);
      }
      if ("null".equals(value)) {
        query.and(filterFieldName, Q.IS_NULL);
      } else {
        if (value instanceof List<?>) {
          final List<?> list = (List<?>)value;
          final CollectionValue collection = new CollectionValue(list);
          query.and(filterFieldName, Q.IN, collection);
        } else {
          final Object conditionValue = value;
          if (value instanceof String && ((String)value).indexOf('%') == -1) {
            query.and(filterFieldName, Q.EQUAL, conditionValue);
          } else {
            query.and(filterFieldName, Q.ILIKE, conditionValue);
          }
        }
      }
    }
  }

  private void newQueryFilterCondition(final TableRecordStoreConnection connection,
    final Query query, final HttpServletRequest request) {
    final String[] filterFieldNames = request.getParameterValues("filterFieldName");
    final String[] filterValues = request.getParameterValues("filterValue");
    if (filterFieldNames != null) {
      for (int i = 0; i < filterFieldNames.length; i++) {
        final String filterFieldName = filterFieldNames[i];
        if (Property.hasValue(filterFieldName) && i < filterValues.length) {
          final String filterValue = filterValues[i];
          Object value = filterValue;
          if (filterValue.charAt(0) == '[') {
            value = JsonParser.read(filterValue);
          }
          newQueryFilterCondition(query, request, filterFieldName, value);
        }
      }
    }
    final String search = request.getParameter("$search");
    if (search != null && search.trim().length() > 0) {
      newQueryFilterConditionSearch(connection, query, search);
    }
  }

  protected void newQueryFilterConditionSearch(final TableRecordStoreConnection connection,
    final Query query, final String search) {
  }

  private void newQueryOrderBy(final TableRecordStoreConnection connection, final Query query,
    final String orderBy) {
    if (Property.hasValue(orderBy)) {
      for (String orderClause : orderBy.split(",")) {
        orderClause = orderClause.trim();
        String fieldName;
        boolean ascending = true;
        final int spaceIndex = orderClause.indexOf(' ');
        if (spaceIndex == -1) {
          fieldName = orderClause;
        } else {
          fieldName = orderClause.substring(0, spaceIndex);
          if ("desc".equalsIgnoreCase(orderClause.substring(spaceIndex + 1))) {
            ascending = false;
          }
        }
        query.addOrderBy(fieldName, ascending);
      }
    }
    connection.addDefaultSortOrder(this.tablePath, query);
  }

  private void newQuerySelect(final TableRecordStoreConnection connection, final Query query,
    final String select) {
    if (Property.hasValue(select)) {
      for (String selectItem : select.split(",")) {
        selectItem = selectItem.trim();
        query.select(selectItem);
      }
    }
  }

  public JsonObject readJsonBody(final HttpServletRequest request) throws IOException {
    final JsonObject json;
    try (
      Reader reader = request.getReader()) {
      json = JsonParser.read(reader);
    }
    return json;
  }

  protected void responseJson(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response,
    final JsonObject jsonObject) throws IOException {
    setContentTypeJson(response);
    response.setStatus(200);
    try (
      PrintWriter writer = response.getWriter();
      JsonWriter jsonWriter = new JsonWriter(writer);) {
      jsonWriter.write(jsonObject);
    }
  }

  protected void responseRecordJson(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, final Query query)
    throws IOException {
    final Record record = connection.getRecord(query);
    responseRecordJson(connection, request, response, record);
  }

  protected void responseRecordJson(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, final Record record)
    throws IOException {
    if (record == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } else {
      setContentTypeJson(response);
      response.setStatus(200);
      final RecordDefinition recordDefinition = record.getRecordDefinition();
      try (
        PrintWriter writer = response.getWriter();
        JsonRecordWriter jsonWriter = new JsonRecordWriter(recordDefinition, writer);) {
        jsonWriter.setProperty(IoConstants.SINGLE_OBJECT_PROPERTY, true);
        jsonWriter.write(record);
      }
    }
  }

  protected void responseRecords(final TableRecordStoreConnection connection,
    final HttpServletRequest request, final HttpServletResponse response, final RecordReader reader,
    final Long count) throws IOException {
    if ("csv".equals(request.getParameter("format"))) {
      responseRecordsCsv(response, reader);
    } else {
      responseRecordsJson(connection, response, reader, count);
    }
  }

  protected void responseRecordsCsv(final HttpServletResponse response, final RecordReader reader)
    throws IOException {
    response.setHeader("Content-Disposition", "attachment; filename=Export.csv");
    setContentTypeText(response, Csv.MIME_TYPE);
    response.setStatus(200);
    final Csv csv = (Csv)IoFactory.factoryByFileExtension(RecordWriterFactory.class, "csv");
    try (
      PrintWriter writer = response.getWriter();
      CsvRecordWriter recordWriter = csv.newRecordWriter(reader, writer)) {
      recordWriter.setMaxFieldLength(32000);
      recordWriter.writeAll(reader);
    }
  }

  public void responseRecordsJson(final TableRecordStoreConnection connection,
    final HttpServletResponse response, final Query query, final Long count) throws IOException {
    try (
      Transaction transaction = connection.newTransaction();
      final RecordReader records = connection.getRecordReader(query)) {
      responseRecordsJson(connection, response, records, count);
    } catch (final Exception e) {
      throw Exceptions.wrap(query.toString(), e);
    }
  }

  protected void responseRecordsJson(final TableRecordStoreConnection connection,
    final HttpServletResponse response, final RecordReader reader, final Long count)
    throws IOException {
    reader.open();
    setContentTypeJson(response);
    response.setStatus(200);
    try (
      PrintWriter writer = response.getWriter();
      JsonRecordWriter jsonWriter = new JsonRecordWriter(reader, writer);) {
      if (count != null) {
        jsonWriter.setHeader(JsonObject.hash("@odata.count", count));
      }
      jsonWriter.setItemsPropertyName("value");
      jsonWriter.writeAll(reader);
    }
  }

  public void setContentTypeJson(final HttpServletResponse response) {
    setContentTypeText(response, Json.MIME_TYPE);
  }

  public void setContentTypeText(final HttpServletResponse response, final String contentType) {
    response.setCharacterEncoding(UTF_8);
    response.setContentType(contentType);
  }

}
