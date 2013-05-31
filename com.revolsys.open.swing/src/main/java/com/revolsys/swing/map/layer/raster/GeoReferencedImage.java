package com.revolsys.swing.map.layer.raster;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.cs.WktCsParser;
import com.revolsys.gis.cs.esri.EsriCoordinateSystems;
import com.revolsys.spring.SpringUtil;
import com.revolsys.swing.map.layer.MapTile;
import com.revolsys.swing.map.layer.ProjectionImageFilter;

public class GeoReferencedImage {

  private BoundingBox boundingBox;

  private BufferedImage image;

  private int imageWidth = -1;

  private int imageHeight = -1;

  private GeometryFactory geometryFactory = GeometryFactory.getFactory();

  private PlanarImage jaiImage;

  public GeoReferencedImage() {
  }

  public GeoReferencedImage(final BoundingBox boundingBox,
    final BufferedImage image) {
    this(boundingBox, image.getWidth(), image.getHeight());
    setImage(image);
  }

  public GeoReferencedImage(final BoundingBox boundingBox,
    final int imageWidth, final int imageHeight) {
    this.boundingBox = boundingBox;
    this.geometryFactory = boundingBox.getGeometryFactory();
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
  }

  public GeoReferencedImage(final Resource imageResource) {
    final File file = SpringUtil.getOrDownloadFile(imageResource);
    jaiImage = JAI.create("fileload", file.getAbsolutePath());
    setImage(jaiImage.getAsBufferedImage());
    loadImageMetaData(imageResource, jaiImage);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof MapTile) {
      final MapTile tile = (MapTile)obj;
      return tile.getBoundingBox().equals(boundingBox);
    }
    return false;
  }

  public BoundingBox getBoundingBox() {
    return boundingBox;
  }

  public CoordinateSystem getCoordinateSystem() {
    return geometryFactory.getCoordinateSystem();
  }

  public GeometryFactory getGeometryFactory() {
    return geometryFactory;
  }

  public BufferedImage getImage() {
    return image;
  }

  public GeoReferencedImage getImage(final CoordinateSystem coordinateSystem,
    final double resolution) {
    final int imageSrid = getGeometryFactory().getSRID();
    if (imageSrid > 0 && imageSrid != coordinateSystem.getId()) {
      final BoundingBox boundingBox = getBoundingBox();
      final ProjectionImageFilter filter = new ProjectionImageFilter(
        boundingBox, coordinateSystem, resolution);

      final BufferedImage newImage = filter.filter(getImage());

      final BoundingBox destBoundingBox = filter.getDestBoundingBox();
      return new GeoReferencedImage(destBoundingBox, newImage);
    }
    return this;
  }

  public double getImageAspectRatio() {
    final int imageWidth = getImageWidth();
    final int imageHeight = getImageHeight();
    if (imageWidth > 0 && imageHeight > 0) {
      return (double)imageWidth / imageHeight;
    } else {
      return 0;
    }
  }

  public int getImageHeight() {
    if (imageHeight == -1) {
      imageHeight = image.getHeight();
    }
    return imageHeight;
  }

  public int getImageWidth() {
    if (imageWidth == -1) {
      imageWidth = image.getWidth();
    }
    return imageWidth;
  }

  public PlanarImage getJaiImage() {
    if (jaiImage == null && image != null) {
      jaiImage = PlanarImage.wrapRenderedImage(image);
    }
    return jaiImage;
  }

  @Override
  public int hashCode() {
    return boundingBox.hashCode();
  }

  public BufferedImage loadImage() {
    return image;
  }

  protected void loadImageMetaData(final Resource imageResource,
    final PlanarImage jaiImage) {
  }

  protected void loadProjectionFile(final Resource resource) {
    final Resource projectionFile = SpringUtil.getResourceWithExtension(
      resource, "prj");
    if (projectionFile.exists()) {
      CoordinateSystem coordinateSystem = new WktCsParser(projectionFile).parse();
      coordinateSystem = EsriCoordinateSystems.getCoordinateSystem(coordinateSystem);
      setCoordinateSystem(coordinateSystem);
    }
  }

  protected void loadWorldFile(final Resource resource,
    final String worldFileExtension) {

    final Resource worldFile = SpringUtil.getResourceWithExtension(resource,
      worldFileExtension);
    if (worldFile.exists()) {
      try {
        final BufferedReader reader = SpringUtil.getBufferedReader(resource);
        try {
          final double xPixelSize = Double.parseDouble(reader.readLine());
          final double yRotation = Double.parseDouble(reader.readLine());
          final double xRotation = Double.parseDouble(reader.readLine());
          final double yPixelSize = Double.parseDouble(reader.readLine());
          final double xCoordinate = Double.parseDouble(reader.readLine());
          final double yCoordinate = Double.parseDouble(reader.readLine());

          final double[] transformationMatrix = {
            xPixelSize, yRotation, xRotation, yPixelSize, xCoordinate,
            yCoordinate
          };
          System.out.println(transformationMatrix);
          // setTransformationMatrix(transformationMatrix);
          // final BoundingBox boundingBox = new
          // BoundingBox(getCoordinateSystem());
        } finally {
          reader.close();
        }
      } catch (final IOException e) {
        LoggerFactory.getLogger(getClass()).error(
          "Error reading world file " + worldFile, e);
      }
    }
  }

  protected void setBoundingBox(final BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public void setCoordinateSystem(final CoordinateSystem coordinateSystem) {
    setGeometryFactory(GeometryFactory.getFactory(coordinateSystem));
  }

  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public void setImage(final BufferedImage image) {
    this.image = image;
  }

  protected void setImageHeight(final int imageHeight) {
    this.imageHeight = imageHeight;
  }

  protected void setImageWidth(final int imageWidth) {
    this.imageWidth = imageWidth;
  }

  public void setJaiImage(final RenderedOp jaiImage) {
    this.jaiImage = jaiImage;
  }
}
