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
package com.revolsys.jts.geom.prep;

import java.util.List;

import com.revolsys.jts.algorithm.PointLocator;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.vertex.Vertex;
import com.revolsys.jts.noding.SegmentStringUtil;

/**
 * Computes the <tt>intersects</tt> spatial relationship predicate
 * for a target {@link PreparedLineString} relative to other {@link Geometry} classes.
 * Uses short-circuit tests and indexing to improve performance. 
 * 
 * @author Martin Davis
 *
 */
class PreparedLineStringIntersects {
  /**
   * Computes the intersects predicate between a {@link PreparedLineString}
   * and a {@link Geometry}.
   * 
   * @param prep the prepared linestring
   * @param geom a test geometry
   * @return true if the linestring intersects the geometry
   */
  public static boolean intersects(final PreparedLineString prep,
    final Geometry geom) {
    final PreparedLineStringIntersects op = new PreparedLineStringIntersects(
      prep);
    return op.intersects(geom);
  }

  protected PreparedLineString prepLine;

  /**
   * Creates an instance of this operation.
   * 
   * @param prepPoly the target PreparedLineString
   */
  public PreparedLineStringIntersects(final PreparedLineString prepLine) {
    this.prepLine = prepLine;
  }

  /**
   * Tests whether this geometry intersects a given geometry.
   * 
   * @param geom the test geometry
   * @return true if the test geometry intersects
   */
  public boolean intersects(final Geometry geom) {
    /**
     * If any segments intersect, obviously intersects = true
     */
    final List lineSegStr = SegmentStringUtil.extractSegmentStrings(geom);
    // only request intersection finder if there are segments (ie NOT for point
    // inputs)
    if (lineSegStr.size() > 0) {
      final boolean segsIntersect = prepLine.getIntersectionFinder()
        .intersects(lineSegStr);
      // MD - performance testing
      // boolean segsIntersect = false;
      if (segsIntersect) {
        return true;
      }
    }
    /**
     * For L/L case we are done
     */
    if (geom.getDimension() == 1) {
      return false;
    }

    /**
     * For L/A case, need to check for proper inclusion of the target in the test
     */
    if (geom.getDimension() == 2 && prepLine.isAnyTargetComponentInTest(geom)) {
      return true;
    }

    /** 
     * For L/P case, need to check if any points lie on line(s)
     */
    if (geom.getDimension() == 0) {
      return isAnyTestPointInTarget(geom);
    }

    return false;
  }

  /**
   * Tests whether any representative point of the test Geometry intersects
   * the target geometry.
   * Only handles test geometries which are Puntal (dimension 0)
   * 
   * @param geom a Puntal geometry to test
   * @return true if any point of the argument intersects the prepared geometry
   */
  protected boolean isAnyTestPointInTarget(final Geometry geometry) {
    /**
     * This could be optimized by using the segment index on the lineal target.
     * However, it seems like the L/P case would be pretty rare in practice.
     */
    final PointLocator locator = new PointLocator();
    final Geometry realGeometry = prepLine.getGeometry();
    for (final Vertex vertex : geometry.vertices()) {
      if (locator.intersects(vertex, realGeometry)) {
        return true;
      }
    }
    return false;
  }

}
