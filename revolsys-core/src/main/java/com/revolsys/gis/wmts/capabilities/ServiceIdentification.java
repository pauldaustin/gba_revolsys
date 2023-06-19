package com.revolsys.gis.wmts.capabilities;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.revolsys.record.io.format.xml.XmlUtil;

public class ServiceIdentification extends OwsDescription {

  private final String serviceType;

  private final List<String> serviceTypeVersions = new ArrayList<>();

  private final List<String> profiles;

  public ServiceIdentification(final Element element) {
    super(element);
    this.serviceType = XmlUtil.getFirstElementText(element, "ServiceType");
    this.profiles = XmlUtil.getListString(element, "Profile");
    // TODO fees
    // TODO AccessConstraints
  }

  public List<String> getProfiles() {
    return this.profiles;
  }

  public String getServiceType() {
    return this.serviceType;
  }

  public List<String> getServiceTypeVersions() {
    return this.serviceTypeVersions;
  }

}
