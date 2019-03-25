package com.revolsys.gis.esri.gdb.file;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import com.revolsys.collection.ValueHolder;
import com.revolsys.collection.iterator.AbstractIterator;
import com.revolsys.datatype.DataType;
import com.revolsys.datatype.DataTypes;
import com.revolsys.esri.filegdb.jni.EsriFileGdb;
import com.revolsys.esri.filegdb.jni.Geodatabase;
import com.revolsys.esri.filegdb.jni.Row;
import com.revolsys.esri.filegdb.jni.Table;
import com.revolsys.esri.filegdb.jni.VectorOfWString;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.ClockDirection;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.gis.esri.gdb.file.capi.FileGdbDomainCodeTable;
import com.revolsys.gis.esri.gdb.file.capi.type.AbstractFileGdbFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.AreaFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.BinaryFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.DateFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.DoubleFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.FloatFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.GeometryFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.GlobalIdFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.GuidFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.IntegerFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.LengthFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.OidFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.ShortFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.StringFieldDefinition;
import com.revolsys.gis.esri.gdb.file.capi.type.XmlFieldDefinition;
import com.revolsys.identifier.Identifier;
import com.revolsys.identifier.SingleIdentifier;
import com.revolsys.io.BaseCloseable;
import com.revolsys.io.FileUtil;
import com.revolsys.io.PathName;
import com.revolsys.io.PathUtil;
import com.revolsys.io.Writer;
import com.revolsys.jdbc.JdbcUtils;
import com.revolsys.logging.Logs;
import com.revolsys.parallel.SingleThreadExecutor;
import com.revolsys.record.Record;
import com.revolsys.record.code.CodeTable;
import com.revolsys.record.io.format.esri.gdb.xml.EsriGeodatabaseXmlConstants;
import com.revolsys.record.io.format.esri.gdb.xml.model.DEFeatureClass;
import com.revolsys.record.io.format.esri.gdb.xml.model.DEFeatureDataset;
import com.revolsys.record.io.format.esri.gdb.xml.model.DETable;
import com.revolsys.record.io.format.esri.gdb.xml.model.Domain;
import com.revolsys.record.io.format.esri.gdb.xml.model.EsriGdbXmlParser;
import com.revolsys.record.io.format.esri.gdb.xml.model.EsriGdbXmlSerializer;
import com.revolsys.record.io.format.esri.gdb.xml.model.EsriXmlRecordDefinitionUtil;
import com.revolsys.record.io.format.esri.gdb.xml.model.Field;
import com.revolsys.record.io.format.esri.gdb.xml.model.Index;
import com.revolsys.record.io.format.esri.gdb.xml.model.SpatialReference;
import com.revolsys.record.io.format.esri.gdb.xml.model.enums.FieldType;
import com.revolsys.record.io.format.xml.XmlProcessor;
import com.revolsys.record.property.LengthFieldName;
import com.revolsys.record.query.AbstractMultiCondition;
import com.revolsys.record.query.BinaryCondition;
import com.revolsys.record.query.CollectionValue;
import com.revolsys.record.query.Column;
import com.revolsys.record.query.Condition;
import com.revolsys.record.query.ILike;
import com.revolsys.record.query.In;
import com.revolsys.record.query.LeftUnaryCondition;
import com.revolsys.record.query.Like;
import com.revolsys.record.query.Query;
import com.revolsys.record.query.QueryValue;
import com.revolsys.record.query.RightUnaryCondition;
import com.revolsys.record.query.SqlCondition;
import com.revolsys.record.query.Value;
import com.revolsys.record.query.functions.EnvelopeIntersects;
import com.revolsys.record.query.functions.WithinDistance;
import com.revolsys.record.schema.AbstractRecordStore;
import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.record.schema.RecordDefinition;
import com.revolsys.record.schema.RecordDefinitionImpl;
import com.revolsys.record.schema.RecordDefinitionProxy;
import com.revolsys.record.schema.RecordStoreSchema;
import com.revolsys.record.schema.RecordStoreSchemaElement;
import com.revolsys.util.CloseableValueHolder;
import com.revolsys.util.Dates;
import com.revolsys.util.Exceptions;
import com.revolsys.util.JavaBeanUtil;
import com.revolsys.util.Property;
import com.revolsys.util.StringBuilders;

public class FileGdbRecordStore extends AbstractRecordStore {
  private static final Map<FieldType, Constructor<? extends AbstractFileGdbFieldDefinition>> ESRI_FIELD_TYPE_FIELD_DEFINITION_MAP = new HashMap<>();

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\?");

  private static final SingleThreadExecutor TASK_EXECUTOR = new SingleThreadExecutor(
    "ESRI FGDB Create Thread");

  static {
    addFieldTypeConstructor(FieldType.esriFieldTypeInteger, IntegerFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeSmallInteger, ShortFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeDouble, DoubleFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeSingle, FloatFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeString, StringFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeDate, DateFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeGeometry, GeometryFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeOID, OidFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeBlob, BinaryFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeGlobalID, GlobalIdFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeGUID, GuidFieldDefinition.class);
    addFieldTypeConstructor(FieldType.esriFieldTypeXML, XmlFieldDefinition.class);
  }

  private static void addFieldTypeConstructor(final FieldType fieldType,
    final Class<? extends AbstractFileGdbFieldDefinition> fieldClass) {
    try {
      final Constructor<? extends AbstractFileGdbFieldDefinition> constructor = fieldClass
        .getConstructor(int.class, Field.class);
      ESRI_FIELD_TYPE_FIELD_DEFINITION_MAP.put(fieldType, constructor);
    } catch (final SecurityException e) {
      Logs.error(FileGdbRecordStore.class, "No public constructor for ESRI type " + fieldType, e);
    } catch (final NoSuchMethodException e) {
      Logs.error(FileGdbRecordStore.class, "No public constructor for ESRI type " + fieldType, e);
    }

  }

