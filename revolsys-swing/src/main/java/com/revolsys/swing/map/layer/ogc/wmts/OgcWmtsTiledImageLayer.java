package com.revolsys.swing.map.layer.ogc.wmts;

import java.awt.image.BufferedImage;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jeometry.common.data.type.DataType;
import org.jeometry.common.exception.Exceptions;
import org.jeometry.common.exception.WrappedException;
import org.jeometry.common.logging.Logs;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.gis.tiled.TileLevel;
import com.revolsys.gis.wmts.WmtsClient;
import com.revolsys.gis.wmts.capabilities.WmtsLayerDefinition;
import com.revolsys.gis.wmts.capabilities.WmtsTileMatrix;
import com.revolsys.gis.wmts.capabilities.WmtsTileMatrixSet;
import com.revolsys.record.io.format.json.JsonObject;
import com.revolsys.swing.map.layer.raster.AbstractTiledGeoreferencedImageLayer;
import com.revolsys.swing.map.layer.tile.AbstractTiledLayerRenderer;
import com.revolsys.swing.map.view.ViewRenderer;
import com.revolsys.util.Property;
import com.revolsys.webservice.WebService;
import com.revolsys.webservice.WebServiceConnectionManager;

public class OgcWmtsTiledImageLayer extends AbstractTiledGeoreferencedImageLayer<WmtsMapTile> {
  private String connectionName;

  private String serviceUrl;

  private boolean hasError = false;

  private String layerId;

  private WmtsLayerDefinition wmtsLayerDefinition;

  public OgcWmtsTiledImageLayer() {
    super("ogcWmtsImageLayer");
  }

  public OgcWmtsTiledImageLayer(final Map<String, ? extends Object> properties) {
    this();
    setProperties(properties);
  }

