import java.awt.Graphics2D;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

class RGB {
	float r, g, b;

	RGB(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	RGB avg(RGB rgb) {
		float r = (this.r + rgb.r) / 2;
		float g = (this.g + rgb.g) / 2;
		float b = (this.b + rgb.b) / 2;

		return new RGB(r, g, b);
	}

	RGB diff(RGB rgb) {
		float r = (this.r - rgb.r) / 2;
		float g = (this.g - rgb.g) / 2;
		float b = (this.b - rgb.b) / 2;

		return new RGB(r, g, b);
	}

	RGB add(RGB rgb) {
		float r = (this.r + rgb.r);
		float g = (this.g + rgb.g);
		float b = (this.b + rgb.b);

		return new RGB(r, g, b);
	}

	RGB sub(RGB rgb) {
		float r = (this.r - rgb.r);
		float g = (this.g - rgb.g);
		float b = (this.b - rgb.b);

		return new RGB(r, g, b);
	}

	@Override
	public String toString() {
		return "RGB: " + this.r + " " + this.g + " " + this.b;
	}
}

public class ImageDisplay {
	JFrame frame;

	JLabel label;
	JLabel overlayLabel;

	BufferedImage originalImage;

	String imgPath;

	int reductionFactor;

	int originalWidth = 512; // default image width and height
	int originalHeight = 512;

	RGB[][] originalMatrix = new RGB[originalWidth][originalHeight];

	private void resetOriginalMatrix() {
		for (int y = 0; y < originalHeight; y++) {
			for (int x = 0; x < originalWidth; x++) {
				int pixel = originalImage.getRGB(x, y);

				originalMatrix[x][y] = getRGB(pixel);
			}
		}
	}

	private int getLevels(int reductionFactor) {
		int factor = originalWidth / (int) Math.pow(2, reductionFactor);
		int lvl = (int) (Math.log(factor) / Math.log(2));

		return lvl + 1;
	}

	private RGB getRGB(int pixel) {
		int r = pixel >> 16 & 0xff;
		int g = pixel >> 8 & 0xff;
		int b = pixel & 0xff;

		RGB rgb = new RGB(r, g, b);

		return rgb;
	}

	private int getPixel(RGB rgb) {
		int r = (int) rgb.r;
		int g = (int) rgb.g;
		int b = (int) rgb.b;

		int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

		return pix;
	}

	private RGB[][] copyMatrix(RGB[][] mat) {
		RGB[][] finalMatrix = new RGB[originalWidth][originalHeight];

		for (int y = 0; y < originalHeight; y++) {
			for (int x = 0; x < originalWidth; x++) {
				RGB rgb = mat[x][y];
				finalMatrix[x][y] = new RGB(rgb.r, rgb.g, rgb.b);
			}
		}

		return finalMatrix;
	}

	private void reduceImageMatrix(int factor) {
		int scale = factor / 2;
		int width = originalWidth / scale;
		int height = originalHeight / scale;

		RGB[][] rowUpdatedMatrix = copyMatrix(originalMatrix);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x += 2) {
				RGB left_rgb = originalMatrix[x][y];
				RGB right_rgb = originalMatrix[x + 1][y];

				RGB avg_rgb = left_rgb.avg(right_rgb);
				RGB diff_rgb = left_rgb.diff(right_rgb);

				rowUpdatedMatrix[x / 2][y] = avg_rgb;
				rowUpdatedMatrix[width / 2 + x / 2][y] = diff_rgb;
			}
		}

