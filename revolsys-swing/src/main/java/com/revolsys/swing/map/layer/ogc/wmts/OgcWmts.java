package com.revolsys.swing.map.layer.ogc.wmts;

import java.util.function.Function;

import com.revolsys.gis.wmts.capabilities.WmtsLayerDefinition;
import com.revolsys.io.map.MapObjectFactoryRegistry;
import com.revolsys.swing.map.layer.BaseMapLayer;
import com.revolsys.swing.menu.MenuFactory;

public class OgcWmts {
  public static void factoryInit() {
    MapObjectFactoryRegistry.newFactory("ogcWmtsImageLayer", "OGC WMTS Image Layer", (config) -> {
      return new OgcWmtsTiledImageLayer(config);
    });

    MenuFactory.addMenuInitializer(WmtsLayerDefinition.class, (menu) -> {
      final Function<WmtsLayerDefinition, BaseMapLayer> baseMapLayerFactory = OgcWmtsTiledImageLayer::new;
      BaseMapLayer.addNewLayerMenu(menu, baseMapLayerFactory);
    });

  }
}
