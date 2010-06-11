/**
 * 
 */
package de.saumya.fractals.padic;

import java.awt.Color;

import de.saumya.fractals.ColorMapFactory;
import de.saumya.fractals.PixelProducer;

class PAdicDustProducer extends PixelProducer {

    final ColorMapFactory      factory = new ColorMapFactory();
    final Color[]              colors  = this.factory.map(true, true, true);

    final double               min;
    final double               max;
    final short                anchor;
    final PAdicTupleProjection proj;

    PAdicDustProducer(final int width, final int height, final int frames,
            final int p, final int maxIteration, final int k) {
        super(width, height, frames);
        this.proj = new PAdicTupleProjection(new PAdicTuple(maxIteration,
                new short[] { (short) p }), width);
        this.anchor = (short) k;
        this.min = 1;
        this.max = p;
    }

    void setup(final int frame) {
        final int pos = this.anchor;
        final long anchor = this.proj.tuple.maxValue() * pos / this.width;
        final long length = this.proj.tuple.maxValue() * (this.frames - frame)
                / this.frames;

        this.proj.setup(anchor - length * pos / this.width, anchor + length
                * (this.width - pos) / this.width);
    }

    @Override
    protected void produce(final int frame, final int[] pixels) {
        int index = 0;
        setup(frame);
        for (int y = 0; y < this.height; y++) {
            this.proj.reset();
            for (int x = 0; x < this.width; x++) {
                final int fx = (int) ((this.proj.nextOrder() - this.min) * 255 / (this.max - this.min));
                pixels[index] = this.colors[fx].getRGB();
                index++;
            }
        }
        System.err.println("frame " + frame);
    }
}