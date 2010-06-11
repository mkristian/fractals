/**
 * 
 */
package de.saumya.fractals;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;

public class AnimationApplet extends Applet {

    private static final long serialVersionUID = 1L;

    volatile Image[]          img;
    int                       index            = 0;
    int                       maxImg;

    protected PixelProducer createPixelProducer(final int width,
            final int height) {
        return new PixelProducer(width, height, 25) {

            @Override
            protected void produce(final int frame, final int[] pixels) {
                final Color[] colors = new ColorMapFactory().map(true,
                                                                 true,
                                                                 true);
                int index = 0;
                for (int y = 0; y < this.height; y++) {
                    for (int x = 0; x < this.width; x++) {
                        final int fx = (x * 256 / this.width + frame * 10) % 256;
                        if (fx < 0) {
                            pixels[index] = Color.RED.getRGB();
                            System.err.println(x + " " + y);
                        }
                        else {
                            pixels[index] = colors[fx].getRGB();
                        }

                        index++;
                    }
                }
                System.err.println("frame " + frame);
            }
        };
    }

    @Override
    public void init() {
        final PixelProducer pixels = createPixelProducer(this.getSize().width,
                                                         this.getSize().height);

        this.setVisible(false);
        resize(pixels.width, pixels.height);
        this.setVisible(true);
        this.setBackground(Color.BLUE);

        this.img = new Image[pixels.frames];
        this.maxImg = this.img.length - 1;

        new ProducerThread(this, pixels).start();

        final AnimationThread at = new AnimationThread(200);
        at.start();
    }

    @Override
    public void paint(final Graphics g) {
        if (this.img[this.index] != null) {
            g.drawImage(this.img[this.index], 0, 0, this);
            this.index = (this.index < this.maxImg) ? this.index + 1 : 0;
        }
        else if (this.img[0] != null) {
            g.drawImage(this.img[0], 0, 0, this);
            this.index = 0;
        }
    }

    class ProducerThread extends Thread {
        AnimationApplet animationApplet;
        PixelProducer   pixels;

        public ProducerThread(final AnimationApplet a,
                final PixelProducer producer) {
            this.animationApplet = a;
            this.pixels = producer;
        }

        @Override
        public void run() {
            for (int i = 0; i < this.pixels.frames; i++) {
                final MemoryImageSource mis = new MemoryImageSource(this.pixels.width,
                        this.pixels.height,
                        this.pixels.produce(i),
                        0,
                        this.pixels.width);
                mis.setAnimated(false);
                mis.setFullBufferUpdates(false);

                this.animationApplet.img[i] = this.animationApplet.createImage(mis);
            }
        }

    }

    class AnimationThread extends Thread {
        int delay;

        public AnimationThread(final int delay) {
            this.delay = delay;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    sleep(this.delay);
                    repaint();
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}