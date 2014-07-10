/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.revolsys.gis.esri.gdb.file.capi.swig;

public enum FieldType {
  fieldTypeSmallInteger(0), fieldTypeInteger(1), fieldTypeSingle(2), fieldTypeDouble(
    3), fieldTypeString(4), fieldTypeDate(5), fieldTypeOID(6), fieldTypeGeometry(
    7), fieldTypeBlob(8), fieldTypeRaster(9), fieldTypeGUID(10), fieldTypeGlobalID(
    11), fieldTypeXML(12);

  private static class SwigNext {
    private static int next = 0;
  }

  public static FieldType swigToEnum(final int swigValue) {
    final FieldType[] swigValues = FieldType.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0
      && swigValues[swigValue].swigValue == swigValue) {
      return swigValues[swigValue];
    }
    for (final FieldType swigEnum : swigValues) {
      if (swigEnum.swigValue == swigValue) {
        return swigEnum;
      }
    }
    throw new IllegalArgumentException("No enum " + FieldType.class
      + " with value " + swigValue);
  }

  private final int swigValue;

  @SuppressWarnings("unused")
  private FieldType() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private FieldType(final FieldType swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue + 1;
  }

  @SuppressWarnings("unused")
  private FieldType(final int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue + 1;
  }

  public final int swigValue() {
    return this.swigValue;
  }
}
