package com.revolsys.gis.wmts.capabilities;

import org.w3c.dom.Element;

import com.revolsys.gis.wmts.WmtsClient;
import com.revolsys.record.io.format.xml.XmlUtil;

public class WmtsCapabilities extends OwsCapabilities {

  private WmtsContents contents;

  private WmtsClient client;

  public WmtsCapabilities(WmtsClient client, final Element element) {
    super(element);
    this.client = client;
    XmlUtil.forFirstElement(element, "Contents", (serviceElement) -> {
      this.contents = new WmtsContents(this, serviceElement);
    });
  }

  public WmtsClient getClient() {
    return client;
  }

  public WmtsContents getContents() {
    return contents;
  }
}