  private static <V> V getSingleThreadResult(final Callable<V> callable) {
    final ValueHolder<Throwable> exception = new ValueHolder<>();
    final V result = TASK_EXECUTOR.call(() -> {
      try {
        return callable.call();
      } catch (RuntimeException | Error e) {
        exception.setValue(e);
        return null;
      }
    });
    final Throwable e = exception.getValue();
    if (e == null) {
      return result;
    } else if (e instanceof Error) {
      throw (Error)e;
    } else {
      throw (RuntimeException)e;
    }
  }

  public static SpatialReference getSpatialReference(final GeometryFactory geometryFactory) {
    if (geometryFactory == null || geometryFactory.getHorizontalCoordinateSystemId() == 0) {
      return null;
    } else {
      final String wkt = getSingleThreadResult(() -> {
        return EsriFileGdb
          .getSpatialReferenceWkt(geometryFactory.getHorizontalCoordinateSystemId());
      });
      final SpatialReference spatialReference = SpatialReference.get(geometryFactory, wkt);
      return spatialReference;
    }
  }

  private static void runSingleThread(final Runnable runnable) {
    getSingleThreadResult(() -> {
      runnable.run();
      return null;
    });
  }

  private final Object apiSync = new Object();

  private final Map<PathName, String> catalogPathByPath = new HashMap<>();

  private boolean createMissingRecordStore = true;

  private boolean createMissingTables = true;

  private PathName defaultSchemaPath = PathName.ROOT;

  private Map<String, List<String>> domainFieldNames = new HashMap<>();

  private boolean exists = false;

  private String fileName;

  private CloseableValueHolder<Geodatabase> geodatabase = CloseableValueHolder.lambda( //
    () -> {
      if (isExists()) {
        return getSingleThreadResult(() -> {
          try {
            return EsriFileGdb.openGeodatabase(this.fileName);
          } catch (final FileGdbException e) {
            final String message = e.getMessage();
            if ("The system cannot find the path specified. (-2147024893)".equals(message)) {
              return null;
            } else {
              throw e;
            }
          }
        });
      } else {
        return null;
      }
    }, //
    geodatabase -> {
      if (geodatabase != null) {

        final int closeResult = EsriFileGdb.CloseGeodatabase(geodatabase);
        if (closeResult != 0) {
          Logs.error(this, "Error closing: " + this.fileName + " ESRI Error=" + closeResult);
        }
      }
    }//
  );

  private final Map<PathName, AtomicLong> idGenerators = new HashMap<>();

  private final Map<String, TableReference> tableByCatalogPath = new HashMap<>();

  private boolean createLengthField = false;

  private boolean createAreaField = false;

  FileGdbRecordStore(final File file) {
    this.fileName = FileUtil.getCanonicalPath(file);
    setConnectionProperties(Collections.singletonMap("url", FileUtil.toUrl(file).toString()));
    this.catalogPathByPath.put(PathName.ROOT, "\\");
  }

  @Override
  public void addCodeTable(final CodeTable codeTable) {
    super.addCodeTable(codeTable);
    if (codeTable instanceof Domain) {
      final Domain domain = (Domain)codeTable;
      newDomainCodeTable(domain);
    }
  }

  public void alterDomain(final Domain domain) {
    final String domainDefinition = EsriGdbXmlSerializer.toString(domain);
    threadGeodatabase(geodatabase -> geodatabase.alterDomain(domainDefinition));
  }

  @Override
  public void appendQueryValue(final Query query, final StringBuilder buffer,
    final QueryValue condition) {
    if (condition instanceof Like || condition instanceof ILike) {
      final BinaryCondition like = (BinaryCondition)condition;
      final QueryValue left = like.getLeft();
      final QueryValue right = like.getRight();
      buffer.append("UPPER(CAST(");
      appendQueryValue(query, buffer, left);
      buffer.append(" AS VARCHAR(4000))) LIKE ");
      if (right instanceof Value) {
        final Value valueCondition = (Value)right;
        final Object value = valueCondition.getValue();
        buffer.append("'");
        if (value != null) {
          final String string = DataTypes.toString(value);
          buffer.append(string.toUpperCase().replaceAll("'", "''"));
        }
        buffer.append("'");
      } else {
        appendQueryValue(query, buffer, right);
      }
    } else if (condition instanceof LeftUnaryCondition) {
      final LeftUnaryCondition unaryCondition = (LeftUnaryCondition)condition;
      final String operator = unaryCondition.getOperator();
      final QueryValue right = unaryCondition.getValue();
      buffer.append(operator);
      buffer.append(" ");
      appendQueryValue(query, buffer, right);
    } else if (condition instanceof RightUnaryCondition) {
      final RightUnaryCondition unaryCondition = (RightUnaryCondition)condition;
      final QueryValue left = unaryCondition.getValue();
      final String operator = unaryCondition.getOperator();
      appendQueryValue(query, buffer, left);
      buffer.append(" ");
      buffer.append(operator);
    } else if (condition instanceof BinaryCondition) {
      final BinaryCondition binaryCondition = (BinaryCondition)condition;
      final QueryValue left = binaryCondition.getLeft();
      final String operator = binaryCondition.getOperator();
      final QueryValue right = binaryCondition.getRight();
      appendQueryValue(query, buffer, left);
      buffer.append(" ");
      buffer.append(operator);
      buffer.append(" ");
      appendQueryValue(query, buffer, right);
    } else if (condition instanceof AbstractMultiCondition) {
      final AbstractMultiCondition multipleCondition = (AbstractMultiCondition)condition;
      buffer.append("(");
      boolean first = true;
      final String operator = multipleCondition.getOperator();
      for (final QueryValue subCondition : multipleCondition.getQueryValues()) {
        if (first) {
          first = false;
        } else {
          buffer.append(" ");
          buffer.append(operator);
          buffer.append(" ");
        }
        appendQueryValue(query, buffer, subCondition);
      }
      buffer.append(")");
    } else if (condition instanceof In) {
      final In in = (In)condition;
      if (in.isEmpty()) {
        buffer.append("1==0");
      } else {
        final QueryValue left = in.getLeft();
        appendQueryValue(query, buffer, left);
        buffer.append(" IN (");
        appendQueryValue(query, buffer, in.getValues());
        buffer.append(")");
      }
    } else if (condition instanceof Value) {
      final Value valueCondition = (Value)condition;
      Object value = valueCondition.getValue();
      if (value instanceof Identifier) {
        final Identifier identifier = (Identifier)value;
        value = identifier.getValue(0);
      }
      appendValue(buffer, value);
    } else if (condition instanceof CollectionValue) {
      final CollectionValue collectionValue = (CollectionValue)condition;
      final List<Object> values = collectionValue.getValues();
      boolean first = true;
      for (final Object value : values) {
        if (first) {
          first = false;
        } else {
          buffer.append(", ");
        }
        appendValue(buffer, value);
      }
    } else if (condition instanceof Column) {
      final Column column = (Column)condition;
      final Object name = column.getName();
      buffer.append(name);
    } else if (condition instanceof SqlCondition) {
      final SqlCondition sqlCondition = (SqlCondition)condition;
      final String where = sqlCondition.getSql();
      final List<Object> parameters = sqlCondition.getParameterValues();
      if (parameters.isEmpty()) {
        if (where.indexOf('?') > -1) {
          throw new IllegalArgumentException(
            "No arguments specified for a where clause with placeholders: " + where);
        } else {
          buffer.append(where);
        }
      } else {
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(where);
        int i = 0;
        while (matcher.find()) {
          if (i >= parameters.size()) {
            throw new IllegalArgumentException(
              "Not enough arguments for where clause with placeholders: " + where);
          }
          final Object argument = parameters.get(i);
          final StringBuffer replacement = new StringBuffer();
          matcher.appendReplacement(replacement, DataTypes.toString(argument));
          buffer.append(replacement);
          appendValue(buffer, argument);
          i++;
        }
        final StringBuffer tail = new StringBuffer();
        matcher.appendTail(tail);
        buffer.append(tail);
      }
    } else if (condition instanceof EnvelopeIntersects) {
      buffer.append("1 = 1");
    } else if (condition instanceof WithinDistance) {
      buffer.append("1 = 1");
    } else {
      condition.appendDefaultSql(query, this, buffer);
    }
  }

