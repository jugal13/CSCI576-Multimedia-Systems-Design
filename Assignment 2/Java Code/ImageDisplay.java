import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

class HSV {
  float h, s, v;

  HSV(float h, float s, float v) {
    this.h = h;
    this.s = s;
    this.v = v;
  }
}

class RGB {
  int r, g, b;

  RGB(int r, int g, int b) {
    this.r = r;
    this.g = g;
    this.b = b;
  }
}

public class ImageDisplay {
  JFrame frame;
  JLabel label;

  BufferedImage originalImage;
  BufferedImage[] objectImages;

  String originalImagePath;
  String[] objectImagePaths;

  int[][] objectImageHSVBins;
  double[][] objectImageHSVBinsNormalised;

  // default image width and height
  int originalHeight = 480;
  int originalWidth = 640;

  private boolean validateArgs(String[] args) {
    if (args.length < 2) {
      System.out.println("Need an image and object atleast");
      return false;
    }

    System.out.println("Number of Arguments: " + args.length);
    System.out.println();

    originalImagePath = args[0];
    System.out.println("Original Image Path: " + originalImagePath);
    System.out.println();

    objectImagePaths = Arrays.copyOfRange(args, 1, args.length);
    objectImages = new BufferedImage[args.length - 1];
    objectImageHSVBins = new int[args.length - 1][15];
    objectImageHSVBinsNormalised = new double[args.length - 1][15];
    for (int i = 0; i < objectImagePaths.length; i++) {
      System.out.println("Object " + (i + 1) + " Path: " + objectImagePaths[i]);
    }

    System.out.println();
    System.out.println("Finished Initialisation");
    System.out.println();

    return true;
  }

  private int getPixel(byte r, byte g, byte b, boolean isObject) {
    int red = r & 0xff;
    int green = g & 0xff;
    int blue = b & 0xff;

    if (isObject && red == 0 && green == 255 && blue == 0) {
      return Color.BLACK.getRGB();
    }

    return 0xff000000 | ((red) << 16) | ((green) << 8) | blue;
  }

  private RGB getRGB(int pixel) {
    int r = pixel >> 16 & 0xff;
    int g = pixel >> 8 & 0xff;
    int b = pixel & 0xff;

    return new RGB(r, g, b);
  }

  private float min3(float a, float b, float c) {
    return Math.min(Math.min(a, b), c);
  }

  private float max3(float a, float b, float c) {
    return Math.max(Math.max(a, b), c);
  }

  private HSV RGBtoHSV(int pixel) {
    RGB rgb = getRGB(pixel);
    int r = rgb.r;
    int g = rgb.g;
    int b = rgb.b;

    float min = min3(r, g, b);
    float max = max3(r, g, b);

    float delta = max - min;
    float v = max;
    float h, s;

    if (max != 0)
      s = delta / max; // s
    else {
      // r = g = b = 0 // s = 0, v is undefined
      s = 0;
      h = 0;
      return new HSV(h, s, v);
    }
    if (r == max)
      h = (g - b) / delta; // between yellow & magenta
    else if (g == max)
      h = 2 + (b - r) / delta; // between cyan & yellow
    else
      h = 4 + (r - g) / delta; // between magenta & cyan

    h *= 60; // degrees

    if (h < 0)
      h += 360;

    HSV hsv = new HSV(h, s, v);

    return hsv;
  }

