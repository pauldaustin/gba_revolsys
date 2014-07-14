package com.revolsys.swing.pdf;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.measure.Measure;
import javax.measure.quantity.Length;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.PDLineDashPattern;
import org.springframework.util.StringUtils;

import com.revolsys.gis.model.coordinates.PointWithOrientation;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.LinearRing;
import com.revolsys.jts.geom.Polygon;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.renderer.TextStyleRenderer;
import com.revolsys.swing.map.layer.record.style.GeometryStyle;
import com.revolsys.swing.map.layer.record.style.TextStyle;

public class PdfViewport extends Viewport2D implements AutoCloseable {

  private final PDDocument document;

  private final PDPage page;

  private final PDPageContentStream contentStream;

  private final Set<Float> alphaSet = new HashSet<>();

  private int styleId = 0;

  private final Map<GeometryStyle, String> styleNames = new HashMap<>();

  private final Canvas canvas = new Canvas();

  public PdfViewport(final PDDocument document, final PDPage page,
    final Project project, final int width, final int height,
    final BoundingBox boundingBox) throws IOException {
    super(project, width, height, boundingBox);
    this.document = document;
    this.page = page;
    this.contentStream = new PDPageContentStream(document, page);
  }

  @Override
  public void close() throws IOException {
    contentStream.close();
  }

  @Override
  public void drawGeometry(final Geometry geometry, final GeometryStyle style) {
    try {
      contentStream.saveGraphicsState();
      setGeometryStyle(style);
      contentStream.setNonStrokingColor(style.getPolygonFill());
      contentStream.setStrokingColor(style.getLineColor());

      for (Geometry part : geometry.geometries()) {
        part = part.convert(getGeometryFactory());
        if (part instanceof LineString) {
          final LineString line = (LineString)part;

          drawLine(line);
          contentStream.stroke();
        } else if (part instanceof Polygon) {
          final Polygon polygon = (Polygon)part;

          int i = 0;
          for (final LinearRing ring : polygon.rings()) {
            if (i == 0) {
              if (ring.isClockwise()) {
                drawLineReverse(ring);
              } else {
                drawLine(ring);
              }
            } else {
              if (ring.isCounterClockwise()) {
                drawLineReverse(ring);
              } else {
                drawLine(ring);
              }
            }
            contentStream.closeSubPath();
            i++;
          }
          contentStream.fill(PathIterator.WIND_NON_ZERO);
          for (final LinearRing ring : polygon.rings()) {

            drawLine(ring);
            contentStream.stroke();
          }
        }
      }

    } catch (final IOException e) {
      e.printStackTrace();
    } finally {
      try {
        contentStream.restoreGraphicsState();
      } catch (final IOException e) {
      }
    }
  }

  private void drawLine(final LineString line) throws IOException {
    for (int i = 0; i < line.getVertexCount(); i++) {
      final double modelX = line.getX(i);
      final double modelY = line.getY(i);
      final double[] viewCoordinates = toViewCoordinates(modelX, modelY);
      final float viewX = (float)viewCoordinates[0];
      final float viewY = (float)(getViewHeightPixels() - viewCoordinates[1]);
      if (i == 0) {
        contentStream.moveTo(viewX, viewY);
      } else {
        contentStream.lineTo(viewX, viewY);
      }
    }
  }

  private void drawLineReverse(final LineString line) throws IOException {
    final int toVertexIndex = line.getVertexCount() - 1;
    for (int i = toVertexIndex; i >= 0; i--) {
      final double modelX = line.getX(i);
      final double modelY = line.getY(i);
      final double[] viewCoordinates = toViewCoordinates(modelX, modelY);
      final float viewX = (float)viewCoordinates[0];
      final float viewY = (float)(getViewHeightPixels() - viewCoordinates[1]);
      if (i == toVertexIndex) {
        contentStream.moveTo(viewX, viewY);
      } else {
        contentStream.lineTo(viewX, viewY);
      }
    }
  }