  public void appendValue(final StringBuilder buffer, Object value) {
    if (value instanceof SingleIdentifier) {
      final SingleIdentifier identifier = (SingleIdentifier)value;
      value = identifier.getValue(0);
    }
    if (value == null) {
      buffer.append("''");
    } else if (value instanceof Number) {
      buffer.append(value);
    } else if (value instanceof java.util.Date) {
      final String stringValue = Dates.format("yyyy-MM-dd", (java.util.Date)value);
      buffer.append("DATE '" + stringValue + "'");
    } else {
      final Object value1 = value;
      final String stringValue = DataTypes.toString(value1);
      buffer.append("'");
      buffer.append(stringValue.replaceAll("'", "''"));
      buffer.append("'");
    }
  }

  @Override
  @PreDestroy
  public void close() {
    if (FileGdbRecordStoreFactory.release(this)) {
      closeDo();
    }
  }

  public void closeDo() {
    this.exists = false;
    synchronized (this.apiSync) {
      try {
        if (!isClosed()) {
          final Writer<Record> writer = getThreadProperty("writer");
          if (writer != null) {
            setThreadProperty("writer", null);
            writer.close();
          }
          synchronized (this.tableByCatalogPath) {
            for (final TableReference table : this.tableByCatalogPath.values()) {
              table.close();
            }
            this.tableByCatalogPath.clear();
          }

          final CloseableValueHolder<Geodatabase> geodatabase = this.geodatabase;
          if (geodatabase != null) {
            this.geodatabase = null;
            geodatabase.close();
          }
        }
      } finally {
        super.close();
      }
    }
  }

  private void closeGeodatabase(final Geodatabase geodatabase) {
    if (geodatabase != null) {
      final int closeResult = EsriFileGdb.CloseGeodatabase(geodatabase);
      if (closeResult != 0) {
        Logs.error(this, "Error closing: " + this.fileName + " ESRI Error=" + closeResult);
      }
    }
  }

  public void deleteGeodatabase() {
    this.createMissingRecordStore = false;
    this.createMissingTables = false;
    final String fileName = this.fileName;
    try {
      closeDo();
    } finally {
      if (new File(fileName).exists()) {
        final Integer deleteResult = getSingleThreadResult(() -> {
          return EsriFileGdb.DeleteGeodatabase(fileName);
        });
        if (deleteResult != null && deleteResult != 0) {
          Logs.error(this, "Error deleting: " + fileName + " ESRI Error=" + deleteResult);
        }
      }
    }
  }

  @Override
  public boolean deleteRecord(final Record record) {
    try (
      TableWrapper tableWrapper = getTableWrapper(record)) {
      if (tableWrapper != null) {
        return tableWrapper.deleteRecord(record);
      }
    }
    return false;
  }

  protected String getCatalogPath(final PathName path) {
    final String catalogPath = this.catalogPathByPath.get(path);
    if (Property.hasValue(catalogPath)) {
      return catalogPath;
    } else {
      return toCatalogPath(path);
    }
  }

  protected String getCatalogPath(final RecordStoreSchemaElement element) {
    final PathName path = element.getPathName();
    return getCatalogPath(path);
  }

  private VectorOfWString getChildDatasets(final Geodatabase geodatabase, final String catalogPath,
    final String datasetType) {
    final boolean pathExists = isPathExists(geodatabase, catalogPath);
    if (pathExists) {
      return geodatabase.getChildDatasets(catalogPath, datasetType);
    } else {
      return null;
    }
  }

  public PathName getDefaultSchemaPath() {
    return this.defaultSchemaPath;
  }

  public Map<String, List<String>> getDomainFieldNames() {
    return this.domainFieldNames;
  }

