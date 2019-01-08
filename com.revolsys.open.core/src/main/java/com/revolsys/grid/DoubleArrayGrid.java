package com.revolsys.grid;

import java.util.Arrays;
import java.util.function.DoubleConsumer;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;

public class DoubleArrayGrid extends AbstractGrid {
  protected static final double NULL_VALUE = -Double.MAX_VALUE;

  protected final double[] cells;

  public DoubleArrayGrid(final double x, final double y, final int gridWidth, final int gridHeight,
    final double gridCellWidth, final double gridCellHeight, final double[] values) {
    this(GeometryFactory.DEFAULT_3D, x, y, gridWidth, gridHeight, gridCellWidth, gridCellHeight,
      values);
  }

  public DoubleArrayGrid(final double x, final double y, final int gridWidth, final int gridHeight,
    final double gridCellSize, final double[] values) {
    this(x, y, gridWidth, gridHeight, gridCellSize, gridCellSize, values);
  }

  public DoubleArrayGrid(final GeometryFactory geometryFactory, final BoundingBox boundingBox,
    final int gridWidth, final int gridHeight, final double gridCellWidth,
    final double gridCellHeight, final double[] values) {
    super(geometryFactory, boundingBox, gridWidth, gridHeight, gridCellWidth, gridCellHeight);
    this.cells = values;
    expandRange();
  }

  public DoubleArrayGrid(final GeometryFactory geometryFactory, final BoundingBox boundingBox,
    final int gridWidth, final int gridHeight, final double gridCellSize, final double[] values) {
    this(geometryFactory, boundingBox, gridWidth, gridHeight, gridCellSize, gridCellSize, values);
  }

  public DoubleArrayGrid(final GeometryFactory geometryFactory, final double x, final double y,
    final int gridWidth, final int gridHeight, final double gridCellSize) {
    this(geometryFactory, x, y, gridWidth, gridHeight, gridCellSize, gridCellSize);
  }

  public DoubleArrayGrid(final GeometryFactory geometryFactory, final double x, final double y,
    final int gridWidth, final int gridHeight, final double gridCellWidth,
    final double gridCellHeight) {
    super(geometryFactory, x, y, gridWidth, gridHeight, gridCellWidth, gridCellHeight);
    final int size = gridWidth * gridHeight;
    final double[] values = new double[size];
    Arrays.fill(values, NULL_VALUE);
    this.cells = values;
  }

  public DoubleArrayGrid(final GeometryFactory geometryFactory, final double x, final double y,
    final int gridWidth, final int gridHeight, final double gridCellWidth,
    final double gridCellHeight, final double[] values) {
    super(geometryFactory, x, y, gridWidth, gridHeight, gridCellWidth, gridCellHeight);
    this.cells = values;
    expandRange();
  }

  public DoubleArrayGrid(final GeometryFactory geometryFactory, final double x, final double y,
    final int gridWidth, final int gridHeight, final double gridCellSize, final double[] values) {
    this(geometryFactory, x, y, gridWidth, gridHeight, gridCellSize, gridCellSize, values);
  }

  @Override
  public void clear() {
    super.clear();
    Arrays.fill(this.cells, NULL_VALUE);
  }

  @Override
  protected void expandRange() {
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    for (final double value : this.cells) {
      if (value != NULL_VALUE) {
        if (value < min) {
          min = value;
        }
        if (value > max) {
          max = value;
        }
      }
    }
    final double minZ = min;
    final double maxZ = max;
    setValueRange(minZ, maxZ);

  }

  @Override
  public void forEachValueFinite(final DoubleConsumer action) {
    for (final double value : this.cells) {
      if (value != NULL_VALUE) {
        action.accept(value);
      }
    }
  }

  public double[] getCellsDouble() {
    return this.cells;
  }

  @Override
  public double getValueFast(final int gridX, final int gridY) {
    final int index = gridY * this.gridWidth + gridX;
    final double value = this.cells[index];
    if (value == NULL_VALUE) {
      return Double.NaN;
    } else {
      return value;
    }
  }

  @Override
  public boolean hasValueFast(final int gridX, final int gridY) {
    final int index = gridY * this.gridWidth + gridX;
    final double value = this.cells[index];
    if (value == NULL_VALUE) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public DoubleArrayGrid newGrid(final GeometryFactory geometryFactory, final double x,
    final double y, final int width, final int height, final double cellSize) {
    return new DoubleArrayGrid(geometryFactory, x, y, width, height, cellSize);
  }

  public DoubleArrayGrid newGrid(final int gridWidth, final int gridHeight,
    final double gridCellWidth, final double gridCellHeight, final double[] newValues) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final BoundingBox boundingBox = getBoundingBox();
    return new DoubleArrayGrid(geometryFactory, boundingBox, gridWidth, gridHeight, gridCellWidth,
      gridCellHeight, newValues);
  }

  @Override
  public DoubleArrayGrid resample(final int newGridCellSize) {
    final double gridCellWidth = getGridCellWidth();
    final double gridCellHeight = getGridCellHeight();
    final double cellRatioX = gridCellWidth / newGridCellSize;
    final double cellRatioY = gridCellHeight / newGridCellSize;
    final int stepX = (int)Math.round(1 / cellRatioX);
    final int stepY = (int)Math.round(1 / cellRatioY);
    final int gridWidth = getGridWidth();
    final int gridHeight = getGridHeight();

    final int newGridWidth = (int)Math.round(gridWidth * cellRatioX);
    final int newGridHeight = (int)Math.round(gridHeight * cellRatioY);

    final double[] oldValues = this.cells;
    final double[] newValues = new double[newGridWidth * newGridHeight];

    int newIndex = 0;
    for (int gridYMin = 0; gridYMin < gridHeight; gridYMin += stepY) {
      final int gridYMax = gridYMin + stepY;
      for (int gridXMin = 0; gridXMin < gridWidth; gridXMin += stepX) {
        final int gridXMax = gridXMin + stepX;
        int count = 0;
        long sum = 0;
        for (int gridY = gridYMin; gridY < gridYMax; gridY++) {
          for (int gridX = gridXMin; gridX < gridXMax; gridX++) {
            final double oldValue = oldValues[gridY * gridWidth + gridX];
            if (oldValue != NULL_VALUE) {
              count++;
              sum += oldValue;
            }
          }
        }
        if (count > 0) {
          newValues[newIndex] = (int)(sum / count);
        } else {
          newValues[newIndex] = NULL_VALUE;
        }
        newIndex++;
      }
    }
    return newGrid(newGridWidth, newGridHeight, newGridCellSize, newGridCellSize, newValues);
  }

  @Override
  public void setValue(final int gridX, final int gridY, final double value) {
    final int width = getGridWidth();
    final int height = getGridHeight();
    if (gridX >= 0 && gridX < width && gridY >= 0 && gridY < height) {
      final int index = gridY * width + gridX;
      final double valueDouble = value;
      this.cells[index] = valueDouble;
      clearCachedObjects();
    }
  }

  @Override
  public void setValueNull(final int gridX, final int gridY) {
    final int width = getGridWidth();
    final int height = getGridHeight();
    if (gridX >= 0 && gridX < width && gridY >= 0 && gridY < height) {
      final int index = gridY * width + gridX;
      this.cells[index] = NULL_VALUE;
      clearCachedObjects();
    }
  }
}
