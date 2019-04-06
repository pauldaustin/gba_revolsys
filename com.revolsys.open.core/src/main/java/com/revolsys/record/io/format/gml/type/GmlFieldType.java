package com.revolsys.record.io.format.gml.type;

import org.jeometry.common.datatype.DataType;

import com.revolsys.record.io.format.xml.XmlWriter;

public interface GmlFieldType {

  DataType getDataType();

  String getXmlSchemaTypeName();

  void writeValue(XmlWriter out, Object value);
}
