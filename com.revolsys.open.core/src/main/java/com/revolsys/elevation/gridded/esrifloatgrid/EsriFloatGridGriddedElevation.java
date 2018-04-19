package com.revolsys.elevation.gridded.esrifloatgrid;

import java.util.Map;

import com.revolsys.collection.map.MapEx;
import com.revolsys.elevation.gridded.GriddedElevationModel;
import com.revolsys.elevation.gridded.GriddedElevationModelReadFactory;
import com.revolsys.elevation.gridded.GriddedElevationModelReader;
import com.revolsys.geometry.io.PointReader;
import com.revolsys.geometry.io.PointReaderFactory;
import com.revolsys.io.AbstractIoFactoryWithCoordinateSystem;
import com.revolsys.spring.resource.Resource;

public class EsriFloatGridGriddedElevation extends AbstractIoFactoryWithCoordinateSystem
  implements GriddedElevationModelReadFactory, PointReaderFactory {
  public static final String FILE_EXTENSION = "flt";

  public static final String FILE_EXTENSION_ZIP = FILE_EXTENSION + ".zip";

  public static final String PROPERTY_READ_DATA = "readData";

  public EsriFloatGridGriddedElevation() {
    super("ESRI Float Grid");
    addMediaTypeAndFileExtension("image/x-esri-float-grid", FILE_EXTENSION);
    addFileExtension(FILE_EXTENSION_ZIP);
  }

  @Override
  public GriddedElevationModel newGriddedElevationModel(final Resource resource,
    final Map<String, ? extends Object> properties) {
    try (
      EsriFloatGridGriddedElevationModelReader reader = new EsriFloatGridGriddedElevationModelReader(
        resource, properties)) {
      return reader.read();
    }
  }

  @Override
  public GriddedElevationModelReader newGriddedElevationModelReader(final Resource resource,
    final Map<String, ? extends Object> properties) {
    return new EsriFloatGridGriddedElevationModelReader(resource, properties);
  }

  @Override
  public PointReader newPointReader(final Resource resource, final MapEx properties) {
    return new EsriFloatGridGriddedElevationModelReader(resource, properties);
  }

}