  private int getBin(int x) {
    if (x >= 346 && x <= 354) {
      // RED
      return 0;
    }

    if (x >= 355 || x <= 14) {
      // RED
      return 0;
    }

    if (x >= 15 && x <= 20) {
      return 1;
    }

    if (x >= 21 && x <= 35) {
      return 2;
    }

    if (x >= 36 && x <= 50) {
      return 3;
    }

    if (x >= 51 && x <= 60) {
      return 4;
    }

    if (x >= 61 && x <= 80) {
      return 5;
    }

    if (x >= 81 && x <= 140) {
      // GREEN
      return 6;
    }

    if (x >= 141 && x <= 169) {
      return 7;
    }

    if (x >= 170 && x <= 200) {
      return 8;
    }

    if (x >= 201 && x <= 220) {
      return 9;
    }

    if (x >= 221 && x <= 240) {
      // BLUE
      return 10;
    }

    if (x >= 241 && x <= 280) {
      return 11;
    }

    if (x >= 281 && x <= 320) {
      return 12;
    }

    if (x >= 321 && x <= 330) {
      return 13;
    }

    if (x >= 331 && x <= 345) {
      return 14;
    }

    return 0;
  }

  /**
   * Read Image RGB
   * Reads the image of given width and height at the given imgPath into the
   * provided BufferedImage.
   */
  private void readImageRGB(
      String imagePath,
      BufferedImage image,
      boolean isObject,
      int objectIndex) {
    try {
      int frameLength = originalWidth * originalHeight * 3;

      File file = new File(imagePath);
      RandomAccessFile raf = new RandomAccessFile(file, "r");
      raf.seek(0);

      byte[] bytes = new byte[frameLength];

      raf.read(bytes);

      int index = 0;
      int pixelCount = 0;
      for (int y = 0; y < originalHeight; y++) {
        for (int x = 0; x < originalWidth; x++) {
          byte r = bytes[index];
          byte g = bytes[index + originalHeight * originalWidth];
          byte b = bytes[index + originalHeight * originalWidth * 2];

          int pixel = getPixel(r, g, b, isObject);
          image.setRGB(x, y, pixel);

          if (isObject) {
            HSV hsv = RGBtoHSV(pixel);

            if (hsv.s != 0) {
              pixelCount++;
              int bin = getBin((int) hsv.h);

              objectImageHSVBins[objectIndex][bin] += 1;
            }
          }

          index++;
        }
      }

      if (isObject) {
        for (int i = 0; i < objectImageHSVBins[objectIndex].length; i++) {
          objectImageHSVBinsNormalised[objectIndex][i] = (double) objectImageHSVBins[objectIndex][i] / pixelCount;
        }
      }

      raf.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private int getSecondLargestBinFreq(int[] currObjectImageHSVBinsFreq) {
    int largest = -1;
    for (int i = 0; i < currObjectImageHSVBinsFreq.length; i++) {
      if (currObjectImageHSVBinsFreq[i] > largest) {
        largest = currObjectImageHSVBinsFreq[i];
      }
    }

    int secondLargest = -1;
    for (int i = 0; i < currObjectImageHSVBinsFreq.length; i++) {
      if (currObjectImageHSVBinsFreq[i] > secondLargest && currObjectImageHSVBinsFreq[i] < largest) {
        secondLargest = currObjectImageHSVBinsFreq[i];
      }
    }

    return secondLargest;
  }

  private boolean getObjectPixelMatch(int pixel, int[] currObjectImageHSVBinsFreq, int secondLargestFreq) {
    RGB rgb = getRGB(pixel);
    HSV hsv = RGBtoHSV(pixel);

    if (hsv.s == 0) {
      return false;
    }

    if (rgb.r == rgb.b && rgb.b == rgb.g) {
      return false;
    }

    if (Math.abs(rgb.r - rgb.g) <= 20 && Math.abs(rgb.r - rgb.b) <= 20 && Math.abs(rgb.b - rgb.g) <= 20) {
      return false;
    }

    int bin = getBin((int) hsv.h);

    return currObjectImageHSVBinsFreq[bin] >= secondLargestFreq;
  }

  private int[][] buildMatchMatix(int[] currObjectImageHSVBinsFreq) {
    int[][] matchMatrix = new int[originalHeight][originalWidth];

    int secondLargestFreq = getSecondLargestBinFreq(currObjectImageHSVBinsFreq);

    for (int row = 0; row < originalHeight; row++) {
      for (int col = 0; col < originalWidth; col++) {
        matchMatrix[row][col] = getObjectPixelMatch(
            originalImage.getRGB(col, row),
            currObjectImageHSVBinsFreq,
            secondLargestFreq) ? 1 : 0;
      }
    }

    return matchMatrix;
  }

  private Rectangle bfs(int initialRow, int initialCol, int[][] visited, int[][] matchMatrix) {
    Queue<Point> q = new LinkedList<Point>();

    visited[initialRow][initialCol] = 1;
    q.add(new Point(initialRow, initialCol));

    int minRow = originalHeight, maxRow = -1, minCol = originalWidth, maxCol = -1;

    while (!q.isEmpty()) {
      Point p = q.remove();

      int currentRow = p.x;
      int currentCol = p.y;

      minRow = Math.min(currentRow, minRow);
      minCol = Math.min(currentCol, minCol);
      maxRow = Math.max(currentRow, maxRow);
      maxCol = Math.max(currentCol, maxCol);

      Point[] neighbours = { new Point(-1, -1), new Point(-1, 0), new Point(-1, 1),
          new Point(0, -1), new Point(0, 0),
          new Point(0, 1), new Point(1, -1), new Point(1, 0), new Point(1, 1) };

      for (Point neighbour : neighbours) {
        int row = currentRow + neighbour.x;
        int col = currentCol + neighbour.y;

        if (col >= 0 && col < originalWidth && row >= 0 && row < originalHeight) {
          if (matchMatrix[row][col] == 1 && visited[row][col] == 0) {
            visited[row][col] = 1;
            q.add(new Point(row, col));
          }
        }
      }
    }

    return new Rectangle(minRow, minCol, maxRow - minRow + 1, maxCol - minCol + 1);
  }

  private List<Rectangle> getObjectBoundingBoxes(int[][] matchMatrix) {
    int[][] visited = new int[originalHeight][originalWidth];

    List<Rectangle> boundingBoxes = new ArrayList<Rectangle>();

    for (int row = 0; row < originalHeight; row++) {
      for (int col = 0; col < originalWidth; col++) {
        if (visited[row][col] == 0 && matchMatrix[row][col] == 1) {
          Rectangle boundingBox = bfs(row, col, visited, matchMatrix);

          if (boundingBox != null) {
            boundingBoxes.add(boundingBox);
          }
        }
      }
    }

    int maxClusterSize = -1;

    for (Rectangle rect : boundingBoxes) {
      int clusterSize = rect.width * rect.height;

      maxClusterSize = Math.max(clusterSize, maxClusterSize);
    }

    double minimumRequiredClusterSize = maxClusterSize - 0.5 * maxClusterSize;
    List<Rectangle> filteredBoundingBoxes = new ArrayList<Rectangle>();

    for (Rectangle rect : boundingBoxes) {
      int clusterSize = rect.width * rect.height;

      if (clusterSize > minimumRequiredClusterSize) {
        filteredBoundingBoxes.add(rect);
      }
    }

    return filteredBoundingBoxes;
  }

  private double calculateEuclideanDistance(double[] currBoundingBoxHSVBinNormalised,
      double[] currObjectImageHSVBinsNormalised) {
    double total = 0;

    for (int i = 0; i < currBoundingBoxHSVBinNormalised.length; i++) {
      total += Math.pow((currBoundingBoxHSVBinNormalised[i] - currObjectImageHSVBinsNormalised[i]), 2);
    }

    return Math.sqrt(total);
  }

  private List<Rectangle> filterBoundingBoxesByDistance(List<Rectangle> boundingBoxes,
      double[] currObjectImageHSVBinsNormalised) {
    List<Rectangle> filteredBoundingBoxes = new ArrayList<Rectangle>();

    for (Rectangle boundingBox : boundingBoxes) {
      double[] currBoundingBoxHSVBinNormalised = new double[15];

      int pixelCount = boundingBox.width * boundingBox.height;

      for (int row = 0; row < boundingBox.width; row++) {
        for (int col = 0; col < boundingBox.height; col++) {
          int currentRow = boundingBox.x + row;
          int currentCol = boundingBox.y + col;

          int pixel = originalImage.getRGB(currentCol, currentRow);

          HSV hsv = RGBtoHSV(pixel);

          int bin = getBin((int) hsv.h);

          currBoundingBoxHSVBinNormalised[bin] += 1;
        }
      }

      for (int i = 0; i < currBoundingBoxHSVBinNormalised.length; i++) {
        currBoundingBoxHSVBinNormalised[i] /= pixelCount;
      }

      double distance = calculateEuclideanDistance(currBoundingBoxHSVBinNormalised, currObjectImageHSVBinsNormalised);

      System.out.println("Object Rectangle: " + boundingBox);
      System.out.println("Eucledian Distance: " + distance);

      if (distance <= 0.52) {
        filteredBoundingBoxes.add(boundingBox);
      }
    }

    return filteredBoundingBoxes;
  }

  private List<Rectangle> obtainObjectBoundingBoxes(int objectIndex) {
    int[] currObjectImageHSVBinsFreq = objectImageHSVBins[objectIndex];

    int[][] matchMatrix = buildMatchMatix(currObjectImageHSVBinsFreq);

    List<Rectangle> boundingBoxes = getObjectBoundingBoxes(matchMatrix);

    double[] currObjectImageHSVBinsNormalised = objectImageHSVBinsNormalised[objectIndex];
    List<Rectangle> filteredBoundingBoxes = filterBoundingBoxesByDistance(boundingBoxes,
        currObjectImageHSVBinsNormalised);

    return filteredBoundingBoxes;
  }

  private String getFileName(String path) {
    String[] values = path.split("/");

    for (String value : values) {
      if (value.contains(".rgb")) {
        return value;
      }
    }

    return "";
  }

  public void showImage() {
    // Read in the specified image
    originalImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);
    readImageRGB(originalImagePath, originalImage, false, -1);

    // Read object images
    for (int i = 0; i < objectImages.length; i++) {
      objectImages[i] = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);
      readImageRGB(objectImagePaths[i], objectImages[i], true, i);
    }

    Map<String, List<Rectangle>> objectBoundingBoxes = new HashMap<String, List<Rectangle>>();

    for (int i = 0; i < objectImagePaths.length; i++) {
      String fileName = getFileName(objectImagePaths[i]);

      System.out.println("Object: " + getFileName(objectImagePaths[i]));
      System.out.println(Arrays.toString(objectImageHSVBins[i]));

      List<Rectangle> boundingBoxes = obtainObjectBoundingBoxes(i);

      System.out.println();
      objectBoundingBoxes.put(fileName, boundingBoxes);
    }

    // Use label to display the image
    frame = new JFrame();
    frame.setTitle("Img: " + getFileName(originalImagePath));

    Graphics2D g2d = (Graphics2D) originalImage.getGraphics();

    for (String s : objectBoundingBoxes.keySet()) {
      for (Rectangle boudingBox : objectBoundingBoxes.get(s)) {
        g2d.drawRect(boudingBox.y, boudingBox.x, boudingBox.height, boudingBox.width);
        g2d.drawString(s, boudingBox.y + 1, boudingBox.x + boudingBox.width - 1);
      }
    }

    ImageIcon icon = new ImageIcon(originalImage);

    label = new JLabel(icon);

    frame.add(label);

    frame.pack();
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    ImageDisplay renderImage = new ImageDisplay();

    // Validating Arguments
    boolean result = renderImage.validateArgs(args);

    if (!result) {
      // Invalid arguments exiting
      System.exit(1);
    }

    // Render image
    renderImage.showImage();
  }
}
