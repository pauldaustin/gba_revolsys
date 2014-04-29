/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.revolsys.jtstest.geomop;

import com.revolsys.io.wkt.WktWriter;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.operation.buffer.validate.BufferResultValidator;
import com.revolsys.jts.util.Assert;
import com.revolsys.jtstest.testrunner.GeometryResult;
import com.revolsys.jtstest.testrunner.Result;

/**
 * A {@link GeometryOperation} which validates the results of the 
 * {@link Geometry} <tt>buffer()</tt> method.
 * If an invalid result is found, an exception is thrown (this is the most
 * convenient and noticeable way of flagging the problem when using the TestRunner).
 * All other Geometry methods are executed normally.
 * <p>
 * This class can be used via the <tt>-geomop</tt> command-line option
 * or by the <tt>&lt;geometryOperation&gt;</tt> XML test file setting.
 *
 * @author mbdavis
 *
 */
public class BufferValidatedGeometryOperation implements GeometryOperation {

  private final boolean returnEmptyGC = false;

  private GeometryMethodOperation chainOp = new GeometryMethodOperation();

  private int argCount = 0;

  private double distance;

  private int quadSegments;

  private int endCapStyle;

  public BufferValidatedGeometryOperation() {

  }

  /**
   * Creates a new operation which chains to the given {@link GeometryMethodOperation}
   * for non-intercepted methods.
   * 
   * @param chainOp the operation to chain to
   */
  public BufferValidatedGeometryOperation(final GeometryMethodOperation chainOp) {
    this.chainOp = chainOp;
  }

  private void checkContainment(final Geometry geom, final Geometry buffer) {
    boolean isCovered = true;
    String errMsg = "";
    if (distance > 0) {
      isCovered = buffer.covers(geom);
      errMsg = "Geometry is not contained in (positive) buffer";
    } else if (distance < 0) {
      errMsg = "Geometry does not contain (negative) buffer";
      // covers is always false for empty geometries, so don't bother testing
      // them
      if (buffer.isEmpty()) {
        isCovered = true;
      } else {
        isCovered = geom.covers(buffer);
      }

    }
    if (!isCovered) {
      reportError(errMsg, null);
    }
  }

  private void checkDistance(final Geometry geom, final double distance,
    final Geometry buffer) {
    final BufferResultValidator bufValidator = new BufferResultValidator(geom,
      distance, buffer);
    if (!bufValidator.isValid()) {
      final String errorMsg = bufValidator.getErrorMessage();
      final Coordinates errorLoc = bufValidator.getErrorLocation();
      reportError(errorMsg, errorLoc);
    }
  }

  private void checkEmpty(final Geometry geom) {
    if (geom.isEmpty()) {
      return;
    }
    reportError("Expected empty buffer result", null);
  }

  @Override
  public Class getReturnType(final String opName) {
    return chainOp.getReturnType(opName);
  }

  /**
   * Invokes the named operation
   * 
   * @param opName
   * @param geometry
   * @param args
   * @return the result
   * @throws Exception
   * @see GeometryOperation#invoke
   */
  @Override
  public Result invoke(final String opName, final Geometry geometry,
    final Object[] args) throws Exception {
    final boolean isBufferOp = opName.equalsIgnoreCase("buffer");
    // if not a buffer op, do the default
    if (!isBufferOp) {
      return chainOp.invoke(opName, geometry, args);
    }
    parseArgs(args);
    return invokeBufferOpValidated(geometry, args);
  }

  private Geometry invokeBuffer(final Geometry geom) {
    if (argCount == 1) {
      return geom.buffer(distance);
    }
    if (argCount == 2) {
      return geom.buffer(distance, quadSegments);
    }
    Assert.shouldNeverReachHere("Unknown or unhandled buffer method");
    return null;
  }

  private Result invokeBufferOpValidated(final Geometry geometry,
    final Object[] args) {
    Geometry result = null;

    result = invokeBuffer(geometry);

    // validate
    validate(geometry, result);

    /**
     * Return an empty GeometryCollection as the result.  
     * This allows the test case to avoid specifying an exact result
     */
    if (returnEmptyGC) {
      result = result.getGeometryFactory().geometryCollection();
    }
    return new GeometryResult(result);
  }

  private boolean isEmptyBufferExpected(final Geometry geom) {
    final boolean isNegativeBufferOfNonAreal = geom.getDimension() < 2
      && distance <= 0.0;
    return isNegativeBufferOfNonAreal;
  }

  private void parseArgs(final Object[] args) {
    argCount = args.length;
    distance = Double.parseDouble((String)args[0]);
    if (argCount >= 2) {
      quadSegments = Integer.parseInt((String)args[1]);
    }
    if (argCount >= 3) {
      endCapStyle = Integer.parseInt((String)args[2]);
    }
  }

  private void reportError(final String msg, final Coordinates loc) {
    String locStr = "";
    if (loc != null) {
      locStr = " at " + WktWriter.point(loc);
    }
    // System.out.println(msg);
    throw new RuntimeException(msg + locStr);
  }

  private void validate(final Geometry geom, final Geometry buffer) {
    if (isEmptyBufferExpected(geom)) {
      checkEmpty(buffer);
      return;
    }
    // simple containment check
    checkContainment(geom, buffer);

    // could also check distances of boundaries
    checkDistance(geom, distance, buffer);
    // need special check for negative buffers which disappear. Somehow need to
    // find maximum inner circle - via skeleton?
  }

}