  public final String getFileName() {
    return this.fileName;
  }

  @Override
  public Record getRecord(final PathName typePath, final Object... id) {
    final RecordDefinition recordDefinition = getRecordDefinition(typePath);
    if (recordDefinition == null) {
      throw new IllegalArgumentException("Unknown type " + typePath);
    } else {
      final String catalogPath = getCatalogPath(typePath);
      final FileGdbQueryIterator iterator = new FileGdbQueryIterator(this, catalogPath,
        recordDefinition.getIdFieldName() + " = " + id[0]);
      try {
        if (iterator.hasNext()) {
          return iterator.next();
        } else {
          return null;
        }
      } finally {
        iterator.close();
      }
    }
  }

  @Override
  public int getRecordCount(final Query query) {
    if (query == null) {
      return 0;
    } else {
      String typePath = query.getTypeName();
      RecordDefinition recordDefinition = query.getRecordDefinition();
      if (recordDefinition == null) {
        typePath = query.getTypeName();
        recordDefinition = getRecordDefinition(typePath);
        if (recordDefinition == null) {
          return 0;
        }
      } else {
        typePath = recordDefinition.getPath();
      }
      final StringBuilder whereClause = getWhereClause(query);
      final BoundingBox boundingBox = QueryValue.getBoundingBox(query);

      final TableReference table = getTableReference(recordDefinition);
      if (boundingBox == null) {
        if (whereClause.length() == 0) {
          return table.valueFunction(Table::getRowCount, 0);
        } else {
          final StringBuilder sql = new StringBuilder();
          sql.append("SELECT OBJECTID FROM ");
          sql.append(JdbcUtils.getTableName(typePath));
          if (whereClause.length() > 0) {
            sql.append(" WHERE ");
            sql.append(whereClause);
          }

          try (
            final FileGdbEnumRowsIterator rows = table.query(sql.toString(), false)) {
            int count = 0;
            for (@SuppressWarnings("unused")
            final Row row : rows) {
              count++;
            }
            return count;
          }
        }
      } else {
        final GeometryFieldDefinition geometryField = (GeometryFieldDefinition)recordDefinition
          .getGeometryField();
        if (geometryField == null || boundingBox.isEmpty()) {
          return 0;
        } else {
          final StringBuilder sql = new StringBuilder();
          sql.append("SELECT " + geometryField.getName() + " FROM ");
          sql.append(JdbcUtils.getTableName(typePath));
          if (whereClause.length() > 0) {
            sql.append(" WHERE ");
            sql.append(whereClause);
          }

          try (
            final FileGdbEnumRowsIterator rows = table.query(sql.toString(), false)) {
            int count = 0;
            for (final Row row : rows) {
              final Geometry geometry = (Geometry)geometryField.getValue(row);
              if (geometry != null) {
                final BoundingBox geometryBoundingBox = geometry.getBoundingBox();
                if (geometryBoundingBox.bboxIntersects(boundingBox)) {
                  count++;
                }
              }
            }
            return count;
          }
        }
      }
    }
  }

