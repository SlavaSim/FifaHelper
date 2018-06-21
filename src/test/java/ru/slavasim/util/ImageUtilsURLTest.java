package ru.slavasim.util;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ImageUtilsURLTest {
    public static void main(String[] args) {
        new ImageUtilsURLTest();
    }

    public ImageUtilsURLTest() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                }

                JFrame frame = new JFrame("Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new TestPane());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            }
        });
    }

    public class TestPane extends JPanel {

        private BufferedImage image;
        private BufferedImage blackWhite;
        private List<BufferedImage> candidates = new ArrayList<>();
        private List<BufferedImage> letters = new ArrayList<>();
        private List<BufferedImage> answer = new ArrayList<>();

        public TestPane() {
            try {
                File file = new File(Resources.getResource("images/image1.txt").getFile());
                String data = FileUtils.readFileToString(file, Charset.defaultCharset());
                image = ImageUtils.imageFromDataUrl(data);
                blackWhite = new ImageBuilder(image)
                        .toBlackWhite()
                        .getImage();
//                blackWhite = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

                File shapesFile = new File(Resources.getResource("images/shapes.txt").getFile());
                List<String> lines = FileUtils.readLines(shapesFile, Charset.defaultCharset());
                for (String line : lines) {
                    String[] coords = line.split(",");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int w = Integer.parseInt(coords[2]) - x;
                    int h = Integer.parseInt(coords[3]) - y;
                    BufferedImage shape = ImageUtils.imageFromShape(blackWhite, new Rectangle(x, y, w, h));
//                    BufferedImage shape2 = new ImageBuilder(shape).invert().getImage();
                    candidates.add(shape);
                }

                letters = ImageUtils.lettersFromImage(blackWhite, 4);
                for (int i = 0; i < letters.size(); i++) {
                    answer.add(candidates.get(ImageUtils.bestMatch(letters.get(i), candidates)));
                }


/*
                Graphics2D g2d = blackWhite.createGraphics();
                g2d.drawImage(image, 0, 0, this);
                g2d.dispose();
*/
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (image != null) {
                size = new Dimension(image.getWidth() * 3, image.getHeight() * 2);
            }
            return size;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {

                int x = (getWidth() - (image.getWidth() * 2)) / 2;
                int y = (getHeight() - image.getHeight()) / 2;

                g.drawImage(image, x, y, this);
                x += image.getWidth();
                g.drawImage(blackWhite, x, y, this);
                x = 0;
                y = 2;
                for (BufferedImage candidate : candidates) {
                    g.drawImage(candidate, x, y, this);
                    x += candidate.getWidth() + 2;
                }
                x = 0;
                y = 40;
                for (BufferedImage letter : letters) {
                    g.drawImage(letter, x, y, this);
                    x += letter.getWidth() + 2;
                }
                x = 0;
                y = 80;
                for (BufferedImage a : answer) {
                    g.drawImage(a, x, y, this);
                    x += a.getWidth() + 2;
                }
            }
        }


    }
}