  @Override
  public void drawText(final LayerRecord object, final Geometry geometry,
    final TextStyle style) {
    try {
      final String label = TextStyleRenderer.getLabel(object, style);
      if (StringUtils.hasText(label) && geometry != null) {
        final PointWithOrientation point = TextStyleRenderer.getTextLocation(
          this, geometry, style);
        if (point != null) {
          final double orientation = point.getOrientation();

          contentStream.saveGraphicsState();
          try {
            // style.setTextStyle(viewport, graphics);

            final double x = point.getX();
            final double y = point.getY();
            final double[] location = toViewCoordinates(x, y);

            // style.setTextStyle(viewport, graphics);

            final Measure<Length> textDx = style.getTextDx();
            double dx = Viewport2D.toDisplayValue(this, textDx);

            final Measure<Length> textDy = style.getTextDy();
            double dy = Viewport2D.toDisplayValue(this, textDy);
            final Font font = style.getFont(this);
            final FontMetrics fontMetrics = canvas.getFontMetrics(font);

            double maxWidth = 0;
            final String[] lines = label.split("[\\r\\n]");
            for (final String line : lines) {
              final Rectangle2D bounds = fontMetrics.getStringBounds(line,
                canvas.getGraphics());
              final double width = bounds.getWidth();
              maxWidth = Math.max(width, maxWidth);
            }
            final int descent = fontMetrics.getDescent();
            final int ascent = fontMetrics.getAscent();
            final int leading = fontMetrics.getLeading();
            final double maxHeight = lines.length * (ascent + descent)
              + (lines.length - 1) * leading;
            final String verticalAlignment = style.getTextVerticalAlignment();
            if ("top".equals(verticalAlignment)) {
            } else if ("middle".equals(verticalAlignment)) {
              dy += maxHeight / 2;
            } else {
              dy += maxHeight;
            }

            String horizontalAlignment = style.getTextHorizontalAlignment();
            double screenX = location[0];
            double screenY = getViewHeightPixels() - location[1];
            final String textPlacementType = style.getTextPlacementType();
            if ("auto".equals(textPlacementType)) {
              if (screenX < 0) {
                screenX = 1;
                dx = 0;
                horizontalAlignment = "left";
              }
              final int viewWidth = getViewWidthPixels();
              if (screenX + maxWidth > viewWidth) {
                screenX = (int)(viewWidth - maxWidth - 1);
                dx = 0;
                horizontalAlignment = "left";
              }
              if (screenY < maxHeight) {
                screenY = 1;
                dy = 0;
              }
              final int viewHeight = getViewHeightPixels();
              if (screenY > viewHeight) {
                screenY = viewHeight - 1 - maxHeight;
                dy = 0;
              }
            }
            AffineTransform transform = new AffineTransform();
            transform.translate(screenX, screenY);
            if (orientation != 0) {
              transform.rotate(-Math.toRadians(orientation), 0, 0);
            }
            transform.translate(dx, dy);

            for (int i = 0; i < lines.length; i++) {
              final String line = lines[i];
              transform.translate(0, ascent);
              final AffineTransform lineTransform = new AffineTransform(
                transform);
              final Rectangle2D bounds = fontMetrics.getStringBounds(line,
                canvas.getGraphics());
              final double width = bounds.getWidth();
              final double height = bounds.getHeight();

              if ("right".equals(horizontalAlignment)) {
                transform.translate(-width, 0);
              } else if ("center".equals(horizontalAlignment)) {
                transform.translate(-width / 2, 0);
              }
              transform.translate(dx, 0);

              transform.scale(1, 1);
              if (Math.abs(orientation) > 90) {
                transform.rotate(Math.PI, maxWidth / 2, -height / 4);
              }
              /*
               * final double textHaloRadius = Viewport2D.toDisplayValue(this,
               * style.getTextHaloRadius()); if (textHaloRadius > 0) {
               * graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
               * RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB); final Stroke
               * savedStroke = graphics.getStroke(); final Stroke outlineStroke
               * = new BasicStroke( (float)textHaloRadius, BasicStroke.CAP_BUTT,
               * BasicStroke.JOIN_BEVEL);
               * graphics.setColor(style.getTextHaloFill());
               * graphics.setStroke(outlineStroke); final Font font =
               * graphics.getFont(); final FontRenderContext fontRenderContext =
               * graphics.getFontRenderContext(); final TextLayout textLayout =
               * new TextLayout(line, font, fontRenderContext); final Shape
               * outlineShape =
               * textLayout.getOutline(TextStyleRenderer.NOOP_TRANSFORM);
               * graphics.draw(outlineShape); graphics.setStroke(savedStroke); }
               */
              final Color textBoxColor = style.getTextBoxColor();
              if (textBoxColor != null) {
                contentStream.setNonStrokingColor(textBoxColor);
                final double cornerSize = Math.max(height / 2, 5);
                // final RoundRectangle2D.Double box = new
                // RoundRectangle2D.Double(
                // bounds.getX() - 3, bounds.getY() - 1, width + 6, height + 2,
                // cornerSize, cornerSize);
                contentStream.fillRect((float)bounds.getX() - 3,
                  (float)bounds.getY() - 1, (float)width + 6, (float)height + 2);
              }
              contentStream.setNonStrokingColor(style.getTextFill());

              contentStream.beginText();
              final PDFont font2 = PDType1Font.HELVETICA_BOLD;

              contentStream.setFont(font2, font.getSize2D());
              contentStream.setTextMatrix(transform);
              contentStream.drawString(line);
              contentStream.endText();

              transform = lineTransform;
              transform.translate(0, (leading + descent));
            }

          } finally {
            contentStream.restoreGraphicsState();
          }
        }
      }
    } catch (final IOException e) {
      throw new RuntimeException("Unable to write PDF", e);
    }
  }

