package com.revolsys.gis.wmts.capabilities;

import org.w3c.dom.Element;

import com.revolsys.record.io.format.xml.XmlUtil;

public class OwsDescription {

  private String title;

  private String abstractText;

  private String keywords;

  public OwsDescription(final Element element) {
    // TODO all of these are lists due to languages
    this.title = XmlUtil.getFirstElementText(element, "Title");
    this.abstractText = XmlUtil.getFirstElementText(element, "Abstract");
    this.keywords = XmlUtil.getFirstElementText(element, "Keywords");
  }

  public String getAbstractText() {
    return abstractText;
  }

  public String getKeywords() {
    return keywords;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return title;
  }
}
