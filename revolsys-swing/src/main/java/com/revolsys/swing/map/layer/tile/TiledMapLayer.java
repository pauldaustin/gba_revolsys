package com.revolsys.swing.map.layer.tile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jeometry.common.data.type.DataType;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.gis.tiled.TileLevel;
import com.revolsys.gis.tiled.TiledLayer;
import com.revolsys.record.io.format.json.JsonObject;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.component.ValueField;
import com.revolsys.swing.layout.GroupLayouts;
import com.revolsys.swing.map.layer.raster.AbstractTiledGeoreferencedImageLayer;
import com.revolsys.swing.map.layer.raster.TiledGeoreferencedImageLayerRenderer;
import com.revolsys.swing.map.view.ViewRenderer;

public class TiledMapLayer extends AbstractTiledGeoreferencedImageLayer<TiledMapLayerTile> {

  private final Object initSync = new Object();

  private TiledLayer layer;

  public TiledMapLayer() {
    super("tiledMapLayer");
  }

  public TiledMapLayer(final Map<String, ? extends Object> properties) {
    this();
    setProperties(properties);
  }

  public TiledMapLayer(final TiledLayer layer) {
    this();
    this.layer = layer;
    setName(layer.getLayerName());
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof TiledMapLayer) {
      final TiledMapLayer layer = (TiledMapLayer)other;
      if (DataType.equal(layer.layer, this.layer)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<TiledMapLayerTile> getOverlappingMapTiles(
    final AbstractTiledLayerRenderer<?, ?> renderer, final ViewRenderer view) {
    final List<TiledMapLayerTile> tiles = new ArrayList<>();
    try {
      final double viewResolution = view.getMetresPerPixel();
      final TileLevel level = getTileLevel(viewResolution);
      if (level != null) {
        final BoundingBox viewBoundingBox = view.getBoundingBox();
        final BoundingBox maxBoundingBox = getBoundingBox();
        final BoundingBox boundingBox = viewBoundingBox.bboxToCs(this)
          .bboxIntersection(maxBoundingBox);
        final double minX = boundingBox.getMinX();
        final double minY = boundingBox.getMinY();
        final double maxX = boundingBox.getMaxX();
        final double maxY = boundingBox.getMaxY();

        // Tiles start at the North-West corner of the map
        final int minTileX = level.getTileX(minX);
        final int minTileY = level.getTileY(maxY);
        final int maxTileX = level.getTileX(maxX);
        final int maxTileY = level.getTileY(minY);

        for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
          for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            final TiledMapLayerTile tile = new TiledMapLayerTile(this, level, tileX, tileY);
            tiles.add(tile);
          }
        }
      }
    } catch (final Throwable e) {
      setError(e);
    }
    return tiles;
  }

  @Override
  public double getResolution(final ViewRenderer view) {
    if (this.layer == null) {
      return 0;
    } else {
      final double metresPerPixel = view.getMetresPerPixel();
      return this.layer.getResolution(metresPerPixel);
    }
  }

  public BufferedImage getTileImage(TileLevel level, int tileX, int tileY) {
    // TODO Auto-generated method stub
    return null;
  }

  public TileLevel getTileLevel(double viewResolution) {
    /// TODO
    return null;
  }

  @Override
  protected boolean initializeDo() {
    synchronized (this.initSync) {
      // TODO
      return true;
    }
  }

  @Override
  protected ValueField newPropertiesTabGeneralPanelSource(final BasePanel parent) {
    final ValueField panel = super.newPropertiesTabGeneralPanelSource(parent);

    // final String url = getUrl();
    // if (Property.hasValue(url)) {
    // SwingUtil.addLabelledReadOnlyTextField(panel, "URL", url);
    // }
    GroupLayouts.makeColumns(panel, panel.getComponentCount() / 2, true);
    return panel;
  }

  @Override
  protected TiledGeoreferencedImageLayerRenderer<TiledMapLayerTile> newRenderer() {
    return new TiledGeoreferencedImageLayerRenderer<TiledMapLayerTile>(this);
  }

  @Override
  public void refreshDo() {
    initializeForce();
    super.refreshDo();
  }

  @Override
  public JsonObject toMap() {
    final JsonObject map = super.toMap();
    // if (Property.hasValue(this.connectionName)) {
    // addToMap(map, "connectionName", this.connectionName);
    // addToMap(map, "servicePath", this.servicePath);
    // } else {
    // addToMap(map, "url", this.url);
    // addToMap(map, "username", this.username);
    // addToMap(map, "password", PasswordUtil.encrypt(this.password));
    // }
    // addToMap(map, "useServerExport", this.useServerExport, false);
    return map;
  }

}
