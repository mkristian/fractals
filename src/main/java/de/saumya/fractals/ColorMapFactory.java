/**
 * 
 */
package de.saumya.fractals;

import java.awt.Color;

public class ColorMapFactory {

    public Color[] map(final boolean red, final boolean green,
            final boolean blue) {
        final Color colorArray[] = new Color[256];

        for (int i = 0; i < colorArray.length; i++) {
            colorArray[i] = new Color(red ? i : 0, green ? i : 0, blue ? i : 0);
        }
        return colorArray;
    }

    public Color[] mapInvers(final boolean red, final boolean green,
            final boolean blue) {
        final Color colorArray[] = new Color[256];

        for (int i = 0; i < colorArray.length; i++) {
            colorArray[i] = new Color(red ? colorArray.length - i - 1 : 0,
                    green ? colorArray.length - i - 1 : 0,
                    blue ? colorArray.length - i - 1 : 0);
        }
        return colorArray;
    }
}