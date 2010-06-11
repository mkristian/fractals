/**
 * 
 */
package de.saumya.fractals;

abstract public class PixelProducer {
    protected final int frames;
    protected int       width;
    protected int       height;

    public PixelProducer(final int width, final int height, final int frames) {
        this.width = width;
        this.height = height;
        this.frames = frames;
    }

    public int[] produce(final int frame) {
        final int pixels[] = new int[this.width * this.height];
        produce(frame, pixels);
        return pixels;
    }

    protected abstract void produce(int frame, int[] pixels);

}