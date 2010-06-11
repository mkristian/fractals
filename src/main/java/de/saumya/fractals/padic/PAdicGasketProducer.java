/**
 * 
 */
package de.saumya.fractals.padic;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

import de.saumya.fractals.AnimatedGifEncoder;
import de.saumya.fractals.ColorMapFactory;
import de.saumya.fractals.PixelProducer;

class PAdicGasketProducer extends PixelProducer {
    final ColorMapFactory factory = new ColorMapFactory();
    final Color[]         colors1 = this.factory.map(true, true, true);
    final Color[]         colors2 = this.factory.mapInvers(false, true, false);
    PAdicTupleProjection  proj;
    double                min     = Double.MAX_VALUE;
    double                max     = Double.MIN_VALUE;
    PAdicTuple            coord;

    PAdicGasketProducer(final int frames, final int p, final int pp, final int n) {
        super(0, 0, frames);
        this.coord = new PAdicTuple(n, new short[] { (short) p, (short) pp });
        this.width = (int) this.coord.maxValue(0, 2);
        this.height = (int) this.coord.maxValue(1, 2);
        this.proj = new PAdicTupleProjection(new PAdicTuple(n + 0,
                new short[] { (short) (p * pp) }), (int) this.coord.maxValue());
        do {
            // final double v = this.proj.nextAlphabet();
            final double v = this.proj.nextOrder();
            this.min = v < this.min ? v : this.min;
            this.max = v > this.max ? v : this.max;
        }
        while (this.proj.hasNext());

    }

    @Override
    protected void produce(final int frame, final int[] pixels) {
        final int maxX = (int) this.coord.maxValue(0, 2);
        final int frame2 = this.frames / 2;
        final int boundery = frame2 == 0 ? -1 : 256
                * (frame > frame2 ? this.frames - frame : frame) / frame2;
        System.out.println(boundery);
        do {
            final int fx = (int) ((this.proj.nextOrder() - this.min) * 255 / (this.max - this.min));
            if (fx > boundery) {
                pixels[(int) (this.coord.xValue() + this.coord.yValue() * maxX)] = this.colors1[fx].getRGB();
            }
            else {
                pixels[(int) (this.coord.xValue() + this.coord.yValue() * maxX)] = this.colors2[fx].getRGB();
            }

        }
        while (this.coord.increment());

    }

    public static void main(final String... args) throws IOException,
            DocumentException {
        final int frames = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        final int chunks = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        final int p = 4;
        final int pp = 2;
        final int n = 7;
        final String extension = "pdf";

        final PAdicGasketProducer producer = new PAdicGasketProducer(frames,
                p,
                pp,
                n);

        if (frames == 1) {

            final int[] pixels = producer.produce(0);
            if (chunks == 0) {
                final BufferedImage bi = new BufferedImage(producer.width,
                        producer.height,
                        BufferedImage.TYPE_INT_ARGB);
                bi.setRGB(0,
                          0,
                          producer.width,
                          producer.height,
                          pixels,
                          0,
                          producer.width);

                final Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("PNG");
                final ImageWriter imageWriter = imageWriters.next();
                final File file = new File("padic-" + p + "-" + pp + "-" + n
                        + ".png");
                final ImageOutputStream ios = ImageIO.createImageOutputStream(file);
                imageWriter.setOutput(ios);
                imageWriter.write(bi);
            }
            else {
                final int h = producer.height / p / (chunks == 2 ? p : 1);
                final int w = producer.width / pp / (chunks == 2 ? pp : 1);
                int index = 0;
                Document document = null;
                Rectangle dim = null;
                final BufferedImage bi = new BufferedImage(w,
                        h,
                        BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < producer.width; y += h) {
                    for (int x = 0; x < producer.height; x += w) {
                        for (int i = 0; i < h; i++) {
                            final int row = (i + y) * producer.height;
                            for (int j = 0; j < w; j++) {
                                bi.setRGB(j, i, pixels[row + j + x]);
                            }
                        }
                        if ("pdf".equals(extension)) {
                            if (document == null) {
                                document = new Document();
                                PdfWriter.getInstance(document,
                                                      new FileOutputStream("padic-"
                                                              + p
                                                              + "-"
                                                              + pp
                                                              + "-"
                                                              + n
                                                              + "."
                                                              + extension));
                                dim = document.getPageSize();
                                document.setMargins(10, 10, 10, 0);
                                document.open();
                            }
                            final Image img = Image.getInstance(bi, null);
                            img.scaleAbsolute(dim.getWidth() - 20,
                                              dim.getWidth() - 20);
                            document.add(img);
                            document.newPage();
                        }
                        else {
                            final Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName(extension.toUpperCase());
                            final ImageWriter imageWriter = imageWriters.next();
                            final File file = new File("padic-" + p + "-" + pp
                                    + "-" + n + "-" + index + "." + extension);
                            final ImageOutputStream ios = ImageIO.createImageOutputStream(file);
                            imageWriter.setOutput(ios);
                            imageWriter.write(bi);
                        }
                        index++;
                    }
                }
                if (document != null) {
                    document.close();
                    new File("padic-" + p + "-" + pp + "-" + n + "."
                            + extension).renameTo(new File("padic-" + p + "-"
                            + pp + "-" + n + "-" + index + "." + extension));
                }
            }
        }
        else {
            final AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start("padic-" + p + "-" + pp + "-" + n + "-" + frames
                    + ".gif");
            encoder.setDelay(200);
            encoder.setRepeat(0);
            for (int i = 0; i < frames; i++) {
                final BufferedImage bi = new BufferedImage(producer.width,
                        producer.height,
                        BufferedImage.TYPE_INT_ARGB);
                final int[] pixels = producer.produce(i);
                bi.setRGB(0,
                          0,
                          producer.width,
                          producer.height,
                          pixels,
                          0,
                          producer.width);
                encoder.addFrame(bi);
            }
            encoder.finish();
        }
    }
}