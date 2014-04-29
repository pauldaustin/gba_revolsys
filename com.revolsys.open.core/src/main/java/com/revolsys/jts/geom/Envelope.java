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
package com.revolsys.jts.geom;

import java.io.Serializable;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.ProjectedCoordinateSystem;
import com.revolsys.gis.cs.projection.CoordinatesListProjectionUtil;
import com.revolsys.gis.cs.projection.CoordinatesOperation;
import com.revolsys.gis.cs.projection.ProjectionFactory;
import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.model.coordinates.CoordinatesUtil;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.io.wkt.WktParser;
import com.revolsys.jts.util.EnvelopeUtil;
import com.revolsys.util.CollectionUtil;
import com.revolsys.util.MathUtil;

/**
 *  Defines a rectangular region of the 2D coordinate plane.
 *  It is often used to represent the bounding box of a {@link Geometry},
 *  e.g. the minimum and maximum x and y values of the {@link Coordinates}s.
 *  <p>
 *  Note that Envelopes support infinite or half-infinite regions, by using the values of
 *  <code>Double.POSITIVE_INFINITY</code> and <code>Double.NEGATIVE_INFINITY</code>.
 *  <p>
 *  When Envelope objects are created or initialized,
 *  the supplies extent values are automatically sorted into the correct order.
 *
 *@version 1.7
 */
public class Envelope implements Serializable, BoundingBox {

  static {
    ConvertUtils.register(new Converter() {

      @Override
      public Object convert(
        @SuppressWarnings("rawtypes") final Class paramClass,
        final Object paramObject) {
        if (paramObject == null) {
          return null;
        } else if (BoundingBox.class.isAssignableFrom(paramClass)) {
          if (paramObject instanceof BoundingBox) {
            return paramObject;
          } else {
            return create(paramObject.toString());
          }
        }
        return null;
      }
    }, BoundingBox.class);
  }

  /** The serialization version. */
  private static final long serialVersionUID = -810356856421113732L;

  public static BoundingBox create(final String wkt) {
    if (StringUtils.hasLength(wkt)) {
      GeometryFactory geometryFactory = null;
      final StringBuffer text = new StringBuffer(wkt);
      if (WktParser.hasText(text, "SRID=")) {
        final Integer srid = WktParser.parseInteger(text);
        if (srid != null) {
          geometryFactory = GeometryFactory.getFactory(srid, 2);
        }
        WktParser.hasText(text, ";");
      }
      if (WktParser.hasText(text, "BBOX(")) {
        final Double x1 = WktParser.parseDouble(text);
        if (WktParser.hasText(text, ",")) {
          final Double y1 = WktParser.parseDouble(text);
          WktParser.skipWhitespace(text);
          final Double x2 = WktParser.parseDouble(text);
          if (WktParser.hasText(text, ",")) {
            final Double y2 = WktParser.parseDouble(text);
            return new Envelope(geometryFactory, 2, x1, y1, x2, y2);
          } else {
            throw new IllegalArgumentException("Expecting a ',' not " + text);
          }

        } else {
          throw new IllegalArgumentException("Expecting a ',' not " + text);
        }
      } else if (WktParser.hasText(text, "BBOX EMPTY")) {
        return new Envelope(geometryFactory);
      }
    }

    return new Envelope();
  }

  public static boolean isEmpty(final BoundingBox boundingBox) {
    if (boundingBox == null) {
      return true;
    } else {
      return boundingBox.isEmpty();
    }
  }

  public static Envelope parse(final String bbox) {
    final String[] args = bbox.split(",");
    if (args.length == 4) {
      final double x1 = Double.valueOf(args[0]);
      final double y1 = Double.valueOf(args[1]);
      final double x2 = Double.valueOf(args[2]);
      final double y2 = Double.valueOf(args[3]);
      return new Envelope(GeometryFactory.getFactory(4326), 2, x1, y1, x2, y2);
    } else {
      throw new IllegalArgumentException(
        "BBOX must have match <minX>,<minY>,<maxX>,<maxY> not " + bbox);
    }
  }

  private final double[] bounds;

  private GeometryFactory geometryFactory;

  public Envelope() {
    this.bounds = null;
  }

  public Envelope(final Coordinates... points) {
    this(null, points);
  }

