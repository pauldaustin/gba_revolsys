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

  private Map<String, WmtsTileMatrixSet> tileMatrixSetById = new LinkedHashMap<>();

  private List<WmtsLayerDefinition> layers = new ArrayList<>();

  private Map<String, WmtsLayerDefinition> layerById = new LinkedHashMap<>();

  private WmtsCapabilities capabilities;

  public WmtsContents(WmtsCapabilities capabilities, final Element element) {
    super(element);
    this.capabilities = capabilities;
    this.tileMatrixSets = XmlUtil.getList(element, "TileMatrixSet", WmtsTileMatrixSet::new);
    for (WmtsTileMatrixSet tileMatrixSet : tileMatrixSets) {
      String id = tileMatrixSet.getIdentifier();
      this.tileMatrixSetById.put(id, tileMatrixSet);
    }
    this.layers = XmlUtil.getList(element, "Layer", (e) -> new WmtsLayerDefinition(this, e));
    for (WmtsLayerDefinition layer : layers) {
      String id = layer.getIdentifier();
      this.layerById.put(id, layer);
    }
  }

  public WmtsClient getClient() {
    return capabilities.getClient();
  }

  public WmtsLayerDefinition getLayer(String id) {
    return layerById.get(id);
  }

  public List<WmtsLayerDefinition> getLayers() {
    return layers;
  }

  public WmtsTileMatrixSet getTileMatrixSet(String id) {
    return tileMatrixSetById.get(id);
  }
}
