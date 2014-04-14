package com.revolsys.jtstest.function;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.jts.geom.Coordinate;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.Envelope;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.shape.random.RandomPointsBuilder;
import com.revolsys.jts.shape.random.RandomPointsInGridBuilder;

public class CreateRandomShapeFunctions {

  private static double haltonOrdinate(final int index, final int base) {
    double result = 0;
    double f = 1.0 / base;
    int i = index;
    while (i > 0) {
      result = result + f * (i % base);
      i = (int)Math.floor(i / (double)base);
      f = f / base;
    }
    return result;
  }

  public static Geometry haltonPoints(final Geometry g, final int nPts) {
    return haltonPointsWithBases(g, nPts, 2, 3);
  }

  public static Geometry haltonPoints57(final Geometry g, final int nPts) {
    return haltonPointsWithBases(g, nPts, 5, 7);
  }

  public static Geometry haltonPointsWithBases(final Geometry g,
    final int nPts, final int basei, final int basej) {
    final Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    final Coordinates[] pts = new Coordinates[nPts];
    final double baseX = env.getMinX();
    final double baseY = env.getMinY();

    int i = 0;
    while (i < nPts) {
      final double x = baseX + env.getWidth() * haltonOrdinate(i + 1, basei);
      final double y = baseY + env.getHeight() * haltonOrdinate(i + 1, basej);
      final Coordinates p = new Coordinate(x, y);
      if (!env.contains(p)) {
        continue;
      }
      pts[i++] = p;
    }
    return FunctionsUtil.getFactoryOrDefault(g).createMultiPoint(pts);
  }

  public static Geometry randomLineString(final Geometry g, final int nPts) {
    final Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    final double width = env.getWidth();
    final double hgt = env.getHeight();

    final Coordinates[] pts = new Coordinates[nPts];

    for (int i = 0; i < nPts; i++) {
      final double xLen = width * Math.random();
      final double yLen = hgt * Math.random();
      pts[i] = randomPtInRectangleAround(env.centre(), xLen, yLen);
    }
    return geomFact.lineString(pts);
  }

  private static Coordinates randomPointInTriangle(final Coordinates p0,
    final Coordinates p1, final Coordinates p2) {
    double s = Math.random();
    double t = Math.random();
    if (s + t > 1) {
      s = 1.0 - s;
      t = 1.0 - t;
    }
    final double a = 1 - (s + t);
    final double b = s;
    final double c = t;

    final double rpx = a * p0.getX() + b * p1.getX() + c * p2.getX();
    final double rpy = a * p0.getY() + b * p1.getY() + c * p2.getY();

    return new Coordinate(rpx, rpy);
  }

