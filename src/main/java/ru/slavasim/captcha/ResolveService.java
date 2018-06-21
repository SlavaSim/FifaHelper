package ru.slavasim.captcha;

import org.springframework.stereotype.Service;
import ru.slavasim.util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResolveService {
    public List<Point> resolveCaptcha(String imageDataUrl, List<Rectangle> shapes, int count) {
        try {
            BufferedImage image = ImageUtils.imageFromDataUrl(imageDataUrl);
            List<BufferedImage> letters = ImageUtils.lettersFromImage(image, count);
            List<BufferedImage> candidates = shapes.stream()
                    .map(s -> ImageUtils.imageFromShape(image, s))
                    .collect(Collectors.toList());
            List<Point> points = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Rectangle rect = shapes.get(ImageUtils.bestMatch(letters.get(i), candidates));
                points.add(new Point((int) rect.getCenterX(), (int) rect.getCenterY()));
            }
            return points;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
