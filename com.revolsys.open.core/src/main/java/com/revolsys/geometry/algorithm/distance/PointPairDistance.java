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

package com.revolsys.geometry.algorithm.distance;

import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.Point;
import com.revolsys.record.io.format.wkt.EWktWriter;

/**
 * Contains a pair of points and the distance between them.
 * Provides methods to update with a new point pair with
 * either maximum or minimum distance.
 */
public class PointPairDistance {

  private static final Point EMPTY_POINT = GeometryFactory.DEFAULT.point();

  private double distance = Double.NaN;

  private boolean isNull = true;

  private final Point[] points = {
    EMPTY_POINT, EMPTY_POINT
  };

  public PointPairDistance() {
  }

  public double getDistance() {
    return this.distance;
  }

  public Point getPoint(final int i) {
    return this.points[i];
  }

  public Point[] getPoints() {
    return this.points;
  }

  public void initialize() {
    this.isNull = true;
  }

  public void initialize(final Point p0, final Point p1) {
    initialize(p0, p1, p0.distance(p1));
  }

  /**
   * Initializes the points, avoiding recomputing the distance.
   * @param p0
   * @param p1
   * @param distance the distance between p0 and p1
   */
  private void initialize(final Point p0, final Point p1, final double distance) {
    this.points[0] = p0.newPoint2D();
    this.points[1] = p1.newPoint2D();
    this.distance = distance;
    this.isNull = false;
  }

  public void setMaximum(final Point p0, final Point p1) {
    if (this.isNull) {
      initialize(p0, p1);
      return;
    }
    final double dist = p0.distance(p1);
    if (dist > this.distance) {
      initialize(p0, p1, dist);
    }
  }

  public void setMaximum(final PointPairDistance ptDist) {
    setMaximum(ptDist.points[0], ptDist.points[1]);
  }

  public void setMinimum(final Point p0, final Point p1) {
    if (this.isNull) {
      initialize(p0, p1);
      return;
    }
    final double dist = p0.distance(p1);
    if (dist < this.distance) {
      initialize(p0, p1, dist);
    }
  }

  public void setMinimum(final PointPairDistance ptDist) {
    setMinimum(ptDist.points[0], ptDist.points[1]);
  }

  @Override
  public String toString() {
    return EWktWriter.lineString(this.points[0], this.points[1]);
  }
}