  public OgcWmtsTiledImageLayer(final WmtsLayerDefinition wmtsLayerDefinition) {
    this();
    if (wmtsLayerDefinition == null) {
      setExists(false);
    } else {
      setInitialized(true);
      setExists(true);
      setWmtsLayerDefinition(wmtsLayerDefinition);
    }
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof OgcWmtsTiledImageLayer) {
      final OgcWmtsTiledImageLayer layer = (OgcWmtsTiledImageLayer)other;
      if (DataType.equal(layer.getServiceUrl(), getServiceUrl())) {
        if (DataType.equal(layer.getLayerId(), getLayerId())) {
          if (DataType.equal(layer.getConnectionName(), getConnectionName())) {
            if (DataType.equal(layer.getName(), getName())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public WmtsClient getClient() {
    return wmtsLayerDefinition.getClient();
  }

  public String getConnectionName() {
    return this.connectionName;
  }

  public String getLayerId() {
    return this.layerId;
  }

  @Override
  public List<WmtsMapTile> getOverlappingMapTiles(AbstractTiledLayerRenderer<?, ?> renderer,
    ViewRenderer view) {
    List<WmtsMapTile> tiles = new ArrayList<>();
    final WmtsLayerDefinition layer = wmtsLayerDefinition;
    if (layer != null) {
      try {
        final double viewResolution = view.getMetresPerPixel();
        final WmtsTileMatrix tileMatrix = layer.getTileMatrixSet().getTileLevel(viewResolution);
        if (tileMatrix != null) {
          final BoundingBox viewBoundingBox = view.getBoundingBox();
          final BoundingBox maxBoundingBox = getBoundingBox();
          final BoundingBox boundingBox = viewBoundingBox.bboxToCs(this)
            .bboxIntersection(maxBoundingBox);
          final double minX = boundingBox.getMinX();
          final double minY = boundingBox.getMinY();
          final double maxX = boundingBox.getMaxX();
          final double maxY = boundingBox.getMaxY();

          // Tiles start at the North-West corner of the map
          final int minTileX = tileMatrix.getTileX(minX);
          final int minTileY = tileMatrix.getTileY(maxY);
          final int maxTileX = tileMatrix.getTileX(maxX);
          final int maxTileY = tileMatrix.getTileY(minY);

          for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
              final WmtsMapTile tile = new WmtsMapTile(this, tileMatrix, tileX, tileY);
              tiles.add(tile);
            }
          }
        }
      } catch (final Throwable e) {
        setError(e);
      }
    }
    return tiles;
  }

  public String getServiceUrl() {
    return this.serviceUrl;
  }

  public BufferedImage getTileImage(WmtsTileMatrix tileMatrix, int tileX, int tileY) {
    return wmtsLayerDefinition.getTileImage(tileMatrix, tileX, tileY);
  }

  @Override
  public TileLevel getTileLevel(double metresPerPixel) {
    return wmtsLayerDefinition.getTileMatrixSet().getTileLevel(metresPerPixel);
  }

  public WmtsLayerDefinition getWmtsLayerDefinition() {
    return this.wmtsLayerDefinition;
  }

  @Override
  protected boolean initializeDo() {
    final boolean initialized = super.initializeDo();
    if (initialized) {
      try {
        final WmtsClient wmtsClient;
        if (Property.hasValue(this.connectionName)) {
          final WebService<?> webService = WebServiceConnectionManager
            .getWebService(this.connectionName);
          if (webService == null) {
            Logs.error(this,
              getPath() + ": Web service " + this.connectionName + ": no connection configured");
            return false;
          } else if (webService instanceof WmtsClient) {
            wmtsClient = (WmtsClient)webService;
          } else {
            Logs.error(this,
              getPath() + ": Web service " + this.connectionName + ": is not a OGS WMS service");
            return false;
          }
        } else if (Property.hasValue(this.serviceUrl)) {
          wmtsClient = new WmtsClient(this.serviceUrl);
        } else {
          Logs.error(this, getPath()
            + ": A record store layer requires a connection entry with a name or url, username, and password ");
          return false;
        }
        final WmtsLayerDefinition wmtsLayerDefinition = wmtsClient.getLayer(this.layerId);
        setWmtsLayerDefinition(wmtsLayerDefinition);
        return wmtsLayerDefinition != null;
      } catch (final WrappedException e) {
        final Throwable cause = Exceptions.unwrap(e);
        if (cause instanceof UnknownHostException) {
          return setNotExists("Unknown host: " + cause.getMessage());
        } else {
          throw e;
        }
      }
    }
    return initialized;
  }

  public boolean isHasError() {
    return this.hasError;
  }

  @Override
  protected void refreshDo() {
    this.hasError = false;
    super.refreshDo();
  }

  public void setConnectionName(final String connectionName) {
    this.connectionName = connectionName;
  }

  @Override
  public void setError(final Throwable e) {
    if (!this.hasError) {
      this.hasError = true;
      Logs.error(this, "Unable to get map tiles", e);
    }
  }

  public void setLayerId(final String layerId) {
    this.layerId = layerId;
  }

  public void setServiceUrl(final String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  protected void setWmtsLayerDefinition(final WmtsLayerDefinition wmtsLayerDefinition) {
    this.wmtsLayerDefinition = wmtsLayerDefinition;
    if (wmtsLayerDefinition == null) {
      setExists(false);
    } else {
      setExists(true);
      final WmtsClient wmtsClient = wmtsLayerDefinition.getClient();
      final String connectionName = wmtsClient.getName();
      if (Property.hasValue(connectionName)) {
        this.connectionName = connectionName;
        this.serviceUrl = null;
      } else {
        this.serviceUrl = wmtsClient.getServiceUrl().toString();
      }
      final String layerTitle = wmtsLayerDefinition.getTitle();
      if (!Property.hasValue(getName())) {
        setName(layerTitle);
      }
      this.layerId = wmtsLayerDefinition.getIdentifier();
      WmtsTileMatrixSet tileMatrixSet = wmtsLayerDefinition.getTileMatrixSet();
      GeometryFactory geometryFactory = tileMatrixSet.getGeometryFactory();
      setBoundingBox(wmtsLayerDefinition.getWgs84BoundingBoxes().get(0));
      setGeometryFactory(geometryFactory);
    }
  }

  @Override
  public JsonObject toMap() {
    final JsonObject map = super.toMap();
    map.keySet()
      .removeAll(Arrays.asList("readOnly", "querySupported", "selectSupported", "minimumScale",
        "maximumScale"));
    if (Property.hasValue(this.connectionName)) {
      addToMap(map, "connectionName", this.connectionName);
    } else {
      addToMap(map, "serviceUrl", this.serviceUrl);
    }
    addToMap(map, "layerId", this.layerId);
    return map;
  }
}
