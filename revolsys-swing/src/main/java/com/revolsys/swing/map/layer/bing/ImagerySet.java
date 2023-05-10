package com.revolsys.swing.map.layer.bing;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.gis.tiled.BaseTileLevelSet;
import com.revolsys.gis.tiled.TileLevelSet;

public enum ImagerySet implements TileLevelSet {
  Aerial(19), //
  AerialWithLabels(19), //
  AerialWithLabelsOnDemand(19), //
  CanvasDark(20), //
  CanvasLight(20), //
  CanvasGray(20), //
  CollinsBart(23), //
  OrdnanceSurvey(23), //
  Road(20);

  private int maxLevelOfDetail;

  private final List<BingTileLevel> levels = new ArrayList<>();

  private ImagerySet(final int maxLevelOfDetail) {
    this.maxLevelOfDetail = maxLevelOfDetail;
    for (int i = 0; i < maxLevelOfDetail; i++) {
      this.levels.add(new BingTileLevel(i + 1, BingClient.METRES_PER_PIXEL[i]));
    }
  }

  public int getMaxLevelOfDetail() {
    return this.maxLevelOfDetail;
  }

  @Override
  public BingTileLevel getTileLevel(double metresPerPixel) {
    return BaseTileLevelSet.getTileLevel(this.levels, metresPerPixel);
  }
}
