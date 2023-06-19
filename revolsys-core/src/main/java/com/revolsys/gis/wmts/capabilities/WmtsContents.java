package com.revolsys.gis.wmts.capabilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.revolsys.gis.wmts.WmtsClient;
import com.revolsys.record.io.format.xml.XmlUtil;

public class WmtsContents extends OwsContents {

  private List<WmtsTileMatrixSet> tileMatrixSets = new ArrayList<>();

  private final Map<String, WmtsTileMatrixSet> tileMatrixSetById = new LinkedHashMap<>();

  private List<WmtsLayerDefinition> layers = new ArrayList<>();

  private final Map<String, WmtsLayerDefinition> layerById = new LinkedHashMap<>();

  private final WmtsCapabilities capabilities;

  public WmtsContents(final WmtsCapabilities capabilities, final Element element) {
    super(element);
    this.capabilities = capabilities;
    this.tileMatrixSets = XmlUtil.getList(element, "TileMatrixSet", WmtsTileMatrixSet::new);
    for (final WmtsTileMatrixSet tileMatrixSet : this.tileMatrixSets) {
      final String id = tileMatrixSet.getIdentifier();
      this.tileMatrixSetById.put(id, tileMatrixSet);
    }
    this.layers = XmlUtil.getList(element, "Layer", (e) -> new WmtsLayerDefinition(this, e));
    for (final WmtsLayerDefinition layer : this.layers) {
      final String id = layer.getIdentifier();
      this.layerById.put(id, layer);
    }
  }

  public WmtsClient getClient() {
    return this.capabilities.getClient();
  }

  public WmtsLayerDefinition getLayer(final String id) {
    return this.layerById.get(id);
  }

  public List<WmtsLayerDefinition> getLayers() {
    return this.layers;
  }

  public WmtsTileMatrixSet getTileMatrixSet(final String id) {
    return this.tileMatrixSetById.get(id);
  }
}
