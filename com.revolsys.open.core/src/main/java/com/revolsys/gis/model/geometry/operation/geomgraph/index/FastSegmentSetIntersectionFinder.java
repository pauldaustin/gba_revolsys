package com.revolsys.gis.model.geometry.operation.geomgraph.index;

import java.util.Collection;
import java.util.List;

import com.revolsys.gis.model.geometry.Geometry;
import com.revolsys.gis.model.geometry.operation.chain.MCIndexSegmentSetMutualIntersector;
import com.revolsys.gis.model.geometry.operation.chain.SegmentIntersectionDetector;
import com.revolsys.gis.model.geometry.operation.chain.SegmentSetMutualIntersector;
import com.revolsys.gis.model.geometry.operation.chain.SegmentString;
import com.revolsys.gis.model.geometry.operation.chain.SegmentStringUtil;

/**
 * Finds if two sets of {@link SegmentString}s intersect. Uses indexing for fast
 * performance and to optimize repeated tests against a target set of lines.
 * Short-circuited to return as soon an intersection is found.
 * 
 * @version 1.7
 */
public class FastSegmentSetIntersectionFinder {
  private SegmentSetMutualIntersector segSetMutInt;

  private static final String KEY = FastSegmentSetIntersectionFinder.class.getName();

  public static FastSegmentSetIntersectionFinder get(Geometry geometry) {
    FastSegmentSetIntersectionFinder instance = geometry.getProperty(KEY);
    if (instance == null) {
      List<SegmentString> segments = SegmentStringUtil.extractSegmentStrings(geometry);
      instance = new FastSegmentSetIntersectionFinder(segments);
      geometry.setPropertySoft(KEY, instance);
    }
    return instance;
  }

  // for testing purposes
  // private SimpleSegmentSetMutualIntersector mci;

  public FastSegmentSetIntersectionFinder(
    Collection<SegmentString> baseSegStrings) {
    init(baseSegStrings);
  }

  private void init(Collection<SegmentString> baseSegStrings) {
    segSetMutInt = new MCIndexSegmentSetMutualIntersector();
    // segSetMutInt = new MCIndexIntersectionSegmentSetMutualIntersector();

    // mci = new SimpleSegmentSetMutualIntersector();
    segSetMutInt.setBaseSegments(baseSegStrings);
  }

  /**
   * Gets the segment set intersector used by this class. This allows other uses
   * of the same underlying indexed structure.
   * 
   * @return the segment set intersector used
   */
  public SegmentSetMutualIntersector getSegmentSetIntersector() {
    return segSetMutInt;
  }

  private static LineIntersector li = new RobustLineIntersector();

  public boolean intersects(Collection<SegmentString> segStrings) {
    SegmentIntersectionDetector intFinder = new SegmentIntersectionDetector(li);
    segSetMutInt.setSegmentIntersector(intFinder);

    segSetMutInt.process(segStrings);
    return intFinder.hasIntersection();
  }

  public boolean intersects(Collection<SegmentString> segStrings,
    SegmentIntersectionDetector intDetector) {
    segSetMutInt.setSegmentIntersector(intDetector);

    segSetMutInt.process(segStrings);
    return intDetector.hasIntersection();
  }
}
