package com.revolsys.xml.wadl;

import javax.xml.namespace.QName;

public interface WadlConstants {
  String _NS_URI = "http://research.sun.com/wadl/2006/10";

  String _NS_PREFIX = "wadl";

  QName APPLICATION = new QName(_NS_URI, "application", _NS_PREFIX);

  QName DOC = new QName(_NS_URI, "doc", _NS_PREFIX);

  QName RESOURCES = new QName(_NS_URI, "resources", _NS_PREFIX);

  QName RESOURCE = new QName(_NS_URI, "resource", _NS_PREFIX);

  QName METHOD = new QName(_NS_URI, "method", _NS_PREFIX);

  QName REQUEST = new QName(_NS_URI, "request", _NS_PREFIX);

  QName RESPONSE = new QName(_NS_URI, "response", _NS_PREFIX);

  QName REPRESENTATION = new QName(_NS_URI, "representation", _NS_PREFIX);

  QName MEDIA_TYPE = new QName("mediaType");

  QName NAME = new QName("name");

  QName PATH = new QName("path");

  QName TITLE = new QName("title");
}
