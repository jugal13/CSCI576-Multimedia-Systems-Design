import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

    JFrame frame;
    JLabel label;

    // default image width and height
    int originalWidth = 7680;
    int originalHeight = 4320;

    int antiAliasing;
    int windowSize;
    int scaledWidth;
    int scaledHeight;
    int aliasingGridSize = 9;

    String imagePath;

    double scaleFactor;

    BufferedImage originalImage;
    BufferedImage scaledImage;
    BufferedImage overlayImage;
    BufferedImage combinedImage;

    boolean isControlDown = false;

    private boolean validateArgs(String[] args) {
        // Number of arguments is 4
        // 1. Image Path
        // 2. Scale Factor
        // 3. Antialiasing
        // 4. Window Size

        if (args.length != 4) {
            System.out.println("Invalid arguements, Please enter in the format 'java class image_file S A w'");
            return false;
        }

        imagePath = args[0];

        scaleFactor = Double.parseDouble(args[1]);
        // Scale shoud be between 0 and 1
        if (scaleFactor < 0 || scaleFactor > 1) {
            System.out.println("Invalid Scaling, Please enter a value between 0 and 1");
            return false;
        }

        antiAliasing = Integer.parseInt(args[2]);
        // Antialiasing should be 0 or 1
        if (!(antiAliasing != 0 || antiAliasing != 1)) {
            System.out.println("Invalid antialising value, Please enter a value that is either 0 or 1");
            return false;
        }

        windowSize = Integer.parseInt(args[3]);

        scaledWidth = (int) (originalWidth * scaleFactor);
        scaledHeight = (int) (originalHeight * scaleFactor);

        System.out.println("Image: " + imagePath);
        System.out.println("Scale: " + scaleFactor);
        System.out.println("Antialiasing: " + antiAliasing);
        System.out.println("Window Size: " + windowSize + " x " + windowSize);
        System.out.println("Original Image Width: " + originalWidth);
        System.out.println("Original Image Height: " + originalHeight);
        System.out.println("Resampled Image Width: " + scaledWidth);
        System.out.println("Resampled Image Height: " + scaledHeight);

        return true;
    }

    /**
     * Read Image RGB Reads the image of given width and height at the given
     * imgPath into the provided BufferedImage.
     */
    private void readImageRGB() {
        try {
            int frameLength = originalWidth * originalHeight * 3;

            File file = new File(imagePath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            byte[] bytes = new byte[frameLength];
            raf.read(bytes);

            int index = 0;
            for (int row = 0; row < originalHeight; row++) {
                for (int col = 0; col < originalWidth; col++) {
                    byte red = bytes[index];
                    byte green = bytes[index + originalHeight * originalWidth];
                    byte blue = bytes[index + originalHeight * originalWidth * 2];

                    int pixel = 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff);

                    originalImage.setRGB(col, row, pixel);
                    index++;
                }
            }

            raf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getOriginalPixel(int row, int col) {
        if (row < 0 || col < 0 || row >= originalHeight || col >= originalWidth) {
            return Color.BLACK.getRGB();
        }

        return originalImage.getRGB(col, row);
    }

    private int getAveragePixelValue(int row, int col) {
        int redAverage = 0;
        int greenAverage = 0;
        int blueAverage = 0;
        int size = aliasingGridSize * aliasingGridSize;

        for (int i = 0, x = -(aliasingGridSize / 2); i < aliasingGridSize; i++, x++) {
            for (int j = 0, y = -(aliasingGridSize / 2); j < aliasingGridSize; j++, y++) {
                int originalPixel = getOriginalPixel(row + x, col + y);

                int red = originalPixel >> 16 & 0xFF;
                int green = originalPixel >> 8 & 0xFF;
                int blue = originalPixel & 0xFF;

                redAverage += red;
                greenAverage += green;
                blueAverage += blue;
            }
        }

        // Averaging rgb values
        redAverage /= size;
        greenAverage /= size;
        blueAverage /= size;

        // Recreating pixel with the averaged values
        int pixel = 0xff000000 | ((redAverage & 0xff) << 16) | ((greenAverage & 0xff) << 8) | (blueAverage & 0xff);
        return pixel;
    }

    private void resampleImage() {
        // If scale is one no need to scale down and no need for antialiasing
        if (scaleFactor == 1) {
            scaledImage = originalImage;
            return;
        }

        if (antiAliasing == 1) {
            for (int row = 0; row < scaledHeight; row++) {
                for (int col = 0; col < scaledWidth; col++) {
                    int rowX = (int) (row / scaleFactor);
                    int colY = (int) (col / scaleFactor);

                    int pixel = getAveragePixelValue(rowX, colY);
                    scaledImage.setRGB(col, row, pixel);
                }
            }
            return;
        }

        for (int row = 0; row < scaledHeight; row++) {
            for (int col = 0; col < scaledWidth; col++) {
                int rowX = (int) (row / scaleFactor);
                int colY = (int) (col / scaleFactor);

                scaledImage.setRGB(col, row, originalImage.getRGB(colY, rowX));
            }
        }
    }

    private void resetImage() {
        // redraw scaled image without overlay
        Graphics g = combinedImage.getGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        label.repaint();
    }

    private void buildOverlayImage(int x, int y) {
        int originalRow = (int) (x / scaleFactor);
        int originalCol = (int) (y / scaleFactor);

        for (int row = 0, i = -(windowSize / 2); row < windowSize; row++, i++) {
            for (int col = 0, j = -(windowSize / 2); col < windowSize; col++, j++) {
                int rowX = originalRow + i;
                int colY = originalCol + j;

                int pixel = getOriginalPixel(rowX, colY);

                overlayImage.setRGB(col, row, pixel);
            }
        }
    }

    private void addFrameMouseListener() {
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (isControlDown) {
                    resetImage();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }
        });
    }

    private void addFrameMouseMotionListener() {
        frame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point mousePoint = e.getPoint();

                if (frame.contains(mousePoint) && isControlDown) {
                    int row = mousePoint.y - 29;
                    int col = mousePoint.x;

                    if (row >= 0 && col >= 0 && row <= scaledHeight && col <= scaledWidth) {
                        buildOverlayImage(row, col);

                        int frameRow = row - (windowSize / 2);
                        int frameCol = col - (windowSize / 2);

                        Graphics g = combinedImage.getGraphics();
                        g.drawImage(scaledImage, 0, 0, null);
                        g.drawImage(overlayImage, frameCol, frameRow, null);
                        g.dispose();

                        label.repaint();
                    }
                }
            }
        });
    }

    private void addFrameKeyListener() {
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown()) {
                    isControlDown = true;
                    overlayImage = new BufferedImage(windowSize, windowSize, BufferedImage.TYPE_INT_RGB);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!e.isControlDown()) {
                    isControlDown = false;
                    resetImage();
                }
            }

        });
    }

    public void showImage() {
        // Read in the specified image
        originalImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);
        readImageRGB();

        scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        resampleImage();

        // Use label to display the image
        frame = new JFrame();
        frame.setTitle(
                "Img: " + imagePath +
                        " Scale: " + scaleFactor +
                        " Antialiasing: " + antiAliasing +
                        " Window " + windowSize);

        // setting preferred size of the scaled image
        frame.setPreferredSize(new Dimension(scaledWidth, scaledHeight));

        addFrameMouseListener();
        addFrameMouseMotionListener();
        addFrameKeyListener();

        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        // Creating a combined image to draw the scaled image and the overlay together
        combinedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);

        // Drawing only the scaled image right now
        Graphics g = combinedImage.getGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();

        // Creating a label for the combined image
        label = new JLabel(new ImageIcon(combinedImage));

        // adding the label to the frame
        frame.add(label);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        ImageDisplay renderImage = new ImageDisplay();

        // Validate Arguments
        boolean result = renderImage.validateArgs(args);
        // If not valid exit
        if (!result) {
            // Invalid arguments exiting early
            System.exit(1);
        }

        renderImage.showImage();
    }
}
