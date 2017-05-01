package com.revolsys.swing.map.layer.elevation.gridded;

import java.awt.Graphics2D;

import com.revolsys.collection.map.MapEx;
import com.revolsys.elevation.gridded.GriddedElevationModel;
import com.revolsys.elevation.gridded.GriddedElevationModelImage;
import com.revolsys.elevation.gridded.HillShadeConfiguration;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.AbstractLayerRenderer;
import com.revolsys.swing.map.layer.raster.GeoreferencedImageLayerRenderer;

public class GriddedElevationModelLayerRenderer
  extends AbstractLayerRenderer<GriddedElevationModelLayer> {

  private GriddedElevationModelImage image;

  private Thread worker;

  public GriddedElevationModelLayerRenderer(final GriddedElevationModelLayer layer) {
    super("raster", layer);
    final GriddedElevationModel elevationModel = layer.getElevationModel();
    if (elevationModel != null) {
      this.image = new GriddedElevationModelImage(elevationModel);
      layer.addPropertyChangeListener("refresh", this.image = null);
    }
  }

  @Override
  public void render(final Viewport2D viewport, final GriddedElevationModelLayer layer) {
    final double scaleForVisible = viewport.getScaleForVisible();
    if (layer.isVisible(scaleForVisible)) {
      if (!layer.isEditable()) {
        final GriddedElevationModel elevationModel = layer.getElevationModel();
        if (elevationModel != null) {
          if (this.image == null) {
            synchronized (this) {
              if (this.worker == null) {
                this.worker = new Thread(() -> {
                  final GriddedElevationModelImage image = new GriddedElevationModelImage(
                    elevationModel);
                  image.refresh(new HillShadeConfiguration(elevationModel));
                  // image.refresh(elevationModel);
                  this.image = image;
                  layer.refresh();
                });
                this.worker.start();
              }
            }
          } else {
            final BoundingBox boundingBox = layer.getBoundingBox();
            final Graphics2D graphics = viewport.getGraphics();
            if (graphics != null) {
              GeoreferencedImageLayerRenderer.renderAlpha(viewport, graphics, this.image, true,
                layer.getOpacity() / 255.0);
              GeoreferencedImageLayerRenderer.renderDifferentCoordinateSystem(viewport, graphics,
                boundingBox);
            }
          }
        }
      }
    }
  }

  @Override
  public MapEx toMap() {
    return MapEx.EMPTY;
  }
}