  @Override
  public boolean isHidden(final AbstractRecordLayer layer,
    final LayerRecord record) {
    return false;
  }

  private void setGeometryStyle(final GeometryStyle style) throws IOException {
    String styleName = styleNames.get(style);
    if (styleName == null) {
      styleName = "rgStyle" + styleId++;

      final PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();

      final int lineOpacity = style.getLineOpacity();
      if (lineOpacity != 255) {
        graphicsState.setStrokingAlphaConstant(lineOpacity / 255f);
      }

      final Measure<Length> lineWidth = style.getLineWidth();
      graphicsState.setLineWidth((float)toDisplayValue(lineWidth));

      final List<Measure<Length>> lineDashArray = style.getLineDashArray();
      if (lineDashArray != null && !lineDashArray.isEmpty()) {
        int size = lineDashArray.size();
        if (size == 1) {
          size++;
        }
        final float[] dashArray = new float[size];

        for (int i = 0; i < dashArray.length; i++) {
          if (i < lineDashArray.size()) {
            final Measure<Length> dash = lineDashArray.get(i);
            dashArray[i] = (float)toDisplayValue(dash);
          } else {
            dashArray[i] = dashArray[i - 1];
          }
        }
        final int offset = (int)toDisplayValue(style.getLineDashOffset());
        final COSArray dashCosArray = new COSArray();
        dashCosArray.setFloatArray(dashArray);
        final PDLineDashPattern pattern = new PDLineDashPattern(dashCosArray,
          offset);
        graphicsState.setLineDashPattern(pattern);
      }
      switch (style.getLineCap()) {
        case BUTT:
          graphicsState.setLineCapStyle(0);
        break;
        case ROUND:
          graphicsState.setLineCapStyle(1);
        break;
        case SQUARE:
          graphicsState.setLineCapStyle(2);
        break;
      }

      switch (style.getLineJoin()) {
        case MITER:
          graphicsState.setLineJoinStyle(0);
        break;
        case ROUND:
          graphicsState.setLineJoinStyle(1);
        break;
        case BEVEL:
          graphicsState.setLineJoinStyle(2);
        break;
      }

      final int polygonFillOpacity = style.getPolygonFillOpacity();
      if (polygonFillOpacity != 255) {
        graphicsState.setNonStrokingAlphaConstant(polygonFillOpacity / 255f);
      }

      final PDResources resources = page.findResources();
      Map<String, PDExtendedGraphicsState> graphicsStateDictionary = resources.getGraphicsStates();
      if (graphicsStateDictionary == null) {
        graphicsStateDictionary = new TreeMap<>();
      }
      graphicsStateDictionary.put(styleName, graphicsState);
      resources.setGraphicsStates(graphicsStateDictionary);

      styleNames.put(style, styleName);
    }
    contentStream.appendRawCommands("/" + styleName + " gs\n");
  }
}