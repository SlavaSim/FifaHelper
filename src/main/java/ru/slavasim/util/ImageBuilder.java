package ru.slavasim.util;

import ij.ImagePlus;
import ij.process.ImageConverter;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageBuilder {
    private ImagePlus image;
    private BufferedImage src;

    public ImageBuilder(BufferedImage src) {
        this.src = src;
        image = new ImagePlus("image", src);
    }

    public ImageBuilder toBlackWhite() {
        new ImageConverter(image).convertToGray8();
        image.getProcessor().threshold(128);
        return this;
    }

    public ImageBuilder invert() {
        image.getProcessor().invert();
        return this;
    }

    public BufferedImage getImage() {
        return image.getBufferedImage();
    }

    public ImageBuilder crop(Rectangle shape, int borderSize) {
        Rectangle roi = new Rectangle(shape.x + borderSize, shape.y + borderSize, shape.width - 2 * borderSize, shape.height - 2 * borderSize);
        image.setRoi(roi);
        image = image.crop();
        return this;
    }

    public ImageBuilder trim() {
        return trim(0);
    }

    public ImageBuilder trim(int borderSize) {
        int minY = 0, maxY = 0, minX = Integer.MAX_VALUE, maxX = 0;
        boolean isBlank, minYIsDefined = false;

        for (int y = 0; y < image.getHeight(); y++) {
            isBlank = true;

            for (int x = 0; x < image.getWidth(); x++) {
                //Change condition to (raster.getSample(x, y, 3) != 0)
                //for better performance
                if (image.getPixel(x, y)[0] != 255) {
                    isBlank = false;

                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }

            if (!isBlank) {
                if (!minYIsDefined) {
                    minY = y;
                    minYIsDefined = true;
                } else {
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (minX >= image.getHeight() || maxX == 0 || maxY == 0)
            return this;

        minX = Math.max(minX - borderSize, 0);
        maxX = Math.min(maxX + borderSize, image.getWidth() - 1);
        minY = Math.max(minY - borderSize, 0);
        maxY = Math.min(maxY + borderSize, image.getHeight() - 1);

        return crop(new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1), 0);
    }

    public ImageBuilder resize(int width, int height) {
        image = new ImagePlus("image", image.getProcessor().resize(width, height).getBufferedImage());
        return this;
    }
}
