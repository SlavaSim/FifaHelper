package ru.slavasim.util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BWImage extends BufferedImage {
    public BWImage(BufferedImage image) {
        super(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
    }
}
