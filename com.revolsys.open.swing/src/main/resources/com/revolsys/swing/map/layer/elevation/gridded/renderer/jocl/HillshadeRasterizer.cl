 #pragma OPENCL EXTENSION cl_khr_fp64 : enable
 
 float slope(float dzDivDx, float dzDivDy, float zFactor) {
    return atan(zFactor * sqrt(dzDivDx * dzDivDx + dzDivDy * dzDivDy));
 }
 
float aspect(float dzDivDx, float dzDivDy) {
  double aspect = 0;
  if (dzDivDx == 0) {
    if (dzDivDy > 0) {
      aspect = M_PI * 2;
    } else if (dzDivDy < 0) {
      aspect = M_PI * 2 - M_PI / 2;
    } else {
      aspect = 0;
    }
  } else {
    aspect = atan2(dzDivDy, -dzDivDx);
    if (aspect < 0) {
      aspect = M_PI * 2 + aspect;
    }
  }
  return aspect;
}

int fixNulls3x3(float* m) {
  if (isnan(m[4])) {
    return 0;
  } else {
    if (isnan(m[3])) {
      if (isnan(m[5])) {
        m[3] = m[4];
        m[5] = m[4];
      } else {
        m[3] = m[4] - (m[5] - m[4]);
      }
    } else if (isnan(m[5])) {
      m[5] = m[4];
    }
    if (isnan(m[0])) {
      if (isnan(m[6])) {
        m[0] = m[3];
      } else {
        m[0] = m[3] - (m[6] - m[3]);
      }
    }
    if (isnan(m[1])) {
      if (isnan(m[7])) {
        m[1] = m[4];
      } else {
        m[1] = m[4] - (m[7] - m[4]);
      }
    }
    if (isnan(m[2])) {
      if (isnan(m[8])) {
        m[2] = m[5];
      } else {
        m[2] = m[5] - (m[8] - m[5]);
      }
    }
    if (isnan(m[6])) {
      m[6] = m[3] - (m[0] - m[3]);
    }
    if (isnan(m[7])) {
      m[7] = m[4] - (m[1] - m[4]);
    }
    if (isnan(m[8])) {
      m[8] = m[5] - (m[2] - m[5]);
    }
    return 1;
  }
}

int subGridInt3x3(__global int *cells, int width, int height, float* m, int imageX, int imageY, float offsetZ, float scaleZ) {
  int gridX = imageX;
  int gridY = height - imageY - 1;

  for (int i = 0; i < 9; i++) {
    m[i] = NAN;
  }
  int startY = gridY - 1;
  if (startY < 0) {
    startY = 0;
  }
  int endY = gridY + 1;
  if (endY >= height) {
    endY = height - 1;
  }
  int startX = gridX - 1;
  if (startX < 0) {
    startX = 0;
  }
  int endX = gridX + 1;
  if (endX >= width) {
    endX = width - 1;
  }
  int i = 0;
  for (int y = endY; y >= startY; y--) {
    for (int x = startX; x <= endX; x++) {
      int zInt = cells[y * width + x];
      if (zInt != -2147483648) {
        m[i] = offsetZ + zInt / scaleZ;
      } 
      i++;
    }
  }
  return fixNulls3x3(m);
}

int subGridFloat3x3(__global float *cells, int width, int height, float* m, int imageX, int imageY) {
  int gridX = imageX;
  int gridY = height - imageY - 1;

  for (int i = 0; i < 9; i++) {
    m[i] = NAN;
  }
  int startY = gridY - 1;
  if (startY < 0) {
    startY = 0;
  }
  int endY = gridY + 1;
  if (endY >= height) {
    endY = height - 1;
  }
  int startX = gridX - 1;
  if (startX < 0) {
    startX = 0;
  }
  int endX = gridX + 1;
  if (endX >= width) {
    endX = width - 1;
  }
  int i = 0;
  for (int y = endY; y >= startY; y--) {
    for (int x = startX; x <= endX; x++) {
      m[i] = cells[y * width + x];
      i++;
    }
  }
  return fixNulls3x3(m);
}

