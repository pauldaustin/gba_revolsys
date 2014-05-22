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
package com.revolsys.jts.geom.impl;

import com.revolsys.gis.cs.projection.CoordinatesOperation;
import com.revolsys.gis.data.io.IteratorReader;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.jts.GeometryProperties;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.gis.model.data.equals.NumberEquals;
import com.revolsys.io.Reader;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.CoordinateSequenceComparator;
import com.revolsys.jts.geom.Dimension;
import com.revolsys.jts.geom.Envelope;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.Point;
import com.revolsys.jts.geom.PointList;
import com.revolsys.jts.geom.segment.LineStringSegment;
import com.revolsys.jts.geom.segment.Segment;
import com.revolsys.jts.geom.vertex.AbstractVertex;
import com.revolsys.jts.geom.vertex.LineStringVertex;
import com.revolsys.jts.geom.vertex.Vertex;
import com.revolsys.jts.operation.BoundaryOp;

/**
 *  Models an OGC-style <code>LineString</code>.
 *  A LineString consists of a sequence of two or more vertices,
 *  along with all points along the linearly-interpolated curves
 *  (line segments) between each 
 *  pair of consecutive vertices.
 *  Consecutive vertices may be equal.
 *  The line segments in the line may intersect each other (in other words, 
 *  the linestring may "curl back" in itself and self-intersect.
 *  Linestrings with exactly two identical points are invalid. 
 *  <p> 
 * A linestring must have either 0 or 2 or more points.  
 * If these conditions are not met, the constructors throw 
 * an {@link IllegalArgumentException}
 *
 *@version 1.7
 */
