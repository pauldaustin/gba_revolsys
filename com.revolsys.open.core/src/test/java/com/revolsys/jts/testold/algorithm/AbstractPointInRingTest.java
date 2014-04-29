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
package com.revolsys.jts.testold.algorithm;

import junit.framework.TestCase;

import com.revolsys.jts.geom.Coordinate;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.Location;

/**
 * Tests PointInRing algorithms
 *
 * @version 1.7
 */
public abstract class AbstractPointInRingTest extends TestCase {

  public static final String comb = "POLYGON ((0 0, 0 10, 4 5, 6 10, 7 5, 9 10, 10 5, 13 5, 15 10, 16 3, 17 10, 18 3, 25 10, 30 10, 30 0, 15 0, 14 5, 13 0, 9 0, 8 5, 6 0, 0 0))";

  public static final String repeatedPts = "POLYGON ((0 0, 0 10, 2 5, 2 5, 2 5, 2 5, 2 5, 3 10, 6 10, 8 5, 8 5, 8 5, 8 5, 10 10, 10 5, 10 5, 10 5, 10 5, 10 0, 0 0))";

  public AbstractPointInRingTest(final String name) {
    super(name);
  }

  abstract protected void runPtInRing(Location expectedLoc, Coordinates pt,
    String wkt) throws Exception;

  public void testBox() throws Exception {
    runPtInRing(Location.INTERIOR, new Coordinate(10.0, 10.0,
      Coordinates.NULL_ORDINATE), "POLYGON ((0 0, 0 20, 20 20, 20 0, 0 0))");
  }

  public void testComb() throws Exception {
    runPtInRing(Location.BOUNDARY, new Coordinate(0.0, 0.0,
      Coordinates.NULL_ORDINATE), comb);
    runPtInRing(Location.BOUNDARY, new Coordinate(0.0, 1.0,
      Coordinates.NULL_ORDINATE), comb);
    // at vertex
    runPtInRing(Location.BOUNDARY, new Coordinate(4.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);
    runPtInRing(Location.BOUNDARY, new Coordinate(8.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);

    // on horizontal segment
    runPtInRing(Location.BOUNDARY, new Coordinate(11.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);
    // on vertical segment
    runPtInRing(Location.BOUNDARY, new Coordinate(30.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);
    // on angled segment
    runPtInRing(Location.BOUNDARY, new Coordinate(22.0, 7.0,
      Coordinates.NULL_ORDINATE), comb);

    runPtInRing(Location.INTERIOR, new Coordinate(1.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);
    runPtInRing(Location.INTERIOR, new Coordinate(5.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);
    runPtInRing(Location.INTERIOR, new Coordinate(1.0, 7.0,
      Coordinates.NULL_ORDINATE), comb);

    runPtInRing(Location.EXTERIOR, new Coordinate(12.0, 10.0,
      Coordinates.NULL_ORDINATE), comb);
    runPtInRing(Location.EXTERIOR, new Coordinate(16.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);
    runPtInRing(Location.EXTERIOR, new Coordinate(35.0, 5.0,
      Coordinates.NULL_ORDINATE), comb);
  }

  public void testComplexRing() throws Exception {
    runPtInRing(
      Location.INTERIOR,
      new Coordinate(0.0, 0, Coordinates.NULL_ORDINATE),
      "POLYGON ((-40 80, -40 -80, 20 0, 20 -100, 40 40, 80 -80, 100 80, 140 -20, 120 140, 40 180,     60 40, 0 120, -20 -20, -40 80))");
  }

  /**
   * Tests that repeated points are handled correctly
   * @throws Exception
   */
  public void testRepeatedPts() throws Exception {
    runPtInRing(Location.BOUNDARY, new Coordinate(0.0, 0,
      Coordinates.NULL_ORDINATE), repeatedPts);
    runPtInRing(Location.BOUNDARY, new Coordinate(0.0, 1,
      Coordinates.NULL_ORDINATE), repeatedPts);

    // at vertex
    runPtInRing(Location.BOUNDARY, new Coordinate(2.0, 5,
      Coordinates.NULL_ORDINATE), repeatedPts);
    runPtInRing(Location.BOUNDARY, new Coordinate(8.0, 5,
      Coordinates.NULL_ORDINATE), repeatedPts);
    runPtInRing(Location.BOUNDARY, new Coordinate(10.0, 5,
      Coordinates.NULL_ORDINATE), repeatedPts);

    runPtInRing(Location.INTERIOR, new Coordinate(1.0, 5,
      Coordinates.NULL_ORDINATE), repeatedPts);
    runPtInRing(Location.INTERIOR, new Coordinate(3.0, 5,
      Coordinates.NULL_ORDINATE), repeatedPts);

  }

}
