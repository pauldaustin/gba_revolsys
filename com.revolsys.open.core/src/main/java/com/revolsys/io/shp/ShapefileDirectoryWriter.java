package com.revolsys.io.shp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StringUtils;

import com.revolsys.data.io.AbstractRecordWriterFactory;
import com.revolsys.data.record.Record;
import com.revolsys.data.record.schema.RecordDefinition;
import com.revolsys.gis.io.Statistics;
import com.revolsys.io.AbstractWriter;
import com.revolsys.io.IoConstants;
import com.revolsys.io.PathUtil;
import com.revolsys.io.Writer;
import com.revolsys.io.xbase.XbaseRecordWriter;
import com.revolsys.jts.geom.Geometry;

public class ShapefileDirectoryWriter extends AbstractWriter<Record> {
  private File directory;

  private boolean useZeroForNull = true;

  private Map<String, Writer<Record>> writers = new HashMap<>();

  private Map<String, RecordDefinition> recordDefinitionMap = new HashMap<>();

  private boolean useNamespaceAsSubDirectory;

  private Statistics statistics;

  private String nameSuffix = "";

  public ShapefileDirectoryWriter() {
  }

  public ShapefileDirectoryWriter(final File baseDirectory) {
    setDirectory(baseDirectory);
  }

  @Override
  @PreDestroy
  public void close() {
    if (writers != null) {
      for (final Writer<Record> writer : writers.values()) {
        try {
          writer.close();
        } catch (final RuntimeException e) {
          e.printStackTrace();
        }
      }
      writers = null;
      recordDefinitionMap = null;
    }
    if (statistics != null) {
      statistics.disconnect();
      statistics = null;
    }
  }

  @Override
  public void flush() {
    for (final Writer<Record> writer : writers.values()) {
      try {
        writer.flush();
      } catch (final RuntimeException e) {
        e.printStackTrace();
      }
    }
  }

  public File getDirectory() {
    return directory;
  }

  private File getDirectory(final RecordDefinition recordDefinition) {
    if (useNamespaceAsSubDirectory) {
      final String typePath = recordDefinition.getPath();
      final String schemaName = PathUtil.getPath(typePath);
      if (StringUtils.hasText(schemaName)) {
        final File childDirectory = new File(directory, schemaName);
        if (!childDirectory.mkdirs()) {
          if (!childDirectory.isDirectory()) {
            throw new IllegalArgumentException("Unable to create directory "
              + childDirectory);
          }
        }
        return childDirectory;
      }
    }
    return directory;
  }

  private String getFileName(final RecordDefinition recordDefinition) {
    return recordDefinition.getTypeName();
  }

  public RecordDefinition getRecordDefinition(final String path) {
    return recordDefinitionMap.get(path);
  }

  public String getNameSuffix() {
    return nameSuffix;
  }

  public Statistics getStatistics() {
    return statistics;
  }

  private Writer<Record> getWriter(final Record object) {
    final RecordDefinition recordDefinition = object.getRecordDefinition();
    final String path = recordDefinition.getPath();
    Writer<Record> writer = writers.get(path);
    if (writer == null) {
      final File directory = getDirectory(recordDefinition);
      directory.mkdirs();
      final File file = new File(directory, getFileName(recordDefinition) + nameSuffix
        + ".shp");
      writer = AbstractRecordWriterFactory.recordWriter(recordDefinition,
        new FileSystemResource(file));

      ((XbaseRecordWriter)writer).setUseZeroForNull(useZeroForNull);
      final Geometry geometry = object.getGeometryValue();
      if (geometry != null) {
        setProperty(IoConstants.GEOMETRY_FACTORY, geometry.getGeometryFactory());
      }
      writers.put(path, writer);
      recordDefinitionMap.put(path, ((ShapefileRecordWriter)writer).getRecordDefinition());
    }
    return writer;
  }

  public boolean isUseNamespaceAsSubDirectory() {
    return useNamespaceAsSubDirectory;
  }

  public boolean isUseZeroForNull() {
    return useZeroForNull;
  }

  public void setDirectory(final File baseDirectory) {
    this.directory = baseDirectory;
    baseDirectory.mkdirs();
    statistics = new Statistics("Write Shape "
      + baseDirectory.getAbsolutePath());
    statistics.connect();
  }

  public void setLogCounts(final boolean logCounts) {
    statistics.setLogCounts(false);
  }

  public void setNameSuffix(final String nameSuffix) {
    this.nameSuffix = nameSuffix;
  }

  public void setStatistics(final Statistics statistics) {
    if (this.statistics != statistics) {
      this.statistics = statistics;
      statistics.connect();
    }
  }

  public void setUseNamespaceAsSubDirectory(
    final boolean useNamespaceAsSubDirectory) {
    this.useNamespaceAsSubDirectory = useNamespaceAsSubDirectory;
  }

  public void setUseZeroForNull(final boolean useZeroForNull) {
    this.useZeroForNull = useZeroForNull;
  }

  @Override
  public String toString() {
    return directory.getAbsolutePath();
  }

  @Override
  public void write(final Record object) {
    final Writer<Record> writer = getWriter(object);
    writer.write(object);
    statistics.add(object);
  }

}