		RGB[][] colUpdatedMatrix = copyMatrix(rowUpdatedMatrix);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y += 2) {
				RGB top_rgb = rowUpdatedMatrix[x][y];
				RGB bottom_rgb = rowUpdatedMatrix[x][y + 1];

				RGB avg_rgb = top_rgb.avg(bottom_rgb);
				RGB diff_rgb = top_rgb.diff(bottom_rgb);

				colUpdatedMatrix[x][y / 2] = avg_rgb;
				colUpdatedMatrix[x][height / 2 + y / 2] = diff_rgb;
			}
		}

		originalMatrix = copyMatrix(colUpdatedMatrix);
	}

	private void zeroImageMatrix(int factor) {
		int initial_y = originalHeight / factor;
		int initial_x = originalWidth / factor;

		for (int y = 0; y < originalHeight; y++) {
			for (int x = initial_x; x < originalWidth; x++) {
				originalMatrix[x][y] = new RGB(0, 0, 0);
			}
		}

		for (int y = initial_y; y < originalHeight; y++) {
			for (int x = 0; x < originalWidth; x++) {
				originalMatrix[x][y] = new RGB(0, 0, 0);
			}
		}
	}

	private void reconstructMatrix(int factor) {
		int height = originalHeight / factor;
		int width = originalWidth / factor;

		RGB[][] colUpdatedMatrix = copyMatrix(originalMatrix);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				RGB top_rgb = originalMatrix[x][y].add(originalMatrix[x][y + height]);
				RGB bottom_rgb = originalMatrix[x][y].sub(originalMatrix[x][y + height]);

				colUpdatedMatrix[x][y * 2] = top_rgb;
				colUpdatedMatrix[x][y * 2 + 1] = bottom_rgb;
			}
		}

		RGB[][] rowUpdatedMatrix = copyMatrix(colUpdatedMatrix);

		for (int y = 0; y < height * factor; y++) {
			for (int x = 0; x < width; x++) {
				RGB left_rgb = colUpdatedMatrix[x][y].add(colUpdatedMatrix[x + width][y]);
				RGB right_rgb = colUpdatedMatrix[x][y].sub(colUpdatedMatrix[x + width][y]);

				rowUpdatedMatrix[x * 2][y] = left_rgb;
				rowUpdatedMatrix[x * 2 + 1][y] = right_rgb;
			}
		}

		originalMatrix = copyMatrix(rowUpdatedMatrix);
	}

	/**
	 * Read Image RGB
	 * Reads the image of given width and height at the given imgPath into the
	 * provided BufferedImage.
	 */
	private void readImageRGB(BufferedImage img) {
		try {
			int frameLength = originalWidth * originalHeight * 3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for (int y = 0; y < originalHeight; y++) {
				for (int x = 0; x < originalWidth; x++) {
					byte r = bytes[ind];
					byte g = bytes[ind + originalHeight * originalWidth];
					byte b = bytes[ind + originalHeight * originalWidth * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					// int pix = ((0xff << 24) + (r << 16) + (g << 8) + b);

					img.setRGB(x, y, pix);

					ind++;
				}
			}

			raf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BufferedImage buildImage(int factor) {
		int scaledLevels = getLevels(factor);

		for (int i = 1; i < scaledLevels; i++) {
			int currentLevelFactor = (int) Math.pow(2, i);

			reduceImageMatrix(currentLevelFactor);
		}

		int level = scaledLevels - 1;
		int zeroFactor = (int) Math.pow(2, level);

		zeroImageMatrix(zeroFactor);

		for (int i = scaledLevels - 1; i > 0; i--) {
			int currentLevelFactor = (int) Math.pow(2, i);

			reconstructMatrix(currentLevelFactor);
		}

		BufferedImage compressedImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < originalWidth; y++) {
			for (int x = 0; x < originalHeight; x++) {
				int pixel = getPixel(originalMatrix[x][y]);

				compressedImage.setRGB(x, y, pixel);
			}
		}

		return compressedImage;
	}

	public void showImg() {
		// Creating two images the original buffer and scaled buffer
		originalImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);

		// reading the original image
		readImageRGB(originalImage);
		resetOriginalMatrix();

		// creating a new JFrame to display image
		frame = new JFrame();

		// Setting a title
		frame.setTitle("Img: " + imgPath);

		BufferedImage displayImage = new BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = (Graphics2D) displayImage.getGraphics();

		label = new JLabel(new ImageIcon(displayImage));

		// adding the label to the frame
		frame.add(label);
		frame.pack();
		frame.setVisible(true);

		if (reductionFactor != -1) {
			BufferedImage result = buildImage(reductionFactor);
			g2d.drawImage(result, 0, 0, null);
		} else {
			for (int i = 0; i < 10; i++) {
				try {
					Thread.sleep(250);
				} catch (Exception e) {

				}
				resetOriginalMatrix();
				BufferedImage newItem = buildImage(i);
				g2d.drawImage(newItem, 0, 0, null);
				label.repaint();
			}
		}

	}

	public boolean validateArgs(String[] args) {
		// Number of arguments is 2
		// 1. Image Path
		// 2. Reconstruction factor

		if (args.length != 2) {
			System.out.println("Invalid arguements, Please enter in the format 'java class image_file number'");
			return false;
		}

		imgPath = args[0];

		reductionFactor = Integer.parseInt(args[1]);

		System.out.println("Image: " + imgPath);
		System.out.println("Reconstruction: " + reductionFactor);
		System.out.println("Original Image Width: " + originalWidth);
		System.out.println("Original Image Height: " + originalHeight);

		return true;
	}

	public static void main(String[] args) {
		ImageDisplay renderImage = new ImageDisplay();

		// Validating Arguments
		boolean result = renderImage.validateArgs(args);

		if (!result) {
			// Invalid arguements exiting
			System.exit(1);
		}

		// Render image
		renderImage.showImg();
	}
}