int subGridDouble3x3(__global double *cells, int width, int height, float* m, int imageX, int imageY) {
  int gridX = imageX;
  int gridY = height - imageY - 1;

  for (int i = 0; i < 9; i++) {
    m[i] = NAN;
  }
  int startY = gridY - 1;
  if (startY < 0) {
    startY = 0;
  }
  int endY = gridY + 1;
  if (endY >= height) {
    endY = height - 1;
  }
  int startX = gridX - 1;
  if (startX < 0) {
    startX = 0;
  }
  int endX = gridX + 1;
  if (endX >= width) {
    endX = width - 1;
  }
  int i = 0;
  for (int y = endY; y >= startY; y--) {
    for (int x = startX; x <= endX; x++) {
      m[i] = (float)cells[y * width + x];
      i++;
    }
  }
  return fixNulls3x3(m);
}
float deltaZX(float* m, float xFactor) {
  return (m[2] + 2 * m[5] + m[8] - (m[0] + 2 * m[3] + m[6])) * xFactor;
}

float deltaZY(float* m, float yFactor) {
  return (m[6] + 2 * m[7] + m[8] - (m[0] + 2 * m[1] + m[2])) * yFactor;
}

char4 hillshade(
  float *m,
  const int isNull,
  const float azimuthRadians,
  const float cosZenithRadians,
  const float sinZenithRadians,
  const float xFactor,
  const float yFactor,
  const float zFactor,
  __global char4 *output
) {
  if (isNull == 0) {
    return (char4)(0);
  } else {
    float dzDivDx = deltaZX(m, xFactor);
    float dzDivDy = deltaZY(m, yFactor);

    float slopeRadians = slope(dzDivDx, dzDivDy, zFactor);
    float aspectRadians = aspect(dzDivDx, dzDivDy);
    
    int hs = round((cosZenithRadians * cos(slopeRadians) + sinZenithRadians
      * sin(slopeRadians) * cos(azimuthRadians - aspectRadians)) * 255);
    if (hs < 0) {
      hs = 0;
    } else if (hs > 255) {
      hs = 255;
    }
    return (char4)(hs, hs, hs, 255);
  }
}

 __kernel void hillshadeRasterizer_int(
  __global int *cells,
  const int width,
  const int height,
  const float offsetZ,
  const float scaleZ,
  const float azimuthRadians,
  const float cosZenithRadians,
  const float sinZenithRadians,
  const float xFactor,
  const float yFactor,
  const float zFactor,
  __global char4 *output
) {
  int imageX = get_global_id(0);
  int imageY = get_global_id(1);

  float m[9];
  int isNull = subGridInt3x3(cells, width, height, m, imageX, imageY, offsetZ, scaleZ);
  output[imageY * width + imageX] = hillshade(m, isNull, azimuthRadians, cosZenithRadians, sinZenithRadians, xFactor, yFactor, zFactor, output);
}

 __kernel void hillshadeRasterizer_float(
  __global float *cells,
  const int width,
  const int height,
  const float azimuthRadians,
  const float cosZenithRadians,
  const float sinZenithRadians,
  const float xFactor,
  const float yFactor,
  const float zFactor,
  __global char4 *output
) {
  int imageX = get_global_id(0);
  int imageY = get_global_id(1);

  float m[9];
  int isNull = subGridFloat3x3(cells, width, height, m, imageX, imageY);
  output[imageY * width + imageX] = hillshade(m, isNull, azimuthRadians, cosZenithRadians, sinZenithRadians, xFactor, yFactor, zFactor, output);
}

 __kernel void hillshadeRasterizer_double(
  __global double *cells,
  const int width,
  const int height,
  const float azimuthRadians,
  const float cosZenithRadians,
  const float sinZenithRadians,
  const float xFactor,
  const float yFactor,
  const float zFactor,
  __global char4 *output
) {
  int imageX = get_global_id(0);
  int imageY = get_global_id(1);

  float m[9];
  int isNull = subGridDouble3x3(cells, width, height, m, imageX, imageY);
  output[imageY * width + imageX] = hillshade(m, isNull, azimuthRadians, cosZenithRadians, sinZenithRadians, xFactor, yFactor, zFactor, output);
}