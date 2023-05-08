package com.revolsys.gis.wmts.capabilities;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.revolsys.gis.tiled.TiledLayer;
import com.revolsys.gis.wmts.WmtsClient;
import com.revolsys.raster.BufferedImages;
import com.revolsys.record.io.format.json.JsonObject;
import com.revolsys.record.io.format.xml.XmlUtil;
import com.revolsys.spring.resource.UrlResource;
import com.revolsys.util.UriTemplate;
import com.revolsys.webservice.WebServiceResource;

public class WmtsLayerDefinition extends OwsDatasetDescriptionSummary
  implements TiledLayer, WebServiceResource {
  private final List<String> formats;

  private final List<String> infoFormats;

  private final List<WmtsResourceUrl> resourceUrls;

  private final WmtsContents wmtsContents;

  private final TileMatrixSetLink tileMatrixSetLink;

  private final Map<String, UriTemplate> tileResourceTemplateByFormat = new HashMap<>();

  private UriTemplate defaultUriTemplate;

  public WmtsLayerDefinition(WmtsContents wmtsContents, final Element element) {
    super(element);
    this.wmtsContents = wmtsContents;
    // TODO Style
    this.formats = XmlUtil.getListString(element, "Format");
    this.infoFormats = XmlUtil.getListString(element, "InfoFormat");
    // TODO Dimension
    this.tileMatrixSetLink = XmlUtil.getFirstElement(element, "TileMatrixSetLink",
      e -> new TileMatrixSetLink(wmtsContents, e));
    this.resourceUrls = XmlUtil.getList(element, "ResourceURL", WmtsResourceUrl::new);
    for (final WmtsResourceUrl resourceUrl : this.resourceUrls) {
      if ("tile".equals(resourceUrl.getResourceType())) {
        final String format = resourceUrl.getFormat();
        final String template = resourceUrl.getTemplate();
        final UriTemplate uriTemplate = new UriTemplate(template);
        if (defaultUriTemplate == null)
          defaultUriTemplate = uriTemplate;
        this.tileResourceTemplateByFormat.put(format, uriTemplate);
      }
    }
  }

  public WmtsClient getClient() {
    return getWmtsContents().getClient();
  }

  public List<String> getFormats() {
    return this.formats;
  }

  @Override
  public String getIconName() {
    return "map";
  }

  public List<String> getInfoFormats() {
    return this.infoFormats;
  }

  @Override
  public String getLayerName() {
    return getTitle();
  }

  @Override
  public String getName() {
    return getTitle();
  }

  @Override
  public double getResolution(double metresPerPixel) {
    return getTileMatrixSet().getTileLevelResolution(metresPerPixel);
  }

  public List<WmtsResourceUrl> getResourceUrls() {
    return this.resourceUrls;
  }

  @Override
  public UrlResource getServiceUrl() {
    return getClient().getServiceUrl();
  }

  public BufferedImage getTileImage(WmtsTileMatrix tileMatrix, int tileX, int tileY) {
    JsonObject params = JsonObject.hash()
      .addValue("TileMatrix", tileMatrix.getLevel())
      .addValue("TileCol", tileX)
      .addValue("TileRow", tileY);
    String url = defaultUriTemplate.expandString(params);
    return BufferedImages.readImageIo(url);
  }

  public WmtsTileMatrixSet getTileMatrixSet() {
    return this.tileMatrixSetLink.getTileMatrixSet();
  }

  public TileMatrixSetLink getTileMatrixSetLink() {
    return this.tileMatrixSetLink;
  }

  @Override
  public WmtsClient getWebService() {
    return getClient();
  }

  public WmtsContents getWmtsContents() {
    return this.wmtsContents;
  }
}
