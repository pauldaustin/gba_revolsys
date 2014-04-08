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

import java.util.List;
import java.util.NoSuchElementException;

import com.revolsys.collection.AbstractIterator;

/**
 * Models a collection of {@link Point}s.
 * <p>
 * Any collection of Points is a valid MultiPoint.
 *
 *@version 1.7
 */
public class MultiPoint extends GeometryCollection implements Puntal {

  private static final long serialVersionUID = -8048474874175355449L;

  /**
   *@param  points          the <code>Point</code>s for this <code>MultiPoint</code>
   *      , or <code>null</code> or an empty array to create the empty geometry.
   *      Elements may be empty <code>Point</code>s, but not <code>null</code>s.
   */
  public MultiPoint(final Point[] points, final GeometryFactory factory) {
    super(points, factory);
  }

  /**
   *  Constructs a <code>MultiPoint</code>.
   *
   *@param  points          the <code>Point</code>s for this <code>MultiPoint</code>
   *      , or <code>null</code> or an empty array to create the empty geometry.
   *      Elements may be empty <code>Point</code>s, but not <code>null</code>s.
   *@param  precisionModel  the specification of the grid of allowable points
   *      for this <code>MultiPoint</code>
   *@param  SRID            the ID of the Spatial Reference System used by this
   *      <code>MultiPoint</code>
   * @deprecated Use GeometryFactory instead
   */
  @Deprecated
  public MultiPoint(final Point[] points, final PrecisionModel precisionModel,
    final int SRID) {
    super(points, new GeometryFactory(precisionModel, SRID));
  }

  @Override
  public boolean equalsExact(final Geometry other, final double tolerance) {
    if (!isEquivalentClass(other)) {
      return false;
    }
    return super.equalsExact(other, tolerance);
  }

  /**
   * Gets the boundary of this geometry.
   * Zero-dimensional geometries have no boundary by definition,
   * so an empty GeometryCollection is returned.
   *
   * @return an empty GeometryCollection
   * @see Geometry#getBoundary
   */
  @Override
  public Geometry getBoundary() {
    return getGeometryFactory().createGeometryCollection();
  }

  @Override
  public int getBoundaryDimension() {
    return Dimension.FALSE;
  }

  /**
   *  Returns the <code>Coordinate</code> at the given position.
   *
   *@param  n  the index of the <code>Coordinate</code> to retrieve, beginning
   *      at 0
   *@return    the <code>n</code>th <code>Coordinate</code>
   */
  protected Coordinate getCoordinate(final int n) {
    return ((Point)geometries[n]).getCoordinate();
  }

  @Override
  public int getDimension() {
    return 0;
  }

  @Override
  public String getGeometryType() {
    return "MultiPoint";
  }

  /**
   * @author Paul Austin <paul.austin@revolsys.com>
   */
  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  public <V extends Point> List<V> getPoints() {
    return (List)super.getGeometries();
  }

  @Override
  public boolean isValid() {
    return true;
  }

  /**
   * @author Paul Austin <paul.austin@revolsys.com>
   */
  @Override
  public Iterable<GeometryVertex> vertices() {
    return new AbstractIterator<GeometryVertex>() {
      private GeometryVertex vertex = new GeometryVertex(MultiPoint.this, 0);

      private int index = 0;

      @Override
      protected GeometryVertex getNext() throws NoSuchElementException {
        if (index < getNumGeometries()) {
          vertex.setVertexId(index);
          index++;
          return vertex;
        } else {
          vertex = null;
          throw new NoSuchElementException();
        }
      }
    };
  }

}