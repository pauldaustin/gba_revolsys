package com.revolsys.gis.wmts;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jeometry.common.exception.Exceptions;
import org.w3c.dom.Document;

import com.revolsys.gis.wmts.capabilities.WmtsCapabilities;
import com.revolsys.gis.wmts.capabilities.WmtsLayerDefinition;
import com.revolsys.spring.resource.UrlResource;
import com.revolsys.util.Property;
import com.revolsys.webservice.AbstractWebService;

public class WmtsClient extends AbstractWebService<WmtsLayerDefinition> {

  public static final String J_TYPE = "ogcWmtsServer";

  public static WmtsClient newOgcWmtsClient(final Map<String, ? extends Object> properties) {
    final String serviceUrl = (String)properties.get("serviceUrl");
    if (Property.hasValue(serviceUrl)) {
      final WmtsClient client = new WmtsClient(serviceUrl);
      client.setProperties(properties);
      return client;
    } else {
      throw new IllegalArgumentException("Missing serviceUrl");
    }
  }

  private WmtsCapabilities capabilities;

  private final String capabilitiesUrl;

  public WmtsClient(final String capabilitiesUrl) {
    this.capabilitiesUrl = capabilitiesUrl;
  }

  public WmtsCapabilities getCapabilities() {
    if (this.capabilities == null) {
      loadCapabilities();
    }
    return this.capabilities;
  }

  @Override
  public List<WmtsLayerDefinition> getChildren() {
    return getCapabilities().getContents().getLayers();
  }

  public WmtsLayerDefinition getLayer(final String id) {
    return getCapabilities().getContents().getLayer(id);
  }

  @Override
  public String getWebServiceTypeName() {
    return J_TYPE;
  }

  public boolean isConnected() {
    return this.capabilities != null;
  }

  public WmtsCapabilities loadCapabilities() {
    try (
      InputStream in = new UrlResource(this.capabilitiesUrl).getInputStream()) {
      final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setValidating(false);
      documentBuilderFactory.setNamespaceAware(true);
      final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      final Document document = documentBuilder.parse(in);
      this.capabilities = new WmtsCapabilities(this, document.getDocumentElement());
      return this.capabilities;
    } catch (final Throwable e) {
      throw Exceptions.wrap("Unable to read capabilities: " + new UrlResource(this.capabilitiesUrl),
        e);
    }
  }

  @Override
  public void refresh() {
    loadCapabilities();
  }

}