  public static Geometry randomPoints(final Geometry g, final int nPts) {
    final RandomPointsBuilder shapeBuilder = new RandomPointsBuilder(
      FunctionsUtil.getFactoryOrDefault(g));
    shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
    shapeBuilder.setNumPoints(nPts);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInGrid(final Geometry g, final int nPts) {
    final RandomPointsInGridBuilder shapeBuilder = new RandomPointsInGridBuilder(
      FunctionsUtil.getFactoryOrDefault(g));
    shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
    shapeBuilder.setNumPoints(nPts);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInGridCircles(final Geometry g,
    final int nPts) {
    final RandomPointsInGridBuilder shapeBuilder = new RandomPointsInGridBuilder(
      FunctionsUtil.getFactoryOrDefault(g));
    shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
    shapeBuilder.setNumPoints(nPts);
    shapeBuilder.setConstrainedToCircle(true);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInGridWithGutter(final Geometry g,
    final int nPts, final double gutterFraction) {
    final RandomPointsInGridBuilder shapeBuilder = new RandomPointsInGridBuilder(
      FunctionsUtil.getFactoryOrDefault(g));
    shapeBuilder.setExtent(FunctionsUtil.getEnvelopeOrDefault(g));
    shapeBuilder.setNumPoints(nPts);
    shapeBuilder.setGutterFraction(gutterFraction);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInPolygon(final Geometry g, final int nPts) {
    final RandomPointsBuilder shapeBuilder = new RandomPointsBuilder(
      FunctionsUtil.getFactoryOrDefault(g));
    shapeBuilder.setExtent(g);
    shapeBuilder.setNumPoints(nPts);
    return shapeBuilder.getGeometry();
  }

  public static Geometry randomPointsInTriangle(final Geometry g, final int nPts) {
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    final Coordinates[] gpts = g.getCoordinateArray();
    final Coordinates tri0 = gpts[0];
    final Coordinates tri1 = gpts[1];
    final Coordinates tri2 = gpts[2];

    final List pts = new ArrayList();

    for (int i = 0; i < nPts; i++) {
      pts.add(geomFact.point(randomPointInTriangle(tri0, tri1, tri2)));
    }
    return geomFact.buildGeometry(pts);
  }

  private static Coordinates randomPtInRectangleAround(
    final Coordinates centre, final double width, final double height) {
    final double x0 = centre.getX() + width * (Math.random() - 0.5);
    final double y0 = centre.getY() + height * (Math.random() - 0.5);
    return new Coordinate(x0, y0);
  }

  private static int randomQuadrant(final int exclude) {
    while (true) {
      int quad = (int)(Math.random() * 4);
      if (quad > 3) {
        quad = 3;
      }
      if (quad != exclude) {
        return quad;
      }
    }
  }

  public static Geometry randomRadialPoints(final Geometry g, final int nPts) {
    final Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    final double xLen = env.getWidth();
    final double yLen = env.getHeight();
    final double rMax = Math.min(xLen, yLen) / 2.0;

    final double centreX = env.getMinX() + xLen / 2;
    final double centreY = env.getMinY() + yLen / 2;

    final List pts = new ArrayList();

    for (int i = 0; i < nPts; i++) {
      final double rand = Math.random();
      // use rand^2 to accentuate radial distribution
      final double r = rMax * rand * rand;
      // produces even distribution
      // double r = rMax * Math.sqrt(rand);
      final double ang = 2 * Math.PI * Math.random();
      final double x = centreX + r * Math.cos(ang);
      final double y = centreY + r * Math.sin(ang);
      pts.add(geomFact.point(x, y));
    }
    return geomFact.buildGeometry(pts);
  }

  public static Geometry randomRectilinearWalk(final Geometry g, final int nPts) {
    final Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    final double xLen = env.getWidth();
    final double yLen = env.getHeight();

    final Coordinates[] pts = new Coordinates[nPts];

    boolean xory = true;
    for (int i = 0; i < nPts; i++) {
      Coordinates pt = null;
      if (i == 0) {
        pt = randomPtInRectangleAround(env.centre(), xLen, yLen);
      } else {
        final double dist = xLen * (Math.random() - 0.5);
        double x = pts[i - 1].getX();
        double y = pts[i - 1].getY();
        if (xory) {
          x += dist;
        } else {
          y += dist;
        }
        // switch orientation
        xory = !xory;
        pt = new Coordinate(x, y);
      }
      pts[i] = pt;
    }
    return geomFact.lineString(pts);
  }

  public static Geometry randomSegments(final Geometry g, final int nPts) {
    final Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    final double xLen = env.getWidth();
    final double yLen = env.getHeight();

    final List lines = new ArrayList();

    for (int i = 0; i < nPts; i++) {
      final double x0 = env.getMinX() + xLen * Math.random();
      final double y0 = env.getMinY() + yLen * Math.random();
      final double x1 = env.getMinX() + xLen * Math.random();
      final double y1 = env.getMinY() + yLen * Math.random();
      lines.add(geomFact.lineString(new Coordinates[] {
        new Coordinate(x0, y0), new Coordinate(x1, y1)
      }));
    }
    return geomFact.buildGeometry(lines);
  }

  public static Geometry randomSegmentsInGrid(final Geometry g, final int nPts) {
    final Envelope env = FunctionsUtil.getEnvelopeOrDefault(g);
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);

    final int nCell = (int)Math.sqrt(nPts) + 1;

    final double xLen = env.getWidth() / nCell;
    final double yLen = env.getHeight() / nCell;

    final List lines = new ArrayList();

    for (int i = 0; i < nCell; i++) {
      for (int j = 0; j < nCell; j++) {
        final double x0 = env.getMinX() + i * xLen + xLen * Math.random();
        final double y0 = env.getMinY() + j * yLen + yLen * Math.random();
        final double x1 = env.getMinX() + i * xLen + xLen * Math.random();
        final double y1 = env.getMinY() + j * yLen + yLen * Math.random();
        lines.add(geomFact.lineString(new Coordinates[] {
          new Coordinate(x0, y0), new Coordinate(x1, y1)
        }));
      }
    }
    return geomFact.buildGeometry(lines);
  }

}
