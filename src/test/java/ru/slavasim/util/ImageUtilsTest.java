package ru.slavasim.util;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

//@RunWith(SpringJUnit4ClassRunner.class)
public class ImageUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void imageFromDataUrl() {
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
//
//        try {
//            File file = new File(Resources.getResource("images/image1.txt").getFile());
//            String data = FileUtils.readFileToString(file, Charset.defaultCharset());
//            BufferedImage image = ImageUtils.imageFromDataUrl(data);
//            ImageIO.write(image, "png", new File(file.getParent() + "\\image1.png"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public class TestPane extends JPanel {

        private BufferedImage image;
        private BufferedImage blackWhite;

        public TestPane() {
            try {
                File file = new File(Resources.getResource("images/image1.txt").getFile());
                String data = FileUtils.readFileToString(file, Charset.defaultCharset());
                BufferedImage image = ImageUtils.imageFromDataUrl(data);
                blackWhite = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g2d = blackWhite.createGraphics();
                g2d.drawImage(image, 0, 0, this);
                g2d.dispose();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (image != null) {
                size = new Dimension(image.getWidth() * 2, image.getHeight());
            }
            return size;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {

                int x = (getWidth() - (image.getWidth() * 3)) / 2;
                int y = (getHeight() - image.getHeight()) / 2;

                g.drawImage(image, x, y, this);
                x += image.getWidth();
                g.drawImage(blackWhite, x, y, this);
            }
        }
    }

    @Test
    public void imageFromShape() {
    }

    @Test
    public void lettersFromImage() {
    }

    @Test
    public void bestMatch() {
    }
}