public abstract class AbstractLineString extends AbstractGeometry implements
  LineString {

  private static final long serialVersionUID = 3110669828065365560L;

  /**
   * Creates and returns a full copy of this {@link LineString} object.
   * (including all coordinates contained by it).
   *
   * @return a clone of this instance
   */
  @Override
  public AbstractLineString clone() {
    final AbstractLineString line = (AbstractLineString)super.clone();
    return line;
  }

  @Override
  public int compareToSameClass(final Geometry o) {
    final LineString line = (LineString)o;
    // MD - optimized implementation
    int i = 0;
    int j = 0;
    final int vertexCount = getVertexCount();
    while (i < vertexCount && j < line.getVertexCount()) {
      final int comparison = getPoint(i).compareTo(line.getPoint(j));
      if (comparison != 0) {
        return comparison;
      }
      i++;
      j++;
    }
    if (i < vertexCount) {
      return 1;
    }
    if (j < line.getVertexCount()) {
      return -1;
    }
    return 0;
  }

  @Override
  public int compareToSameClass(final Geometry o,
    final CoordinateSequenceComparator comp) {
    final LineString line = (LineString)o;
    return comp.compare(getCoordinatesList(), line.getCoordinatesList());
  }

  @Override
  protected BoundingBox computeBoundingBox() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (isEmpty()) {
      return new Envelope(geometryFactory);
    } else {
      return new Envelope(geometryFactory, vertices());
    }
  }

  protected double[] convertCoordinates(GeometryFactory geometryFactory) {
    final GeometryFactory sourceGeometryFactory = getGeometryFactory();
    final double[] coordinates = getCoordinates();
    if (isEmpty()) {
      return coordinates;
    } else {
      geometryFactory = getNonZeroGeometryFactory(geometryFactory);
      double[] targetCoordinates;
      final CoordinatesOperation coordinatesOperation = sourceGeometryFactory.getCoordinatesOperation(geometryFactory);
      if (coordinatesOperation == null) {
        return coordinates;
      } else {
        final int sourceAxisCount = getAxisCount();
        targetCoordinates = new double[sourceAxisCount * getVertexCount()];
        coordinatesOperation.perform(sourceAxisCount, coordinates,
          sourceAxisCount, targetCoordinates);
        return targetCoordinates;
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends Geometry> V copy(final GeometryFactory geometryFactory) {
    if (geometryFactory == null) {
      return (V)this.clone();
    } else if (isEmpty()) {
      return (V)geometryFactory.lineString();
    } else {
      final double[] coordinates = convertCoordinates(geometryFactory);
      final int axisCount = getAxisCount();
      return (V)geometryFactory.lineString(axisCount, coordinates);
    }
  }

  @Override
  public boolean doEquals(final int axisCount, final Geometry geometry) {
    final LineString line = (LineString)geometry;
    final int vertexCount = getVertexCount();
    final int vertexCount2 = line.getVertexCount();
    if (vertexCount == vertexCount2) {
      for (int i = 0; i < vertexCount2; i++) {
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
          final double value1 = getCoordinate(i, axisIndex);
          final double value2 = line.getCoordinate(i, axisIndex);
          if (!NumberEquals.equal(value1, value2)) {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(final int axisCount, final int vertexIndex,
    final Point point) {
    for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
      final double value = getCoordinate(vertexIndex, axisIndex);
      final double value2 = point.getCoordinate(axisIndex);
      if (!NumberEquals.equal(value, value2)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equalsExact(final Geometry other, final double tolerance) {
    if (!isEquivalentClass(other)) {
      return false;
    }
    final LineString otherLineString = (LineString)other;
    if (getVertexCount() != otherLineString.getVertexCount()) {
      return false;
    }
    for (int i = 0; i < getVertexCount(); i++) {
      final Point point = getPoint(i);
      final Point otherPoint = otherLineString.getPoint(i);
      if (!equal(point, otherPoint, tolerance)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equalsVertex(final int vertexIndex,
    final double... coordinates) {
    for (int axisIndex = 0; axisIndex < coordinates.length; axisIndex++) {
      final double coordinate = coordinates[axisIndex];
      final double matchCoordinate = getCoordinate(vertexIndex, axisIndex);
      if (!NumberEquals.equal(coordinate, matchCoordinate)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equalsVertex(final int vertexIndex, final int axisCount,
    Point point) {
    point = point.convert(getGeometryFactory(), axisCount);
    for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
      final double coordinate = point.getCoordinate(axisIndex);
      final double matchCoordinate = getCoordinate(vertexIndex, axisIndex);
      if (!NumberEquals.equal(coordinate, matchCoordinate)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equalsVertex(final int vertexIndex, Point point) {
    point = point.convert(getGeometryFactory());
    for (int axisIndex = 0; axisIndex < point.getAxisCount(); axisIndex++) {
      final double coordinate = point.getCoordinate(axisIndex);
      final double matchCoordinate = getCoordinate(vertexIndex, axisIndex);
      if (!NumberEquals.equal(coordinate, matchCoordinate)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets the boundary of this geometry.
   * The boundary of a lineal geometry is always a zero-dimensional geometry (which may be empty).
   *
   * @return the boundary geometry
   * @see Geometry#getBoundary
   */
  @Override
  public Geometry getBoundary() {
    return (new BoundaryOp(this)).getBoundary();
  }

  @Override
  public int getBoundaryDimension() {
    if (isClosed()) {
      return Dimension.FALSE;
    }
    return 0;
  }

  public abstract double[] getCoordinates();

  @Override
  public PointList getCoordinatesList() {
    if (isEmpty()) {
      return new DoubleCoordinatesList(getAxisCount());
    } else {
      return new DoubleCoordinatesList(getAxisCount(), getCoordinates());
    }
  }

  @Override
  public DataType getDataType() {
    return DataTypes.LINE_STRING;
  }

  @Override
  public int getDimension() {
    return 1;
  }

  @Override
  public Point getEndPoint() {
    if (isEmpty()) {
      return null;
    }
    return getPoint(getVertexCount() - 1);
  }

  /**
   *  Returns the length of this <code>LineString</code>
   *
   *@return the length of the linestring
   */
  @Override
  public double getLength() {
    final int vertexCount = getVertexCount();
    if (vertexCount <= 1) {
      return 0.0;
    } else {
      double len = 0.0;
      double x0 = getX(0);
      double y0 = getY(0);
      for (int i = 1; i < vertexCount; i++) {
        final double x1 = getX(i);
        final double y1 = getY(i);
        final double dx = x1 - x0;
        final double dy = y1 - y0;
        len += Math.sqrt(dx * dx + dy * dy);
        x0 = x1;
        y0 = y1;
      }
      return len;
    }
  }

  @Override
  public double getM(final int vertexIndex) {
    return getCoordinate(vertexIndex, 3);
  }

  @Override
  public Point getPoint() {
    if (isEmpty()) {
      return null;
    } else {
      return getPoint(0);
    }
  }

  @Override
  public final Point getPoint(int vertexIndex) {
    if (isEmpty()) {
      return null;
    } else {
      while (vertexIndex < 0) {
        vertexIndex += getVertexCount();
      }
      if (vertexIndex > getVertexCount()) {
        return null;
      } else {
        final int axisCount = getAxisCount();
        final double[] coordinates = new double[axisCount];
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
          coordinates[axisIndex] = getCoordinate(vertexIndex, axisIndex);
        }
        final GeometryFactory geometryFactory = getGeometryFactory();
        return geometryFactory.point(coordinates);
      }
    }
  }

  @Override
  public LineStringSegment getSegment(final int... segmentId) {
    if (segmentId.length == 1) {
      int segmentIndex = segmentId[0];
      final int vertexCount = getSegmentCount();
      if (segmentIndex < vertexCount) {
        while (segmentIndex < 0) {
          segmentIndex += vertexCount;
        }
        return new LineStringSegment(this, segmentIndex);
      }
    }
    return null;
  }

  @Override
  public int getSegmentCount() {
    if (isEmpty()) {
      return 0;
    } else {
      return getVertexCount() - 1;
    }
  }

  @Override
  public Point getStartPoint() {
    if (isEmpty()) {
      return null;
    } else {
      return getPoint(0);
    }
  }

  @Override
  public AbstractVertex getVertex(final int... vertexId) {
    if (vertexId.length == 1) {
      int vertexIndex = vertexId[0];
      final int vertexCount = getVertexCount();
      if (vertexIndex < vertexCount) {
        while (vertexIndex < 0) {
          vertexIndex += vertexCount;
        }
        return new LineStringVertex(this, vertexIndex);
      }
    }
    return null;
  }

  @Override
  public double getX(final int vertexIndex) {
    return getCoordinate(vertexIndex, 0);
  }

  @Override
  public double getY(final int vertexIndex) {
    return getCoordinate(vertexIndex, 1);
  }

  @Override
  public double getZ(final int vertexIndex) {
    return getCoordinate(vertexIndex, 2);
  }

  @Override
  public boolean intersects(final BoundingBox boundingBox) {
    if (isEmpty() || boundingBox.isEmpty()) {
      return false;
    } else {
      final GeometryFactory geometryFactory = boundingBox.getGeometryFactory()
        .convertAxisCount(2);
      double previousX = Double.NaN;
      double previousY = Double.NaN;

      final double[] coordinates = new double[2];
      for (final Vertex vertex : vertices()) {
        vertex.copyCoordinates(geometryFactory, coordinates);
        final double x = coordinates[0];
        final double y = coordinates[1];
        if (!Double.isNaN(previousX)) {
          if (boundingBox.intersects(previousX, previousY, x, y)) {
            return true;
          }
        }
        previousX = x;
        previousY = y;
      }
      return false;
    }
  }

  @Override
  public boolean isClockwise() {
    return !isCounterClockwise();
  }

  @Override
  public boolean isClosed() {
    if (isEmpty()) {
      return false;
    } else {
      final double x1 = getCoordinate(0, 0);
      final double xn = getCoordinate(-1, 0);
      if (x1 == xn) {
        final double y1 = getCoordinate(0, 1);
        final double yn = getCoordinate(-1, 1);
        if (y1 == yn) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isCounterClockwise() {
    final PointList points = getCoordinatesList();
    final boolean counterClockwise = points.isCounterClockwise();
    return counterClockwise;
  }

  @Override
  protected boolean isEquivalentClass(final Geometry other) {
    return other instanceof LineString;
  }

  @Override
  public boolean isRing() {
    return isClosed() && isSimple();
  }

  @Override
  public LineString merge(final LineString line) {
    final int axisCount = Math.max(getAxisCount(), line.getAxisCount());
    final int vertexCount1 = getVertexCount();
    final int vertexCount2 = line.getVertexCount();
    final int vertexCount = vertexCount1 + vertexCount2 - 1;
    final double[] coordinates = new double[vertexCount * axisCount];

    int newVertexCount = 0;
    final Point coordinates1Start = getVertex(0);
    final Point coordinates1End = getVertex(-1);
    final Point coordinates2Start = line.getVertex(0);
    final Point coordinates2End = line.getVertex(-1);
    if (coordinates1Start.equals(2, coordinates2End)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, line, 0,
        coordinates, 0, vertexCount2);
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 1,
        coordinates, newVertexCount, vertexCount1 - 1);
    } else if (coordinates2Start.equals(2, coordinates1End)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 0,
        coordinates, 0, vertexCount1);
      newVertexCount = CoordinatesListUtil.append(axisCount, line, 1,
        coordinates, newVertexCount, vertexCount2 - 1);
    } else if (coordinates1Start.equals(2, coordinates2Start)) {
      newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line, 0,
        coordinates, 0, vertexCount2);
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 1,
        coordinates, newVertexCount, vertexCount);
    } else if (coordinates1End.equals(2, coordinates2End)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 0,
        coordinates, newVertexCount, vertexCount);
      newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line, 1,
        coordinates, newVertexCount, vertexCount2 - 1);
    } else {
      throw new IllegalArgumentException("lines don't touch\n" + this + "\n"
        + line);

    }
    final GeometryFactory factory = getGeometryFactory();
    final LineString newLine = factory.lineString(axisCount, newVertexCount,
      coordinates);
    GeometryProperties.copyUserData(this, newLine);
    return line;
  }

  @Override
  public LineString merge(final Point point, final LineString line) {
    final int axisCount = Math.max(getAxisCount(), line.getAxisCount());
    final int vertexCount1 = getVertexCount();
    final int vertexCount2 = line.getVertexCount();
    final int vertexCount = vertexCount1 + vertexCount2 - 1;
    final double[] coordinates = new double[vertexCount * axisCount];

    int newVertexCount = 0;
    final Point coordinates1Start = getVertex(0);
    final Point coordinates1End = getVertex(-1);
    final Point coordinates2Start = line.getVertex(0);
    final Point coordinates2End = line.getVertex(-1);
    if (coordinates1Start.equals(2, coordinates2End)
      && coordinates1Start.equals(2, point)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, line, 0,
        coordinates, 0, vertexCount2);
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 1,
        coordinates, newVertexCount, vertexCount1 - 1);
    } else if (coordinates2Start.equals(2, coordinates1End)
      && coordinates2Start.equals(2, point)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 0,
        coordinates, 0, vertexCount1);
      newVertexCount = CoordinatesListUtil.append(axisCount, line, 1,
        coordinates, newVertexCount, vertexCount2 - 1);
    } else if (coordinates1Start.equals(2, coordinates2Start)
      && coordinates1Start.equals(2, point)) {
      newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line, 0,
        coordinates, 0, vertexCount2);
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 1,
        coordinates, newVertexCount, vertexCount);
    } else if (coordinates1End.equals(2, coordinates2End)
      && coordinates1End.equals(2, point)) {
      newVertexCount = CoordinatesListUtil.append(axisCount, this, 0,
        coordinates, newVertexCount, vertexCount);
      newVertexCount = CoordinatesListUtil.appendReverse(axisCount, line, 1,
        coordinates, newVertexCount, vertexCount2 - 1);
    } else {
      throw new IllegalArgumentException("lines don't touch\n" + this + "\n"
        + line);

    }
    final GeometryFactory factory = getGeometryFactory();
    final LineString newLine = factory.lineString(axisCount, newVertexCount,
      coordinates);
    GeometryProperties.copyUserData(this, newLine);
    return line;
  }

  @Override
  public LineString move(final double... deltas) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (deltas == null || isEmpty()) {
      return this;
    } else {
      final double[] coordinates = moveCoordinates(deltas);
      final int axisCount = getAxisCount();
      return geometryFactory.lineString(axisCount, coordinates);
    }
  }

  protected double[] moveCoordinates(final double... deltas) {
    final double[] coordinates = getCoordinates();
    final int vertexCount = getVertexCount();
    final int axisCount = getAxisCount();
    final int deltaCount = Math.min(deltas.length, getAxisCount());
    for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
      for (int axisIndex = 0; axisIndex < deltaCount; axisIndex++) {
        coordinates[vertexIndex * axisCount + axisIndex] += deltas[axisIndex];
      }
    }
    return coordinates;
  }

  /**
   * Normalizes a LineString.  A normalized linestring
   * has the first point which is not equal to it's reflected point
   * less than the reflected point.
   */
  @Override
  public LineString normalize() {
    final int vertexCount = getVertexCount();
    for (int i = 0; i < vertexCount / 2; i++) {
      final int j = vertexCount - 1 - i;
      final Vertex point1 = getVertex(i);
      final Vertex point2 = getVertex(j);
      // skip equal points on both ends
      if (!point1.equals(2, point2)) {
        if (point1.compareTo(point2) > 0) {
          return reverse();
        }
        return this;
      }
    }
    return this;
  }

  @Override
  public Iterable<Point> points() {
    return getCoordinatesList();
  }

  /**
   * Creates a {@link LineString} whose coordinates are in the reverse
   * order of this objects
   *
   * @return a {@link LineString} with coordinates in the reverse order
   */
  @Override
  public LineString reverse() {
    final PointList points = getCoordinatesList();
    final PointList reversePoints = points.reverse();
    final GeometryFactory geometryFactory = getGeometryFactory();
    final LineString reverseLine = geometryFactory.lineString(reversePoints);
    GeometryProperties.copyUserData(this, reverseLine);
    return reverseLine;
  }

  @Override
  public Reader<Segment> segments() {
    final LineStringSegment iterator = new LineStringSegment(this, -1);
    return new IteratorReader<Segment>(iterator);
  }

  @Override
  public LineString subLine(final int vertexCount) {
    return subLine(null, 0, vertexCount, null);
  }

  @Override
  public LineString subLine(final int vertexCount, final Point toPoint) {
    return subLine(null, 0, vertexCount, toPoint);
  }

  @Override
  public LineString subLine(final Point fromPoint, final int fromVertexIndex,
    int vertexCount, final Point toPoint) {
    if (fromVertexIndex + vertexCount > getVertexCount()) {
      vertexCount = getVertexCount() - fromVertexIndex;
    }
    int newVertexCount = vertexCount;
    final boolean hasFromPoint = fromPoint != null && !fromPoint.isEmpty();
    if (hasFromPoint) {
      newVertexCount++;
    }
    final boolean hasToPoint = toPoint != null && !toPoint.isEmpty();
    if (hasToPoint) {
      newVertexCount++;
    }
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (newVertexCount < 2) {
      return geometryFactory.lineString();
    } else {
      final int axisCount = getAxisCount();
      final double[] coordinates = new double[newVertexCount * axisCount];
      int vertexIndex = 0;
      if (hasFromPoint) {
        CoordinatesListUtil.setCoordinates(coordinates, axisCount,
          vertexIndex++, fromPoint);
      }
      CoordinatesListUtil.setCoordinates(coordinates, axisCount, vertexIndex,
        this, fromVertexIndex, vertexCount);
      vertexIndex += vertexCount;
      if (hasToPoint) {
        CoordinatesListUtil.setCoordinates(coordinates, axisCount,
          vertexIndex++, toPoint);
      }
      final LineString newLine = geometryFactory.lineString(axisCount,
        coordinates);
      GeometryProperties.copyUserData(this, newLine);
      return newLine;
    }

  }

  @Override
  public Reader<Vertex> vertices() {
    final LineStringVertex vertex = new LineStringVertex(this, -1);
    return vertex.reader();
  }

}