  public RecordDefinitionImpl getRecordDefinition(final PathName schemaName, final String path,
    final String tableDefinition) {
    try {
      final XmlProcessor parser = new EsriGdbXmlParser();
      final DETable deTable = parser.process(tableDefinition);
      final String tableName = deTable.getName();
      final PathName typePath = PathName.newPathName(schemaName.newChild(tableName));
      final RecordStoreSchema schema = getSchema(schemaName);
      final RecordDefinitionImpl recordDefinition = new RecordDefinitionImpl(schema, typePath);
      recordDefinition.setPolygonRingDirection(ClockDirection.CLOCKWISE);
      String lengthFieldName = null;
      String areaFieldName = null;
      if (deTable instanceof DEFeatureClass) {
        final DEFeatureClass featureClass = (DEFeatureClass)deTable;

        lengthFieldName = featureClass.getLengthFieldName();
        final LengthFieldName lengthFieldNameProperty = new LengthFieldName(lengthFieldName);
        lengthFieldNameProperty.setRecordDefinition(recordDefinition);

        areaFieldName = featureClass.getAreaFieldName();
        final LengthFieldName areaFieldNameProperty = new LengthFieldName(areaFieldName);
        areaFieldNameProperty.setRecordDefinition(recordDefinition);

      }
      int fieldNumber = 0;
      for (final Field field : deTable.getFields()) {
        final String fieldName = field.getName();
        AbstractFileGdbFieldDefinition fieldDefinition = null;
        if (fieldName.equals(lengthFieldName)) {
          fieldDefinition = new LengthFieldDefinition(fieldNumber, field);
        } else if (fieldName.equals(areaFieldName)) {
          fieldDefinition = new AreaFieldDefinition(fieldNumber, field);
        } else {
          final FieldType type = field.getType();
          final Constructor<? extends AbstractFileGdbFieldDefinition> fieldConstructor = ESRI_FIELD_TYPE_FIELD_DEFINITION_MAP
            .get(type);
          if (fieldConstructor != null) {
            try {
              fieldDefinition = JavaBeanUtil.invokeConstructor(fieldConstructor, fieldNumber,
                field);
            } catch (final Throwable e) {
              Logs.error(this, tableDefinition);
              throw new RuntimeException("Error creating field for " + typePath + "."
                + field.getName() + " : " + field.getType(), e);
            }
          } else {
            Logs.error(this, "Unsupported field type " + fieldName + ":" + type);
          }
        }
        if (fieldDefinition != null) {
          final Domain domain = field.getDomain();
          if (domain != null) {
            CodeTable codeTable = getCodeTable(domain.getDomainName() + "_ID");
            if (codeTable == null) {
              codeTable = new FileGdbDomainCodeTable(this, domain);
              addCodeTable(codeTable);
            }
            fieldDefinition.setCodeTable(codeTable);
          }
          fieldDefinition.setRecordStore(this);
          recordDefinition.addField(fieldDefinition);
          if (fieldDefinition instanceof GlobalIdFieldDefinition) {
            recordDefinition.setIdFieldName(fieldName);
          }
        }
        fieldNumber++;
      }
      final String oidFieldName = deTable.getOIDFieldName();
      recordDefinition.setProperty(EsriGeodatabaseXmlConstants.ESRI_OBJECT_ID_FIELD_NAME,
        oidFieldName);
      if (deTable instanceof DEFeatureClass) {
        final DEFeatureClass featureClass = (DEFeatureClass)deTable;
        final String shapeFieldName = featureClass.getShapeFieldName();
        recordDefinition.setGeometryFieldName(shapeFieldName);
      }
      for (final Index index : deTable.getIndexes()) {
        if (index.getName().endsWith("_PK")) {
          for (final Field field : index.getFields()) {
            final String fieldName = field.getName();
            recordDefinition.setIdFieldName(fieldName);
          }
        }
      }
      addRecordDefinitionProperties(recordDefinition);
      if (recordDefinition.getIdFieldIndex() == -1) {
        recordDefinition.setIdFieldName(deTable.getOIDFieldName());
      }
      this.catalogPathByPath.put(typePath, deTable.getCatalogPath());
      return recordDefinition;
    } catch (final RuntimeException e) {
      Logs.debug(this, tableDefinition);
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <RD extends RecordDefinition> RD getRecordDefinition(
    final RecordDefinition sourceRecordDefinition) {
    synchronized (this.tableByCatalogPath) {
      if (getGeometryFactory() == null) {
        setGeometryFactory(sourceRecordDefinition.getGeometryFactory());
      }
      final String typePath = sourceRecordDefinition.getPath();
      RecordDefinition recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        if (!sourceRecordDefinition.hasGeometryField()) {
          recordDefinition = getRecordDefinition(PathUtil.getName(typePath));
        }
        if (this.createMissingTables && recordDefinition == null) {
          final GeometryFactory geometryFactory = sourceRecordDefinition.getGeometryFactory();
          final SpatialReference spatialReference = getSpatialReference(geometryFactory);

          final DETable deTable = EsriXmlRecordDefinitionUtil.getDETable(sourceRecordDefinition,
            spatialReference, this.createLengthField, this.createAreaField);
          final RecordDefinitionImpl tableRecordDefinition = newTableRecordDefinition(deTable);
          final String idFieldName = sourceRecordDefinition.getIdFieldName();
          if (idFieldName != null) {
            tableRecordDefinition.setIdFieldName(idFieldName);
          }
          recordDefinition = tableRecordDefinition;
        }
      }
      return (RD)recordDefinition;
    }
  }

  @Override
  public String getRecordStoreType() {
    return FileGdbRecordStoreFactory.DESCRIPTION;
  }

  public TableWrapper getTableLocked(final RecordDefinition recordDefinition) {
    final TableReference table = getTableReference(recordDefinition);
    if (table == null) {
      return null;
    } else {
      return table.writeLock();
    }
  }

  protected TableReference getTableReference(final RecordDefinition recordDefinition) {
    synchronized (this.tableByCatalogPath) {
      final RecordDefinition fgdbRecordDefinition = getRecordDefinition(recordDefinition);
      if (!isExists() || fgdbRecordDefinition == null) {
        return null;
      } else {
        final String catalogPath = getCatalogPath(fgdbRecordDefinition);
        TableReference tableReference = this.tableByCatalogPath.get(catalogPath);
        if (tableReference == null) {
          final PathName pathName = fgdbRecordDefinition.getPathName();
          tableReference = new TableReference(this, this.geodatabase, pathName, catalogPath);
          this.tableByCatalogPath.put(catalogPath, tableReference);
        }
        return tableReference;
      }
    }
  }

  protected TableReference getTableReference(final RecordDefinitionProxy recordDefinition) {
    if (recordDefinition != null) {
      final RecordDefinition rd = recordDefinition.getRecordDefinition();
      return getTableReference(rd);
    }
    return null;
  }

  private TableWrapper getTableWrapper(final Record record) {
    final TableReference tableReference = getTableReference(record);
    if (tableReference != null) {
      try (
        TableWrapper tableWrapper = tableReference.connect()) {
        return tableWrapper;
      }
    }
    return null;
  }

  protected StringBuilder getWhereClause(final Query query) {
    final StringBuilder whereClause = new StringBuilder();
    final Condition whereCondition = query.getWhereCondition();
    if (!whereCondition.isEmpty()) {
      appendQueryValue(query, whereClause, whereCondition);
    }
    return whereClause;
  }

  private boolean hasChildDataset(final Geodatabase geodatabase, final String parentCatalogPath,
    final String datasetType, final String childCatalogPath) {
    try {
      final VectorOfWString childDatasets = geodatabase.getChildDatasets(parentCatalogPath,
        datasetType);
      for (int i = 0; i < childDatasets.size(); i++) {
        final String catalogPath = childDatasets.get(i);
        if (catalogPath.equals(childCatalogPath)) {
          return true;
        }
      }
      return false;
    } catch (final RuntimeException e) {
      if ("-2147211775\tThe item was not found.".equals(e.getMessage())) {
        return false;
      } else {
        throw e;
      }
    }
  }

  @Override
  public void initializeDo() {
    synchronized (this.apiSync) {
      Geodatabase geodatabase = null;
      try {
        super.initializeDo();
        final File file = new File(this.fileName);
        if (file.exists()) {
          if (file.isDirectory()) {
            if (!new File(this.fileName, "gdb").exists()) {
              throw new IllegalArgumentException(
                FileUtil.getCanonicalPath(file) + " is not a valid ESRI File Geodatabase");
            }
            geodatabase = getSingleThreadResult(() -> {
              return EsriFileGdb.openGeodatabase(this.fileName);
            });
          } else {
            throw new IllegalArgumentException(
              FileUtil.getCanonicalPath(file) + " ESRI File Geodatabase must be a directory");
          }
        } else if (this.createMissingRecordStore) {
          geodatabase = getSingleThreadResult(() -> {
            return EsriFileGdb.createGeodatabase(this.fileName);
          });
        } else {
          throw new IllegalArgumentException("ESRI file geodatabase not found " + this.fileName);
        }
        if (geodatabase == null) {
          throw new IllegalArgumentException(
            "Unable to open ESRI file geodatabase not found " + this.fileName);
        }
        final VectorOfWString domainNames = geodatabase.getDomains();
        for (int i = 0; i < domainNames.size(); i++) {
          final String domainName = domainNames.get(i);
          loadDomain(geodatabase, domainName);
        }
        this.exists = true;
      } catch (final Throwable e) {
        try {
          closeDo();
        } finally {
          Exceptions.throwUncheckedException(e);
        }
      } finally {
        if (geodatabase != null) {
          closeGeodatabase(geodatabase);
        }
      }
    }
  }

  @Override
  public void insertRecord(final Record record) {
    try (
      TableWrapper tableWrapper = getTableWrapper(record)) {
      if (tableWrapper != null) {
        tableWrapper.insertRecord(record);
      }
    }
  }

  public boolean isCreateAreaField() {
    return this.createAreaField;
  }

  public boolean isCreateLengthField() {
    return this.createLengthField;
  }

  public boolean isCreateMissingRecordStore() {
    return this.createMissingRecordStore;
  }

  public boolean isCreateMissingTables() {
    return this.createMissingTables;
  }

  public boolean isExists() {
    return this.exists && !isClosed();
  }

  private boolean isPathExists(final Geodatabase geodatabase, String path) {
    if (path == null) {
      return false;
    } else if ("\\".equals(path)) {
      return true;
    } else {
      final boolean pathExists = true;

      path = path.replaceAll("[\\/]+", "\\");
      path = path.replaceAll("\\$", "");
      int index = 0;
      while (index != -1) {
        final String parentPath = path.substring(0, index + 1);
        final int nextIndex = path.indexOf(index + 1, '\\');
        String currentPath;
        if (nextIndex == -1) {
          currentPath = path;
        } else {
          currentPath = path.substring(0, nextIndex);
        }
        boolean found = false;
        final VectorOfWString children = geodatabase.getChildDatasets(parentPath,
          "Feature Dataset");
        for (int i = 0; i < children.size(); i++) {
          final String childPath = children.get(i);
          if (childPath.equals(currentPath)) {
            found = true;
          }
        }
        if (!found) {
          return false;
        }
        index = nextIndex;
      }
      return pathExists;
    }
  }

  protected FileGdbDomainCodeTable loadDomain(final Geodatabase geodatabase,
    final String domainName) {
    final String domainDef = geodatabase.getDomainDefinition(domainName);
    final Domain domain = EsriGdbXmlParser.parse(domainDef);
    if (domain != null) {
      final FileGdbDomainCodeTable codeTable = new FileGdbDomainCodeTable(this, domain);
      super.addCodeTable(codeTable);
      final List<String> fieldNames = this.domainFieldNames.get(domainName);
      if (fieldNames != null) {
        for (final String fieldName : fieldNames) {
          addCodeTable(fieldName, codeTable);
        }
      }
      return codeTable;
    }
    return null;
  }

  public CodeTable newDomainCodeTable(final Domain domain) {
    final String domainName = domain.getDomainName();
    if (!this.domainFieldNames.containsKey(domainName)) {
      final String domainDef = EsriGdbXmlSerializer.toString(domain);
      return threadGeodatabaseResult(geodatabase -> {
        try {
          geodatabase.createDomain(domainDef);
          return loadDomain(geodatabase, domain.getDomainName());
        } catch (final Exception e) {
          Logs.debug(this, domainDef);
          Logs.error(this, "Unable to create domain", e);
          return null;
        }
      });
    }
    return null;
  }

  private RecordStoreSchema newFeatureDatasetSchema(final RecordStoreSchema parentSchema,
    final PathName schemaPath) {

    final PathName childSchemaPath = schemaPath;
    final RecordStoreSchema schema = new RecordStoreSchema(parentSchema, childSchemaPath);
    this.catalogPathByPath.put(childSchemaPath, toCatalogPath(schemaPath));
    return schema;
  }

  @Override
  public AbstractIterator<Record> newIterator(final Query query,
    final Map<String, Object> properties) {
    PathName typePath = query.getTypePath();
    RecordDefinition recordDefinition = query.getRecordDefinition();
    if (recordDefinition == null) {
      recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        throw new IllegalArgumentException("Type name does not exist " + typePath);
      }
    } else {
      typePath = recordDefinition.getPathName();
    }
    final String catalogPath = getCatalogPath(typePath);
    final BoundingBox boundingBox = QueryValue.getBoundingBox(query);
    final Map<? extends CharSequence, Boolean> orderBy = query.getOrderBy();
    final StringBuilder whereClause = getWhereClause(query);
    StringBuilder sql = new StringBuilder();
    if (orderBy.isEmpty() || boundingBox != null) {
      if (!orderBy.isEmpty()) {
        Logs.error(this, "Unable to sort on " + catalogPath + " " + orderBy.keySet()
          + " as the ESRI library can't sort with a bounding box query");
      }
      sql = whereClause;
    } else {
      sql.append("SELECT ");

      final List<String> fieldNames = query.getFieldNames();
      if (fieldNames.isEmpty()) {
        StringBuilders.append(sql, recordDefinition.getFieldNames());
      } else {
        StringBuilders.append(sql, fieldNames);
      }
      sql.append(" FROM ");
      sql.append(JdbcUtils.getTableName(catalogPath));
      if (whereClause.length() > 0) {
        sql.append(" WHERE ");
        sql.append(whereClause);
      }
      boolean first = true;
      for (final Entry<? extends CharSequence, Boolean> entry : orderBy.entrySet()) {
        final CharSequence fieldName = entry.getKey();
        final DataType dataType = recordDefinition.getFieldType(fieldName);
        if (dataType != null && !Geometry.class.isAssignableFrom(dataType.getJavaClass())) {
          if (first) {
            sql.append(" ORDER BY ");
            first = false;
          } else {
            sql.append(", ");
          }
          if (fieldName instanceof FieldDefinition) {
            final FieldDefinition field = (FieldDefinition)fieldName;
            field.appendColumnName(sql);
          } else {
            sql.append(fieldName);
          }
          final Boolean ascending = entry.getValue();
          if (!ascending) {
            sql.append(" DESC");
          }

        } else {
          Logs.error(this, "Unable to sort on " + recordDefinition.getPath() + "." + fieldName
            + " as the ESRI library can't sort on " + dataType + " columns");
        }
      }
    }

    final FileGdbQueryIterator iterator = new FileGdbQueryIterator(this, catalogPath,
      sql.toString(), boundingBox, query, query.getOffset(), query.getLimit());
    iterator.setStatistics(query.getStatistics());
    return iterator;
  }