  /**
   * Construct a new Bounding Box.
   * 
   * @param geometryFactory The geometry factory.
   */
  public Envelope(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
    this.bounds = null;
  }

  public Envelope(final GeometryFactory geometryFactory,
    final Coordinates... points) {
    this(geometryFactory, CollectionUtil.toList(points));
  }

  public Envelope(final GeometryFactory geometryFactory, final int axisCount,
    final double... bounds) {
    this.geometryFactory = geometryFactory;
    if (bounds == null || bounds.length == 0 || axisCount < 1) {
      this.bounds = null;
    } else if (bounds.length % axisCount == 0) {
      this.bounds = EnvelopeUtil.createBounds(axisCount);
      EnvelopeUtil.expand(geometryFactory, this.bounds, bounds);
    } else {
      throw new IllegalArgumentException("Expecting a multiple of " + axisCount
        + " not " + bounds.length);
    }
  }

  public Envelope(final GeometryFactory geometryFactory,
    final Iterable<? extends Coordinates> points) {
    this.geometryFactory = geometryFactory;
    double[] bounds = null;
    if (points != null) {
      for (final Coordinates point : points) {
        if (point != null) {
          if (bounds == null) {
            bounds = EnvelopeUtil.createBounds(geometryFactory, point);
          } else {
            EnvelopeUtil.expand(geometryFactory, bounds, point);
          }
        }
      }
    }
    this.bounds = bounds;
  }

  public Envelope(final int axisCount, final double... bounds) {
    this(null, axisCount, bounds);
  }

  public Envelope(final Iterable<? extends Coordinates> points) {
    this(null, points);
  }

