/**
 * 
 */
package de.saumya.fractals.padic;

import de.saumya.fractals.AnimationApplet;
import de.saumya.fractals.PixelProducer;

public class PAdicGasketApplet extends AnimationApplet {
    private static final long serialVersionUID = 1L;

    @Override
    protected PixelProducer createPixelProducer(final int width,
            final int height) {
        return new PAdicGasketProducer(32, 3, 3, 7);
    }

    // @Override
    // void fillPixels(final int[] pixels, final ColorMapFactory factory) {
    // final Color[] colors = factory.map(true, true, true);
    // final Color[] redColors = factory.map(true, false, false);
    // final int maxX = (int) this.coord.maxValue(0, 2);
    // do {
    // final int fx = f(0, 0);
    // if (fx > 224) {
    // pixels[(int) (this.coord.xValue() + this.coord.yValue() * maxX)] = // new
    // // Color(fx).getRGB();
    // colors[fx].getRGB();
    // // : Color.RED.getRGB();
    // }
    // else {
    // pixels[(int) (this.coord.xValue() + this.coord.yValue() * maxX)] =
    // redColors[fx].getRGB();
    // }
    //
    // }
    // while (this.coord.increment());
    // }
}