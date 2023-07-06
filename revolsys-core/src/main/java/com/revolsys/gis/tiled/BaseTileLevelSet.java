package com.revolsys.gis.tiled;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BaseTileLevelSet implements TileLevelSet {

  public static <TL extends TileLevel> TL getTileLevel(final List<TL> tileLevels,
    final double metresPerPixel) {
    final Iterator<TL> iterator = tileLevels.iterator();
    if (iterator.hasNext()) {
      TL level1 = iterator.next();
      double resolution1 = level1.getPixelWidth();
      while (iterator.hasNext()) {
        final TL level2 = iterator.next();
        final double resolution2 = level2.getPixelWidth();
        if (metresPerPixel >= resolution1
          || resolution1 - metresPerPixel < (resolution1 - resolution2) * 0.7) {
          // Within 70% of more detailed
          return level1;
        }
        level1 = level2;
        resolution1 = resolution2;
      }
      return level1;
    } else {
      return null;
    }
  }

  protected List<TileLevel> tileLevels = new ArrayList<>();

  @Override
  public TileLevel getTileLevel(final double metresPerPixel) {
    final List<TileLevel> tileLevels = this.tileLevels;
    return getTileLevel(tileLevels, metresPerPixel);
  }

}
