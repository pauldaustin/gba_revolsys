package com.revolsys.gis.cs.projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.revolsys.jts.geom.Coordinates;

public class ChainedCoordinatesOperation implements CoordinatesOperation {
  private final List<CoordinatesOperation> operations;

  public ChainedCoordinatesOperation(final CoordinatesOperation... operations) {
    this(Arrays.asList(operations));
  }

  public ChainedCoordinatesOperation(final List<CoordinatesOperation> operations) {
    this.operations = new ArrayList<CoordinatesOperation>(operations);
  }

  @Override
  public void perform(final Coordinates from, final Coordinates to) {
    Coordinates source = from;
    final Coordinates target = to;
    for (final CoordinatesOperation operation : operations) {
      operation.perform(source, target);
      source = target;
    }
  }

  @Override
  public void perform(int sourceNumAxis, double[] sourceCoordinates,
    final int targetNumAxis, final double[] targetCoordinates) {
    for (final CoordinatesOperation operation : operations) {
      operation.perform(sourceNumAxis, sourceCoordinates, targetNumAxis,
        targetCoordinates);
      sourceNumAxis = targetNumAxis;
      sourceCoordinates = targetCoordinates;
    }
  }

  @Override
  public String toString() {
    return operations.toString();
  }
}