  @Override
  public Identifier newPrimaryIdentifier(final PathName typePath) {
    synchronized (this.idGenerators) {
      final RecordDefinition recordDefinition = getRecordDefinition(typePath);
      if (recordDefinition == null) {
        return null;
      } else {
        final String idFieldName = recordDefinition.getIdFieldName();
        if (idFieldName == null) {
          return null;
        } else if (!idFieldName.equals("OBJECTID")) {
          AtomicLong idGenerator = this.idGenerators.get(typePath);
          if (idGenerator == null) {
            long maxId = 0;
            for (final Record record : getRecords(typePath)) {
              final Identifier id = record.getIdentifier();
              final Object firstId = id.getValue(0);
              if (firstId instanceof Number) {
                final Number number = (Number)firstId;
                if (number.longValue() > maxId) {
                  maxId = number.longValue();
                }
              }
            }
            idGenerator = new AtomicLong(maxId);
            this.idGenerators.put(typePath, idGenerator);
          }
          return Identifier.newIdentifier(idGenerator.incrementAndGet());
        } else {
          return null;
        }
      }
    }
  }

  @Override
  public FileGdbWriter newRecordWriter() {
    FileGdbWriter writer = getThreadProperty("writer");
    if (writer == null || writer.isClosed()) {
      writer = new FileGdbWriter(this);
      setThreadProperty("writer", writer);
    }
    return writer;
  }

