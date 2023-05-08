package com.revolsys.gis.wmts.capabilities;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.revolsys.record.io.format.xml.XmlUtil;

public class ServiceIdentification extends OwsDescription {

  private String serviceType;

  private List<String> serviceTypeVersions = new ArrayList<>();

  private List<String> profiles;

  public ServiceIdentification(final Element element) {
    super(element);
    this.serviceType = XmlUtil.getFirstElementText(element, "ServiceType");
    this.profiles = XmlUtil.getListString(element, "Profile");
    // TODO fees
    // TODO AccessConstraints
  }

  public List<String> getProfiles() {
    return profiles;
  }

  public String getServiceType() {
    return serviceType;
  }

  public List<String> getServiceTypeVersions() {
    return serviceTypeVersions;
  }

}
