package ru.slavasim.util;

import java.awt.image.BufferedImage;

public class InvImage extends BufferedImage {
    public InvImage(BufferedImage image) {
        super(image.getWidth(), image.getHeight(), image.getType());
    }
}
