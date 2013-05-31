package com.revolsys.swing.map.layer.openstreetmap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.AbstractTiledImageLayer;
import com.revolsys.swing.map.layer.MapTile;

public class OpenStreetMapLayer extends AbstractTiledImageLayer {

  public static final GeometryFactory GEOMETRY_FACTORY = GeometryFactory.getFactory(4326);

  private static final BoundingBox MAX_BOUNDING_BOX = new BoundingBox(
    GEOMETRY_FACTORY, -180, -85, 180, 85);

  private final OpenStreetMapClient client;

  public OpenStreetMapLayer() {
    this(new OpenStreetMapClient());
  }

  public OpenStreetMapLayer(final OpenStreetMapClient client) {
    this.client = client;
  }

  public OpenStreetMapLayer(final String serverUrl) {
    this(new OpenStreetMapClient(serverUrl));
  }

  @Override
  public BoundingBox getBoundingBox() {
    return MAX_BOUNDING_BOX;
  }

  public OpenStreetMapClient getClient() {
    return client;
  }

  @Override
  public List<MapTile> getOverlappingMapTiles(final Viewport2D viewport) {
    final List<MapTile> tiles = new ArrayList<MapTile>();
    try {
      final double metresPerPixel = viewport.getMetresPerPixel();
      final int zoomLevel = client.getZoomLevel(metresPerPixel);
      double resolution = getResolution(viewport);
      final BoundingBox geographicBoundingBox = viewport.getBoundingBox()
        .convert(GEOMETRY_FACTORY)
        .intersection(MAX_BOUNDING_BOX);
      final double minX = geographicBoundingBox.getMinX();
      final double minY = geographicBoundingBox.getMinY();
      final double maxX = geographicBoundingBox.getMaxX();
      final double maxY = geographicBoundingBox.getMaxY();

      // Tiles start at the North-West corner of the map
      final int minTileY = client.getTileY(zoomLevel, maxY);
      final int maxTileY = client.getTileY(zoomLevel, minY);
      final int minTileX = client.getTileX(zoomLevel, minX);
      final int maxTileX = client.getTileX(zoomLevel, maxX);

      for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
          OpenStreetMapTile tile = new OpenStreetMapTile(this, zoomLevel,
            resolution, tileX, tileY);
          tiles.add(tile);
        }
      }

    } catch (final Throwable e) {
      LoggerFactory.getLogger(getClass()).error("Error getting tile envelopes",
        e);
    }
    return tiles;
  }

  @Override
  public double getResolution(final Viewport2D viewport) {
    final double metresPerPixel = viewport.getMetresPerPixel();
    final int zoomLevel = client.getZoomLevel(metresPerPixel);
    return client.getResolution(zoomLevel);
  }

  @Override
  public boolean isVisible() {
    if (!super.isVisible()) {
      return false;
    } else {
      return true;
    }
  }

}