  @Override
  public FileGdbWriter newRecordWriter(final RecordDefinition recordDefinition) {
    final RecordDefinition fgdbRecordDefinition = getRecordDefinition(recordDefinition);
    return new FileGdbWriter(this, fgdbRecordDefinition);
  }

  private RecordStoreSchema newSchema(final Geodatabase geodatabase, final PathName schemaPath,
    final SpatialReference spatialReference) {
    String parentCatalogPath = "\\";
    RecordStoreSchema schema = getRootSchema();
    for (final PathName childSchemaPath : schemaPath.getPaths()) {
      if (childSchemaPath.length() > 1) {
        RecordStoreSchema childSchema = schema.getSchema(childSchemaPath);
        final String childCatalogPath = toCatalogPath(childSchemaPath);
        if (!hasChildDataset(geodatabase, parentCatalogPath, "Feature Dataset", childCatalogPath)) {
          if (spatialReference != null) {
            final DEFeatureDataset dataset = EsriXmlRecordDefinitionUtil
              .newDEFeatureDataset(childCatalogPath, spatialReference);
            final String datasetDefinition = EsriGdbXmlSerializer.toString(dataset);
            try {
              runSingleThread(() -> geodatabase.createFeatureDataset(datasetDefinition));
            } catch (final Throwable t) {
              Logs.debug(this, datasetDefinition);
              throw Exceptions.wrap("Unable to create feature dataset " + childCatalogPath, t);
            }
          }
        }
        if (childSchema == null) {
          childSchema = newFeatureDatasetSchema(schema, childSchemaPath);
          schema.addElement(childSchema);
        }
        schema = childSchema;
        parentCatalogPath = childCatalogPath;
      }
    }
    return schema;
  }

  private RecordDefinitionImpl newTableRecordDefinition(final DETable deTable) {
    String schemaCatalogPath = deTable.getParentCatalogPath();
    SpatialReference spatialReference;
    if (deTable instanceof DEFeatureClass) {
      final DEFeatureClass featureClass = (DEFeatureClass)deTable;
      spatialReference = featureClass.getSpatialReference();
    } else {
      spatialReference = null;
    }
    PathName schemaPath = toPath(schemaCatalogPath);
    final PathName schemaPath2 = schemaPath;
    RecordStoreSchema schema = this.geodatabase
      .valueFunctionSync(geodatabase -> newSchema(geodatabase, schemaPath2, spatialReference));

    if (schemaPath.equals(this.defaultSchemaPath)) {
      if (!(deTable instanceof DEFeatureClass)) {
        schemaCatalogPath = "\\";
        deTable.setCatalogPath("\\" + deTable.getName());
        schema = getRootSchema();
        schemaPath = schema.getPathName();
      }
    } else if (schemaPath.length() <= 1) {
      if (deTable instanceof DEFeatureClass) {
        schemaPath = this.defaultSchemaPath;
      }
    }
    for (final Field field : deTable.getFields()) {
      final String fieldName = field.getName();
      final CodeTable codeTable = getCodeTableByFieldName(fieldName);
      if (codeTable instanceof FileGdbDomainCodeTable) {
        final FileGdbDomainCodeTable domainCodeTable = (FileGdbDomainCodeTable)codeTable;
        field.setDomain(domainCodeTable.getDomain());
      }
    }
    final String tableDefinition = EsriGdbXmlSerializer.toString(deTable);
    try {
      final String scp = schemaCatalogPath;
      threadGeodatabase(geodatabase -> {
        final Table table = geodatabase.createTable(tableDefinition, scp);
        geodatabase.closeTable(table);
        table.delete();
      });
      final RecordDefinitionImpl recordDefinition = getRecordDefinition(
        PathName.newPathName(schemaPath), schemaCatalogPath, tableDefinition);
      initRecordDefinition(recordDefinition);
      schema.addElement(recordDefinition);
      return recordDefinition;
    } catch (final Throwable t) {
      throw new RuntimeException("Unable to create table " + deTable.getCatalogPath(), t);
    }
  }