  @Override
  public BoundingBox clipToCoordinateSystem() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
    if (coordinateSystem == null) {
      return this;
    } else {
      final BoundingBox areaBoundingBox = coordinateSystem.getAreaBoundingBox();
      return intersection(areaBoundingBox);
    }
  }

  /**
   * <p>Bounding boxes are immutable so clone returns this.</p>
   * 
   * @return this
   */
  @Override
  public BoundingBox clone() {
    return this;
  }

  @Override
  public BoundingBox convert(final GeometryFactory geometryFactory) {
    final GeometryFactory factory = getGeometryFactory();
    if (factory == null) {
      if (geometryFactory == null) {
        return this;
      } else {
        return new Envelope(geometryFactory, getAxisCount(), getBounds());
      }
    } else if (geometryFactory == null) {
      return new Envelope(getAxisCount(), getBounds());
    } else if (factory.equals(geometryFactory)) {
      return this;
    } else if (isEmpty()) {
      return new Envelope(geometryFactory);
    } else {
      final CoordinatesOperation operation = ProjectionFactory.getCoordinatesOperation(
        factory, geometryFactory);
      if (operation != null) {

        double xStep = getWidth() / 10;
        double yStep = getHeight() / 10;
        final double scaleXY = geometryFactory.getScaleXY();
        if (scaleXY > 0) {
          if (xStep < 1 / scaleXY) {
            xStep = 1 / scaleXY;
          }
          if (yStep < 1 / scaleXY) {
            yStep = 1 / scaleXY;
          }
        }

        final int axisCount = getAxisCount();

        final double minX = getMinX();
        final double maxX = getMaxX();
        final double minY = getMinY();
        final double maxY = getMaxY();

        final double[] bounds = getBounds();
        bounds[0] = Double.NaN;
        bounds[1] = Double.NaN;
        bounds[axisCount] = Double.NaN;
        bounds[axisCount + 1] = Double.NaN;

        final double[] to = new double[2];
        expand(geometryFactory, bounds, operation, to, minX, minY);
        expand(geometryFactory, bounds, operation, to, minX, maxY);
        expand(geometryFactory, bounds, operation, to, minX, minY);
        expand(geometryFactory, bounds, operation, to, maxX, minY);

        if (xStep != 0) {
          for (double x = minX + xStep; x < maxX; x += xStep) {
            expand(geometryFactory, bounds, operation, to, x, minY);
            expand(geometryFactory, bounds, operation, to, x, maxY);
          }
        }
        if (yStep != 0) {
          for (double y = minY + yStep; y < maxY; y += yStep) {
            expand(geometryFactory, bounds, operation, to, minX, y);
            expand(geometryFactory, bounds, operation, to, maxX, y);
          }
        }
        return new Envelope(geometryFactory, axisCount, bounds);
      } else {
        return this;
      }
    }
  }

  /**
   * Tests if the <code>Envelope other</code>
   * lies wholely inside this <code>Envelope</code> (inclusive of the boundary).
   *
   *@param  other the <code>Envelope</code> to check
   *@return true if this <code>Envelope</code> covers the <code>other</code> 
   */
  @Override
  public boolean covers(final BoundingBox other) {
    if (other == null || isEmpty() || other.isEmpty()) {
      return false;
    } else {
      final double minX = getMinX();
      final double minY = getMinY();
      final double maxX = getMaxX();
      final double maxY = getMaxY();

      return other.getMinX() >= minX && other.getMaxX() <= maxX
        && other.getMinY() >= minY && other.getMaxY() <= maxY;
    }
  }

  /**
   * Tests if the given point lies in or on the envelope.
   *
   *@param  p  the point which this <code>Envelope</code> is
   *      being checked for containing
   *@return    <code>true</code> if the point lies in the interior or
   *      on the boundary of this <code>Envelope</code>.
   */
  @Override
  public boolean covers(final Coordinates p) {
    final double x = p.getX();
    final double y = p.getY();
    return covers(x, y);
  }

  /**
   * Tests if the given point lies in or on the envelope.
   *
   *@param  x  the x-coordinate of the point which this <code>Envelope</code> is
   *      being checked for containing
   *@param  y  the y-coordinate of the point which this <code>Envelope</code> is
   *      being checked for containing
   *@return    <code>true</code> if <code>(x, y)</code> lies in the interior or
   *      on the boundary of this <code>Envelope</code>.
   */
  @Override
  public boolean covers(final double x, final double y) {
    if (isEmpty()) {
      return false;
    } else {
      final double minX = getMinX();
      final double minY = getMinY();
      final double maxX = getMaxX();
      final double maxY = getMaxY();

      return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
  }

  @Override
  public boolean covers(final Geometry geometry) {
    if (geometry == null) {
      return false;
    } else {
      final BoundingBox boundingBox = geometry.getBoundingBox();
      return covers(boundingBox);
    }
  }

  @Override
  public boolean covers(final Point point) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final Point projectedPoint = point.convert(geometryFactory);
    final boolean contains = covers((Coordinates)projectedPoint);
    return contains;
  }

  /**
   * Computes the distance between this and another
   * <code>Envelope</code>.
   * The distance between overlapping Envelopes is 0.  Otherwise, the
   * distance is the Euclidean distance between the closest points.
   */
  @Override
  public double distance(BoundingBox boundingBox) {
    boundingBox = boundingBox.convert(getGeometryFactory());
    if (intersects(boundingBox)) {
      return 0;
    } else {
      final double minX = getMinX();
      final double minY = getMinY();
      final double maxX = getMaxX();
      final double maxY = getMaxY();

      double dx = 0.0;
      if (maxX < boundingBox.getMinX()) {
        dx = boundingBox.getMinX() - maxX;
      } else if (minX > boundingBox.getMaxX()) {
        dx = minX - boundingBox.getMaxX();
      }

      double dy = 0.0;
      if (maxY < boundingBox.getMinY()) {
        dy = boundingBox.getMinY() - maxY;
      } else if (minY > boundingBox.getMaxY()) {
        dy = minY - boundingBox.getMaxY();
      }

      // if either is zero, the envelopes overlap either vertically or
      // horizontally
      if (dx == 0.0) {
        return dy;
      }
      if (dy == 0.0) {
        return dx;
      }
      return Math.sqrt(dx * dx + dy * dy);
    }
  }

  @Override
  public double distance(final Geometry geometry) {
    final BoundingBox boundingBox = geometry.getBoundingBox();
    return distance(boundingBox);
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof BoundingBox) {
      final BoundingBox boundingBox = (BoundingBox)other;
      if (isEmpty()) {
        return boundingBox.isEmpty();
      } else if (boundingBox.isEmpty()) {
        return false;
      } else if (getSrid() == boundingBox.getSrid()) {
        if (getMaxX() == boundingBox.getMaxX()) {
          if (getMaxY() == boundingBox.getMaxY()) {
            if (getMinX() == boundingBox.getMinX()) {
              if (getMinY() == boundingBox.getMinY()) {
                return true;
              }
            }
          }
        }

      }
    }
    return false;
  }

  @Override
  public BoundingBox expand(final Coordinates coordinates) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (isEmpty()) {
      return new Envelope(geometryFactory, coordinates);
    } else {
      final double x = coordinates.getX();
      final double y = coordinates.getY();

      double minX = getMinX();
      double maxX = getMaxX();
      double minY = getMinY();
      double maxY = getMaxY();

      if (x < minX) {
        minX = x;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (y > maxY) {
        maxY = y;
      }
      return new Envelope(geometryFactory, 2, minX, minY, maxX, maxY);
    }
  }

  /**
   * Return a new bounding box expanded by delta.
   * 
   * @param delta
   * @return
   */
  @Override
  public BoundingBox expand(final double delta) {
    return expand(delta, delta);
  }

  /**
   * Return a new bounding box expanded by deltaX, deltaY.
   * 
   * @param delta
   * @return
   */
  @Override
  public BoundingBox expand(final double deltaX, final double deltaY) {
    if (isEmpty() || (deltaX == 0 && deltaY == 0)) {
      return this;
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      final double x1 = getMinX() - deltaX;
      final double x2 = getMaxX() + deltaX;
      final double y1 = getMinY() - deltaY;
      final double y2 = getMaxY() + deltaY;

      if (x1 > x2 || y1 > y2) {
        return new Envelope(geometryFactory);
      } else {
        return new Envelope(geometryFactory, 2, x1, y1, x2, y2);
      }
    }
  }

  private void expand(final GeometryFactory geometryFactory,
    final double[] bounds, final CoordinatesOperation operation,
    final double[] to, final double... from) {

    operation.perform(2, from, 2, to);
    EnvelopeUtil.expand(geometryFactory, bounds, to);
  }

  @Override
  public BoundingBox expandPercent(final double factor) {
    return expandPercent(factor, factor);
  }

  @Override
  public BoundingBox expandPercent(final double factorX, final double factorY) {
    if (isEmpty()) {
      return this;
    } else {
      final double deltaX = getWidth() * factorX / 2;
      final double deltaY = getHeight() * factorY / 2;
      return expand(deltaX, deltaY);
    }
  }

  @Override
  public BoundingBox expandToInclude(final BoundingBox other) {
    if (other.isEmpty()) {
      return this;
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      final BoundingBox convertedOther = other.convert(geometryFactory);
      if (isEmpty()) {
        return convertedOther;
      } else if (covers(convertedOther)) {
        return this;
      } else {
        final double minX = Math.min(getMinX(), convertedOther.getMinX());
        final double maxX = Math.max(getMaxX(), convertedOther.getMaxX());
        final double minY = Math.min(getMinY(), convertedOther.getMinY());
        final double maxY = Math.max(getMaxY(), convertedOther.getMaxY());
        return new Envelope(geometryFactory, 2, minX, minY, maxX, maxY);
      }
    }
  }

  @Override
  public BoundingBox expandToInclude(final DataObject object) {
    if (object != null) {
      final Geometry geometry = object.getGeometryValue();
      return expandToInclude(geometry);
    }
    return this;
  }

  @Override
  public BoundingBox expandToInclude(final Geometry geometry) {
    if (geometry == null || geometry.isEmpty()) {
      return this;
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      final Geometry convertedGeometry = geometry.convert(geometryFactory);
      final BoundingBox box = convertedGeometry.getBoundingBox();
      return expandToInclude(box);
    }
  }

  public BoundingBox expandToInclude(final Point point) {
    return expandToInclude((Geometry)point);
  }

  /**
   * Gets the area of this envelope.
   * 
   * @return the area of the envelope
   * @return 0.0 if the envelope is null
   */
  @Override
  public double getArea() {
    if (getAxisCount() < 2) {
      return 0;
    } else {
      final double width = getWidth();
      final double height = getHeight();
      return width * height;
    }
  }

  /**
   * Get the aspect ratio x:y.
   * 
   * @return The aspect ratio.
   */
  @Override
  public double getAspectRatio() {
    final double width = getWidth();
    final double height = getHeight();
    final double aspectRatio = width / height;
    return aspectRatio;
  }

  @Override
  public int getAxisCount() {
    if (bounds == null) {
      return 0;
    } else {
      return bounds.length / 2;
    }
  }

  public Point getBottomLeftPoint() {
    return getGeometryFactory().point(getMinX(), getMinY());
  }

  public Point getBottomRightPoint() {
    return getGeometryFactory().point(getMaxX(), getMinY());
  }

  @Override
  public double[] getBounds() {
    if (bounds == null) {
      return bounds;
    } else {
      return bounds.clone();
    }
  }

  @Override
  public Point getCentre() {
    if (isEmpty()) {
      return geometryFactory.point();
    } else {
      final double centreX = getCentreX();
      final double centreY = getCentreY();
      return geometryFactory.point(centreX, centreY);
    }
  }

  @Override
  public double getCentreX() {
    return getMinX() + (getWidth() / 2);
  }

  @Override
  public double getCentreY() {
    return getMinY() + (getHeight() / 2);
  }

  /**
   * Get the geometry factory.
   * 
   * @return The geometry factory.
   */
  @Override
  public CoordinateSystem getCoordinateSystem() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (geometryFactory == null) {
      return null;
    } else {
      return geometryFactory.getCoordinateSystem();
    }
  }

  @Override
  public Coordinates getCornerPoint(int index) {
    final double minX = getMinX();
    final double maxX = getMaxX();
    final double minY = getMinY();
    final double maxY = getMaxY();
    index = index % 4;
    switch (index) {
      case 0:
        return new DoubleCoordinates(maxX, minY);
      case 1:
        return new DoubleCoordinates(minX, minY);
      case 2:
        return new DoubleCoordinates(minX, maxY);
      default:
        return new DoubleCoordinates(maxX, maxY);
    }
  }

  @Override
  public CoordinatesList getCornerPoints() {
    final double minX = getMinX();
    final double maxX = getMaxX();
    final double minY = getMinY();
    final double maxY = getMaxY();
    return new DoubleCoordinatesList(2, maxX, minY, minX, minY, minX, maxY,
      maxX, maxY);
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return geometryFactory;
  }

  /**
   *  Returns the difference between the maximum and minimum y values.
   *
   *@return    max y - min y, or 0 if this is a null <code>Envelope</code>
   */
  @Override
  public double getHeight() {
    if (getAxisCount() < 2) {
      return 0;
    } else {
      return getMaxY() - getMinY();
    }
  }

  @Override
  public Measure<Length> getHeightLength() {
    final double height = getHeight();
    final CoordinateSystem coordinateSystem = getCoordinateSystem();
    if (coordinateSystem == null) {
      return Measure.valueOf(height, SI.METRE);
    } else {
      return Measure.valueOf(height, coordinateSystem.getLengthUnit());
    }
  }

  @Override
  public double getMax(final int axisIndex) {
    if (bounds == null || axisIndex >= getAxisCount()) {
      return Double.NaN;
    } else {
      return EnvelopeUtil.getMax(bounds, axisIndex);
    }
  }

  @Override
  public <Q extends Quantity> Measurable<Q> getMaximum(final int axisIndex) {
    final Unit<Q> unit = getUnit();
    final double max = this.getMax(axisIndex);
    return Measure.valueOf(max, unit);
  }

  @Override
  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  public <Q extends Quantity> double getMaximum(final int axisIndex,
    final Unit convertUnit) {
    final Measurable<Quantity> max = getMaximum(axisIndex);
    return max.doubleValue(convertUnit);
  }

  /**
   *  Returns the <code>Envelope</code>s maximum x-value. min x > max x
   *  indicates that this is a null <code>Envelope</code>.
   *
   *@return    the maximum x-coordinate
   */
  @Override
  public double getMaxX() {
    return getMax(0);
  }

  /**
   *  Returns the <code>Envelope</code>s maximum y-value. min y > max y
   *  indicates that this is a null <code>Envelope</code>.
   *
   *@return    the maximum y-coordinate
   */
  @Override
  public double getMaxY() {
    return getMax(1);
  }

  @Override
  public double getMin(final int axisIndex) {
    if (bounds == null) {
      return Double.NaN;
    } else {
      return EnvelopeUtil.getMin(bounds, axisIndex);
    }
  }

  @Override
  public <Q extends Quantity> Measurable<Q> getMinimum(final int axisIndex) {
    final Unit<Q> unit = getUnit();
    final double min = this.getMin(axisIndex);
    return Measure.valueOf(min, unit);
  }

  @Override
  @SuppressWarnings({
    "rawtypes", "unchecked"
  })
  public <Q extends Quantity> double getMinimum(final int axisIndex,
    final Unit convertUnit) {
    final Measurable<Quantity> min = getMinimum(axisIndex);
    return min.doubleValue(convertUnit);
  }

  /**
   *  Returns the <code>Envelope</code>s minimum x-value. min x > max x
   *  indicates that this is a null <code>Envelope</code>.
   *
   *@return    the minimum x-coordinate
   */
  @Override
  public double getMinX() {
    return getMin(0);
  }

  /**
   *  Returns the <code>Envelope</code>s minimum y-value. min y > max y
   *  indicates that this is a null <code>Envelope</code>.
   *
   *@return    the minimum y-coordinate
   */
  @Override
  public double getMinY() {
    return getMin(1);
  }

  @Override
  public int getSrid() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    if (geometryFactory == null) {
      return 0;
    } else {
      return geometryFactory.getSrid();
    }
  }

  @Override
  public Point getTopLeftPoint() {
    return getGeometryFactory().point(getMinX(), getMaxY());
  }

  public Point getTopRightPoint() {
    return getGeometryFactory().point(getMaxX(), getMaxY());
  }

  @SuppressWarnings("unchecked")
  private <Q extends Quantity> Unit<Q> getUnit() {
    final CoordinateSystem coordinateSystem = getCoordinateSystem();
    if (coordinateSystem == null) {
      return (Unit<Q>)SI.METRE;
    } else {
      return coordinateSystem.<Q> getUnit();
    }
  }

  /**
   *  Returns the difference between the maximum and minimum x values.
   *
   *@return    max x - min x, or 0 if this is a null <code>Envelope</code>
   */
  @Override
  public double getWidth() {
    if (getAxisCount() < 2) {
      return 0;
    } else {
      final double minX = getMinX();
      final double maxX = getMaxX();

      return maxX - minX;
    }
  }

  @Override
  public Measure<Length> getWidthLength() {
    final double width = getWidth();
    final CoordinateSystem coordinateSystem = getCoordinateSystem();
    if (coordinateSystem == null) {
      return Measure.valueOf(width, SI.METRE);
    } else {
      return Measure.valueOf(width, coordinateSystem.getLengthUnit());
    }
  }

  @Override
  public int hashCode() {
    final double minX = getMinX();
    final double minY = getMinY();
    final double maxX = getMaxX();
    final double maxY = getMaxY();
    int result = 17;
    result = 37 * result + CoordinatesUtil.hashCode(minX);
    result = 37 * result + CoordinatesUtil.hashCode(maxX);
    result = 37 * result + CoordinatesUtil.hashCode(minY);
    result = 37 * result + CoordinatesUtil.hashCode(maxY);
    return result;
  }

  /**
   * Computes the intersection of two {@link Envelope}s.
   *
   * @param env the envelope to intersect with
   * @return a new BoundingBox representing the intersection of the envelopes (this will be
   * the null envelope if either argument is null, or they do not intersect
   */
  @Override
  public BoundingBox intersection(final BoundingBox boundingBox) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final BoundingBox convertedBoundingBox = boundingBox.convert(geometryFactory);
    if (isEmpty() || convertedBoundingBox.isEmpty()
      || !intersects(convertedBoundingBox)) {
      return new Envelope(geometryFactory);
    } else {
      final double intMinX = Math.max(getMinX(), convertedBoundingBox.getMinX());
      final double intMinY = Math.max(getMinY(), convertedBoundingBox.getMinY());
      final double intMaxX = Math.min(getMaxX(), convertedBoundingBox.getMaxX());
      final double intMaxY = Math.min(getMaxY(), convertedBoundingBox.getMaxY());
      return new Envelope(geometryFactory, 2, intMinX, intMinY, intMaxX,
        intMaxY);
    }
  }

  /**
   *  Check if the region defined by <code>other</code>
   *  overlaps (intersects) the region of this <code>Envelope</code>.
   *
   *@param  other  the <code>Envelope</code> which this <code>Envelope</code> is
   *          being checked for overlapping
   *@return        <code>true</code> if the <code>Envelope</code>s overlap
   */
  @Override
  public boolean intersects(final BoundingBox other) {
    if (isEmpty() || other.isEmpty()) {
      return false;
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      final BoundingBox convertedBoundingBox = other.convert(geometryFactory);
      final double minX = getMinX();
      final double minY = getMinY();
      final double maxX = getMaxX();
      final double maxY = getMaxY();

      return !(convertedBoundingBox.getMinX() > maxX
        || convertedBoundingBox.getMaxX() < minX
        || convertedBoundingBox.getMinY() > maxY || convertedBoundingBox.getMaxY() < minY);
    }
  }

  /**
   *  Check if the point <code>p</code>
   *  overlaps (lies inside) the region of this <code>Envelope</code>.
   *
   *@param  p  the <code>Coordinate</code> to be tested
   *@return        <code>true</code> if the point overlaps this <code>Envelope</code>
   */
  @Override
  public boolean intersects(final Coordinates point) {
    if (point == null) {
      return false;
    } else {
      final double x = point.getX();
      final double y = point.getY();
      return this.intersects(x, y);
    }
  }

  /**
   *  Check if the point <code>(x, y)</code>
   *  overlaps (lies inside) the region of this <code>Envelope</code>.
   *
   *@param  x  the x-ordinate of the point
   *@param  y  the y-ordinate of the point
   *@return        <code>true</code> if the point overlaps this <code>Envelope</code>
   */
  @Override
  public boolean intersects(final double x, final double y) {
    if (isEmpty()) {
      return false;
    } else {
      final double minX = getMinX();
      final double minY = getMinY();
      final double maxX = getMaxX();
      final double maxY = getMaxY();
      return !(x > maxX || x < minX || y > maxY || y < minY);
    }
  }

  public boolean intersects(final Geometry geometry) {
    final BoundingBox boundingBox = geometry.getBoundingBox();
    return intersects(boundingBox);
  }

  @Override
  public boolean isEmpty() {
    final double minX = getMinX();
    final double maxX = getMaxX();
    if (Double.isNaN(minX)) {
      return true;
    } else if (Double.isNaN(maxX)) {
      return true;
    } else {
      return maxX < minX;
    }
  }

  /**
   * <p>Create a new BoundingBox by moving the min/max x coordinates by xDisplacement and
   * the min/max y coordinates by yDisplacement. If the bounding box is null or the xDisplacement
   * and yDisplacement are 0 then this bounding box will be returned.</p>
   * 
   * @param xDisplacement The distance to move the min/max x coordinates.
   * @param yDisplacement The distance to move the min/max y coordinates.
   * @return The moved bounding box.
   */
  @Override
  public BoundingBox move(final double xDisplacement, final double yDisplacement) {
    if (isEmpty() || (xDisplacement == 0 && yDisplacement == 0)) {
      return this;
    } else {
      final GeometryFactory geometryFactory = getGeometryFactory();
      final double x1 = getMinX() + xDisplacement;
      final double x2 = getMaxX() + xDisplacement;
      final double y1 = getMinY() + yDisplacement;
      final double y2 = getMaxY() + yDisplacement;
      return new Envelope(geometryFactory, 2, x1, y1, x2, y2);
    }
  }

  @Override
  public Geometry toGeometry() {
    GeometryFactory geometryFactory = getGeometryFactory();
    if (geometryFactory == null) {
      geometryFactory = GeometryFactory.getFactory(0, 2);
    }
    if (isEmpty()) {
      return geometryFactory.point();
    } else {
      final double minX = getMinX();
      final double minY = getMinY();
      final double maxX = getMaxX();
      final double maxY = getMaxY();
      final double width = getWidth();
      final double height = getHeight();
      if (width == 0 && height == 0) {
        return geometryFactory.point(minX, minY);
      } else if (width == 0 || height == 0) {
        return geometryFactory.lineString(2, minX, minY, maxX, maxY);
      } else {
        return geometryFactory.polygon(geometryFactory.linearRing(2, minX,
          minY, minX, maxY, maxX, maxY, maxX, minY, minX, minY));
      }
    }
  }

  @Override
  public Polygon toPolygon() {
    return toPolygon(100, 100);

  }

  @Override
  public Polygon toPolygon(final GeometryFactory factory) {
    return toPolygon(factory, 100, 100);
  }

  @Override
  public Polygon toPolygon(final GeometryFactory factory, final int numSegments) {
    return toPolygon(factory, numSegments, numSegments);
  }

  @Override
  public Polygon toPolygon(GeometryFactory geometryFactory, int numX, int numY) {
    if (isEmpty()) {
      return geometryFactory.polygon();
    } else {
      final GeometryFactory factory = getGeometryFactory();
      if (geometryFactory == null) {
        if (factory == null) {
          geometryFactory = GeometryFactory.getFactory(0, 2);
        } else {
          geometryFactory = factory;
        }
      }
      try {
        double minStep = 0.00001;
        final CoordinateSystem coordinateSystem = geometryFactory.getCoordinateSystem();
        if (coordinateSystem instanceof ProjectedCoordinateSystem) {
          minStep = 1;
        } else {
          minStep = 0.00001;
        }

        double xStep;
        final double width = getWidth();
        if (numX <= 1) {
          numX = 1;
          xStep = width;
        } else {
          xStep = width / numX;
          if (xStep < minStep) {
            xStep = minStep;
          }
          numX = Math.max(1, (int)Math.ceil(width / xStep));
        }

        double yStep;
        if (numY <= 1) {
          numY = 1;
          yStep = getHeight();
        } else {
          yStep = getHeight() / numY;
          if (yStep < minStep) {
            yStep = minStep;
          }
          numY = Math.max(1, (int)Math.ceil(getHeight() / yStep));
        }

        final double minX = getMinX();
        final double maxX = getMaxX();
        final double minY = getMinY();
        final double maxY = getMaxY();
        final int numCoordinates = 1 + 2 * (numX + numY);
        CoordinatesList coordinates = new DoubleCoordinatesList(numCoordinates,
          2);
        int i = 0;

        coordinates.setX(i, maxX);
        coordinates.setY(i, minY);
        i++;
        for (int j = 0; j < numX - 1; j++) {
          coordinates.setX(i, maxX - j * xStep);
          coordinates.setY(i, minY);
          i++;
        }
        coordinates.setX(i, minX);
        coordinates.setY(i, minY);
        i++;

        for (int j = 0; j < numY - 1; j++) {
          coordinates.setX(i, minX);
          coordinates.setY(i, minY + j * yStep);
          i++;
        }
        coordinates.setX(i, minX);
        coordinates.setY(i, maxY);
        i++;

        for (int j = 0; j < numX - 1; j++) {
          coordinates.setX(i, minX + j * xStep);
          coordinates.setY(i, maxY);
          i++;
        }

        coordinates.setX(i, maxX);
        coordinates.setY(i, maxY);
        i++;

        for (int j = 0; j < numY - 1; j++) {
          coordinates.setX(i, maxX);
          coordinates.setY(i, minY + (numY - j) * yStep);
          i++;
        }
        coordinates.setX(i, maxX);
        coordinates.setY(i, minY);

        if (geometryFactory != factory && factory != null) {
          coordinates = CoordinatesListProjectionUtil.perform(coordinates,
            factory.getCoordinateSystem(), coordinateSystem);
        }
        final Polygon polygon = geometryFactory.polygon(coordinates);
        return polygon;
      } catch (final IllegalArgumentException e) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to convert to polygon: " + this, e);
        return geometryFactory.polygon();
      }
    }
  }

  @Override
  public Polygon toPolygon(final int numSegments) {
    return toPolygon(numSegments, numSegments);
  }

  @Override
  public Polygon toPolygon(final int numX, final int numY) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    return toPolygon(geometryFactory, numX, numY);
  }

  @Override
  public String toString() {
    final StringBuffer s = new StringBuffer();
    final int srid = getSrid();
    if (srid > 0) {
      s.append("SRID=");
      s.append(srid);
      s.append(";");
    }
    if (isEmpty()) {
      s.append("BBOX EMPTY");
    } else {
      s.append("BBOX");
      final int axisCount = getAxisCount();
      if (axisCount == 3) {
        s.append(" Z");
      } else if (axisCount == 4) {
        s.append(" ZM");
      } else if (axisCount != 2) {
        s.append(" ");
        s.append(axisCount);
      }
      s.append("(");
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        if (axisIndex > 0) {
          s.append(',');
        }
        s.append(MathUtil.toString(getMin(axisIndex)));
      }
      s.append(' ');
      for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
        if (axisIndex > 0) {
          s.append(',');
        }
        s.append(MathUtil.toString(getMax(axisIndex)));
      }
      s.append(')');
    }
    return s.toString();
  }

}
