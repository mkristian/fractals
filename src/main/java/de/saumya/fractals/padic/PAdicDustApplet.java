/**
 * 
 */
package de.saumya.fractals.padic;

import de.saumya.fractals.AnimationApplet;
import de.saumya.fractals.PixelProducer;

public class PAdicDustApplet extends AnimationApplet {
    private static final long serialVersionUID = 1L;

    @Override
    protected PixelProducer createPixelProducer(final int width,
            final int height) {
        return new PAdicDustProducer(800, 40, 256, 5, 6, 800 / 2);
    }

}