  @Override
  protected Map<PathName, ? extends RecordStoreSchemaElement> refreshSchemaElements(
    final RecordStoreSchema schema) {
    synchronized (schema) {
      final Map<PathName, RecordStoreSchemaElement> elementsByPath = new TreeMap<>();
      final Consumer<Geodatabase> action = geodatabase -> {
        final PathName schemaPath = schema.getPathName();
        final String schemaCatalogPath = getCatalogPath(schema);
        final VectorOfWString childDatasets = getChildDatasets(geodatabase, schemaCatalogPath,
          "Feature Dataset");
        if (childDatasets != null) {
          for (int i = 0; i < childDatasets.size(); i++) {
            final String childCatalogPath = childDatasets.get(i);
            final PathName childPath = toPath(childCatalogPath);
            RecordStoreSchema childSchema = schema.getSchema(childPath);
            if (childSchema == null) {
              childSchema = newFeatureDatasetSchema(schema, childPath);
            } else {
              if (childSchema.isInitialized()) {
                childSchema.refresh();
              }
            }
            elementsByPath.put(childPath, childSchema);
          }
        }
        if (schemaPath.isParentOf(this.defaultSchemaPath)
          && !elementsByPath.containsKey(this.defaultSchemaPath)) {
          final SpatialReference spatialReference = getSpatialReference(getGeometryFactory());
          final RecordStoreSchema childSchema = newSchema(geodatabase, this.defaultSchemaPath,
            spatialReference);
          elementsByPath.put(this.defaultSchemaPath, childSchema);
        }

        if (schema.equalPath(this.defaultSchemaPath)) {
          refreshSchemaRecordDefinitions(geodatabase, elementsByPath, schemaPath, "\\",
            "Feature Class");
          refreshSchemaRecordDefinitions(geodatabase, elementsByPath, schemaPath, "\\", "Table");
        }
        refreshSchemaRecordDefinitions(geodatabase, elementsByPath, schemaPath, schemaCatalogPath,
          "Feature Class");
        refreshSchemaRecordDefinitions(geodatabase, elementsByPath, schemaPath, schemaCatalogPath,
          "Table");
      };
      this.geodatabase.valueConsumeSync(action);
      return elementsByPath;
    }
  }

  private void refreshSchemaRecordDefinitions(final Geodatabase geodatabase,
    final Map<PathName, RecordStoreSchemaElement> elementsByPath, final PathName schemaPath,
    final String catalogPath, final String datasetType) {
    final boolean pathExists = isPathExists(geodatabase, catalogPath);
    if (pathExists) {
      final VectorOfWString childFeatureClasses = getChildDatasets(geodatabase, catalogPath,
        datasetType);
      if (childFeatureClasses != null) {
        for (int i = 0; i < childFeatureClasses.size(); i++) {
          final String childCatalogPath = childFeatureClasses.get(i);
          final String tableDefinition = geodatabase.getTableDefinition(childCatalogPath);
          final RecordDefinition recordDefinition = getRecordDefinition(schemaPath,
            childCatalogPath, tableDefinition);
          initRecordDefinition(recordDefinition);
          final PathName childPath = recordDefinition.getPathName();
          elementsByPath.put(childPath, recordDefinition);
        }
      }
    }
  }

  public void setCreateAreaField(final boolean createAreaField) {
    this.createAreaField = createAreaField;
  }

  public void setCreateLengthField(final boolean createLengthField) {
    this.createLengthField = createLengthField;
  }

  public void setCreateMissingRecordStore(final boolean createMissingRecordStore) {
    this.createMissingRecordStore = createMissingRecordStore;
  }

  public void setCreateMissingTables(final boolean createMissingTables) {
    this.createMissingTables = createMissingTables;
  }

  public void setDefaultSchema(final PathName defaultSchema) {
    if (defaultSchema != null) {
      this.defaultSchemaPath = defaultSchema;
    } else {
      this.defaultSchemaPath = PathName.ROOT;
    }
  }

  public void setDefaultSchema(final String defaultSchema) {
    setDefaultSchema(PathName.newPathName(defaultSchema));
  }

  public void setDomainFieldNames(final Map<String, List<String>> domainFieldNames) {
    this.domainFieldNames = domainFieldNames;
  }

  public void setFileName(final String fileName) {
    this.fileName = fileName;
  }

  private void threadGeodatabase(final Consumer<Geodatabase> action) {
    getSingleThreadResult(() -> {
      this.geodatabase.valueConsumeSync(action);
      return null;
    });
  }

  <V> V threadGeodatabaseResult(final Function<Geodatabase, V> action) {
    return getSingleThreadResult(() -> this.geodatabase.valueFunctionSync(action));
  }

  public String toCatalogPath(final PathName path) {
    return path.getPath().replaceAll("/", "\\\\");
  }

  protected PathName toPath(final String catalogPath) {
    return PathName.newPathName(catalogPath);
  }

  @Override
  public String toString() {
    return this.fileName;
  }

  @Override
  public void updateRecord(final Record record) {
    try (
      TableWrapper tableWrapper = getTableWrapper(record)) {
      if (tableWrapper != null) {
        tableWrapper.updateRecord(record);
      }
    }
  }

  public BaseCloseable writeLock(final PathName path) {
    final RecordDefinition recordDefinition = getRecordDefinition(path);
    if (recordDefinition != null) {
      final TableReference table = getTableReference(recordDefinition);
      if (table != null) {
        return table.writeLock();
      }
    }
    return null;
  }

}
