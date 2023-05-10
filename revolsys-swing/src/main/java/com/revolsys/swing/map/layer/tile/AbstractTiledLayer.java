package com.revolsys.swing.map.layer.tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.revolsys.collection.list.Lists;
import com.revolsys.gis.tiled.BaseTileLevel;
import com.revolsys.gis.tiled.TileLevel;
import com.revolsys.record.io.format.json.JsonObject;
import com.revolsys.swing.map.layer.AbstractLayer;
import com.revolsys.swing.map.layer.BaseMapLayer;
import com.revolsys.swing.map.view.ViewRenderer;
import com.revolsys.util.AbstractMapTile;

public abstract class AbstractTiledLayer<D, T extends AbstractMapTile<D>> extends AbstractLayer
  implements BaseMapLayer {

  protected List<TileLevel> tileLevels = new ArrayList<>();

  protected List<Double> tileResolutions;

  public AbstractTiledLayer(final String type) {
    super(type);
    setReadOnly(true);
    setSelectSupported(false);
    setQuerySupported(false);
    setRenderer(newRenderer());
  }

  public abstract List<T> getOverlappingMapTiles(AbstractTiledLayerRenderer<?, ?> renderer,
    final ViewRenderer view);

  public double getResolution(final ViewRenderer view) {
    final TileLevel tileLevel = getTileLevel(view);
    return tileLevel.getPixelWidth();
  }

  public TileLevel getTileLevel(final double metresPerPixel) {
    final int count = this.tileLevels.size();
    for (int i = 0; i < count - 1; i++) {
      final TileLevel level1 = this.tileLevels.get(i);
      final double resolution1 = level1.getTileWidthPixels();
      final TileLevel level2 = this.tileLevels.get(i + 1);
      final double resolution2 = level2.getTileHeightPixels();

      if (metresPerPixel >= resolution1
        || resolution1 - metresPerPixel < (resolution1 - resolution2) * 0.7) {
        // Within 70% of more detailed
        return level1;
      }
    }
    return this.tileLevels.get(count - 1);
  }

  public TileLevel getTileLevel(final ViewRenderer view) {
    final double metresPerPixel = view.getMetresPerPixel();
    return getTileLevel(metresPerPixel);
  }

  protected BaseTileLevel initTileLevel(BaseTileLevel tileLevel) {
    return tileLevel;
  }

  protected abstract AbstractTiledLayerRenderer<D, T> newRenderer();

  protected BaseTileLevel newTileLevel(double resolution) {
    final BaseTileLevel tileLevel = new BaseTileLevel().setPixelSize(resolution);
    return initTileLevel(tileLevel);
  }

  protected List<TileLevel> newTileLevels(List<Double> tileResultions) {
    final List<TileLevel> levels = new ArrayList<>();
    for (final double resolution : this.tileResolutions) {
      final BaseTileLevel tileLevel = newTileLevel(resolution);
      levels.add(tileLevel);
    }
    return levels;
  }

  @Override
  protected void refreshDo() {
    super.refreshDo();
    final AbstractTiledLayerRenderer<D, T> renderer = getRenderer();
    renderer.clearCachedTiles();
  }

  public void setError(final String message, final Throwable e) {
    final AbstractTiledLayerRenderer<D, T> renderer = getRenderer();
    renderer.setError(message, e);
  }

  public void setError(final Throwable e) {
    setError("Error loading '" + getPath() + "', move the map or Refresh the layer to try again",
      e);
  }

  protected void setTileResolutions(double... tileResolutions) {
    this.tileResolutions = Lists.newArray(tileResolutions);
  }

  @Override
  public JsonObject toMap() {
    final JsonObject map = super.toMap();
    map.keySet().removeAll(Arrays.asList("readOnly", "querySupported", "selectSupported"));
    return map;
  }

  protected void updateTileLevels() {
    this.tileLevels = newTileLevels(this.tileResolutions);
  }
}
