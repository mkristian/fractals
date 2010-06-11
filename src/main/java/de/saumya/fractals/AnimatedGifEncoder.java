package de.saumya.fractals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

//from http://www.java2s.com/Code/Java/2D-Graphics-GUI/AnimatedGifEncoder.htm

/**
 * Class AnimatedGifEncoder - Encodes a GIF file consisting of one or more
 * frames.
 * 
 * <pre>
 *  Example:
 *     AnimatedGifEncoder e = new AnimatedGifEncoder();
 *     e.start(outputFileName);
 *     e.setDelay(1000);   // 1 frame per sec
 *     e.addFrame(image1);
 *     e.addFrame(image2);
 *     e.finish();
 * </pre>
 * 
 * No copyright asserted on the source code of this class. May be used for any
 * purpose, however, refer to the Unisys LZW patent for restrictions on use of
 * the associated LZWEncoder class. Please forward any corrections to
 * kweiner@fmsware.com.
 * 
 * @author Kevin Weiner, FM Software
 * @version 1.03 November 2003
 * 
 */
public class AnimatedGifEncoder {

    protected int           width;                         // image size

    protected int           height;

    protected Color         transparent = null;            // transparent color
    // if given

    protected int           transIndex;                    // transparent index
    // in color table

    protected int           repeat      = -1;              // no repeat

    protected int           delay       = 0;               // frame delay
    // (hundredths)

    protected boolean       started     = false;           // ready to output
    // frames

    protected OutputStream  out;

    protected BufferedImage image;                         // current frame

    protected byte[]        pixels;                        // BGR byte array
    // from frame

    protected byte[]        indexedPixels;                 // converted frame
    // indexed to
    // palette

    protected int           colorDepth;                    // number of bit
    // planes

    protected byte[]        colorTab;                      // RGB palette

    protected boolean[]     usedEntry   = new boolean[256]; // active palette
    // entries

    protected int           palSize     = 7;               // color table size
    // (bits-1)

    protected int           dispose     = -1;              // disposal code (-1
    // = use default)

    protected boolean       closeStream = false;           // close stream when
    // finished

    protected boolean       firstFrame  = true;

    protected boolean       sizeSet     = false;           // if false, get
    // size from first
    // frame

    protected int           sample      = 10;              // default sample

    // interval for
    // quantizer

    /**
     * Sets the delay time between each frame, or changes it for subsequent
     * frames (applies to last frame added).
     * 
     * @param ms
     *            int delay time in milliseconds
     */
    public void setDelay(final int ms) {
        this.delay = Math.round(ms / 10.0f);
    }

    /**
     * Sets the GIF frame disposal code for the last added frame and any
     * subsequent frames. Default is 0 if no transparent color has been set,
     * otherwise 2.
     * 
     * @param code
     *            int disposal code.
     */
    public void setDispose(final int code) {
        if (code >= 0) {
            this.dispose = code;
        }
    }

    /**
     * Sets the number of times the set of GIF frames should be played. Default
     * is 1; 0 means play indefinitely. Must be invoked before the first image
     * is added.
     * 
     * @param iter
     *            int number of iterations.
     * @return
     */
    public void setRepeat(final int iter) {
        if (iter >= 0) {
            this.repeat = iter;
        }
    }

    /**
     * Sets the transparent color for the last added frame and any subsequent
     * frames. Since all colors are subject to modification in the quantization
     * process, the color in the final palette for each frame closest to the
     * given color becomes the transparent color for that frame. May be set to
     * null to indicate no transparent color.
     * 
     * @param c
     *            Color to be treated as transparent on display.
     */
    public void setTransparent(final Color c) {
        this.transparent = c;
    }

    /**
     * Adds next GIF frame. The frame is not written immediately, but is
     * actually deferred until the next frame is received so that timing data
     * can be inserted. Invoking <code>finish()</code> flushes all frames. If
     * <code>setSize</code> was not invoked, the size of the first image is used
     * for all subsequent frames.
     * 
     * @param im
     *            BufferedImage containing frame to write.
     * @return true if successful.
     */
    public boolean addFrame(final BufferedImage im) {
        if ((im == null) || !this.started) {
            return false;
        }
        boolean ok = true;
        try {
            if (!this.sizeSet) {
                // use first frame's size
                setSize(im.getWidth(), im.getHeight());
            }
            this.image = im;
            getImagePixels(); // convert to correct format if necessary
            analyzePixels(); // build color table & map pixels
            if (this.firstFrame) {
                writeLSD(); // logical screen descriptior
                writePalette(); // global color table
                if (this.repeat >= 0) {
                    // use NS app extension to indicate reps
                    writeNetscapeExt();
                }
            }
            writeGraphicCtrlExt(); // write graphic control extension
            writeImageDesc(); // image descriptor
            if (!this.firstFrame) {
                writePalette(); // local color table
            }
            writePixels(); // encode and write pixel data
            this.firstFrame = false;
        }
        catch (final IOException e) {
            ok = false;
        }

        return ok;
    }

    /**
     * Flushes any pending data and closes output file. If writing to an
     * OutputStream, the stream is not closed.
     */
    public boolean finish() {
        if (!this.started) {
            return false;
        }
        boolean ok = true;
        this.started = false;
        try {
            this.out.write(0x3b); // gif trailer
            this.out.flush();
            if (this.closeStream) {

                this.out.close();
            }
        }
        catch (final IOException e) {
            ok = false;
        }

        // reset for subsequent use
        this.transIndex = 0;
        this.out = null;
        this.image = null;
        this.pixels = null;
        this.indexedPixels = null;
        this.colorTab = null;
        this.closeStream = false;
        this.firstFrame = true;

        return ok;
    }

    /**
     * Sets frame rate in frames per second. Equivalent to
     * <code>setDelay(1000/fps)</code>.
     * 
     * @param fps
     *            float frame rate (frames per second)
     */
    public void setFrameRate(final float fps) {
        if (fps != 0f) {
            this.delay = Math.round(100f / fps);
        }
    }

    /**
     * Sets quality of color quantization (conversion of images to the maximum
     * 256 colors allowed by the GIF specification). Lower values (minimum = 1)
     * produce better colors, but slow processing significantly. 10 is the
     * default, and produces good color mapping at reasonable speeds. Values
     * greater than 20 do not yield significant improvements in speed.
     * 
     * @param quality
     *            int greater than 0.
     * @return
     */
    public void setQuality(int quality) {
        if (quality < 1) {
            quality = 1;
        }
        this.sample = quality;
    }

    /**
     * Sets the GIF frame size. The default size is the size of the first frame
     * added if this method is not invoked.
     * 
     * @param w
     *            int frame width.
     * @param h
     *            int frame width.
     */
    public void setSize(final int w, final int h) {
        if (this.started && !this.firstFrame) {
            return;
        }
        this.width = w;
        this.height = h;
        if (this.width < 1) {
            this.width = 320;
        }
        if (this.height < 1) {
            this.height = 240;
        }
        this.sizeSet = true;
    }

    /**
     * Initiates GIF file creation on the given stream. The stream is not closed
     * automatically.
     * 
     * @param os
     *            OutputStream on which GIF images are written.
     * @return false if initial write failed.
     */
    public boolean start(final OutputStream os) {
        if (os == null) {
            return false;
        }
        boolean ok = true;
        this.closeStream = false;
        this.out = os;
        try {
            writeString("GIF89a"); // header
        }
        catch (final IOException e) {
            ok = false;
        }
        return this.started = ok;
    }

    /**
     * Initiates writing of a GIF file with the specified name.
     * 
     * @param file
     *            String containing output file name.
     * @return false if open or initial write failed.
     */
    public boolean start(final String file) {
        boolean ok = true;
        try {
            this.out = new BufferedOutputStream(new FileOutputStream(file));
            ok = start(this.out);
            this.closeStream = true;
        }
        catch (final IOException e) {
            ok = false;
        }
        return this.started = ok;
    }

    /**
     * Analyzes image colors and creates color map.
     */
    protected void analyzePixels() {
        final int len = this.pixels.length;
        final int nPix = len / 3;
        this.indexedPixels = new byte[nPix];
        final NeuQuant nq = new NeuQuant(this.pixels, len, this.sample);
        // initialize quantizer
        this.colorTab = nq.process(); // create reduced palette
        // convert map from BGR to RGB
        for (int i = 0; i < this.colorTab.length; i += 3) {
            final byte temp = this.colorTab[i];
            this.colorTab[i] = this.colorTab[i + 2];
            this.colorTab[i + 2] = temp;
            this.usedEntry[i / 3] = false;
        }
        // map image pixels to new palette
        int k = 0;
        for (int i = 0; i < nPix; i++) {
            final int index = nq.map(this.pixels[k++] & 0xff,
                                     this.pixels[k++] & 0xff,
                                     this.pixels[k++] & 0xff);
            this.usedEntry[index] = true;
            this.indexedPixels[i] = (byte) index;
        }
        this.pixels = null;
        this.colorDepth = 8;
        this.palSize = 7;
        // get closest match to transparent color if specified
        if (this.transparent != null) {
            this.transIndex = findClosest(this.transparent);
        }
    }

    /**
     * Returns index of palette color closest to c
     * 
     */
    protected int findClosest(final Color c) {
        if (this.colorTab == null) {
            return -1;
        }
        final int r = c.getRed();
        final int g = c.getGreen();
        final int b = c.getBlue();
        int minpos = 0;
        int dmin = 256 * 256 * 256;
        final int len = this.colorTab.length;
        for (int i = 0; i < len;) {
            final int dr = r - (this.colorTab[i++] & 0xff);
            final int dg = g - (this.colorTab[i++] & 0xff);
            final int db = b - (this.colorTab[i] & 0xff);
            final int d = dr * dr + dg * dg + db * db;
            final int index = i / 3;
            if (this.usedEntry[index] && (d < dmin)) {
                dmin = d;
                minpos = index;
            }
            i++;
        }
        return minpos;
    }

    /**
     * Extracts image pixels into byte array "pixels"
     */
    protected void getImagePixels() {
        final int w = this.image.getWidth();
        final int h = this.image.getHeight();
        final int type = this.image.getType();
        if ((w != this.width) || (h != this.height)
                || (type != BufferedImage.TYPE_3BYTE_BGR)) {
            // create new image with right size/format
            final BufferedImage temp = new BufferedImage(this.width,
                    this.height,
                    BufferedImage.TYPE_3BYTE_BGR);
            final Graphics2D g = temp.createGraphics();
            g.drawImage(this.image, 0, 0, null);
            this.image = temp;
        }
        this.pixels = ((DataBufferByte) this.image.getRaster().getDataBuffer()).getData();
    }

    /**
     * Writes Graphic Control Extension
     */
    protected void writeGraphicCtrlExt() throws IOException {
        this.out.write(0x21); // extension introducer
        this.out.write(0xf9); // GCE label
        this.out.write(4); // data block size
        int transp, disp;
        if (this.transparent == null) {
            transp = 0;
            disp = 0; // dispose = no action
        }
        else {
            transp = 1;
            disp = 2; // force clear if using transparent color
        }
        if (this.dispose >= 0) {
            disp = this.dispose & 7; // user override
        }
        disp <<= 2;

        // packed fields
        this.out.write(0 | // 1:3 reserved
                disp | // 4:6 disposal
                0 | // 7 user input - 0 = none
                transp); // 8 transparency flag

        writeShort(this.delay); // delay x 1/100 sec
        this.out.write(this.transIndex); // transparent color index
        this.out.write(0); // block terminator
    }

    /**
     * Writes Image Descriptor
     */
    protected void writeImageDesc() throws IOException {
        this.out.write(0x2c); // image separator
        writeShort(0); // image position x,y = 0,0
        writeShort(0);
        writeShort(this.width); // image size
        writeShort(this.height);
        // packed fields
        if (this.firstFrame) {
            // no LCT - GCT is used for first (or only) frame
            this.out.write(0);
        }
        else {
            // specify normal LCT
            this.out.write(0x80 | // 1 local color table 1=yes
                    0 | // 2 interlace - 0=no
                    0 | // 3 sorted - 0=no
                    0 | // 4-5 reserved
                    this.palSize); // 6-8 size of color table
        }
    }

    /**
     * Writes Logical Screen Descriptor
     */
    protected void writeLSD() throws IOException {
        // logical screen size
        writeShort(this.width);
        writeShort(this.height);
        // packed fields
        this.out.write((0x80 | // 1 : global color table flag = 1 (gct used)
        0x70 | // 2-4 : color resolution = 7
        0x00 | // 5 : gct sort flag = 0
        this.palSize)); // 6-8 : gct size

        this.out.write(0); // background color index
        this.out.write(0); // pixel aspect ratio - assume 1:1
    }

    /**
     * Writes Netscape application extension to define repeat count.
     */
    protected void writeNetscapeExt() throws IOException {
        this.out.write(0x21); // extension introducer
        this.out.write(0xff); // app extension label
        this.out.write(11); // block size
        writeString("NETSCAPE" + "2.0"); // app id + auth code
        this.out.write(3); // sub-block size
        this.out.write(1); // loop sub-block id
        writeShort(this.repeat); // loop count (extra iterations, 0=repeat
        // forever)
        this.out.write(0); // block terminator
    }

    /**
     * Writes color table
     */
    protected void writePalette() throws IOException {
        this.out.write(this.colorTab, 0, this.colorTab.length);
        final int n = (3 * 256) - this.colorTab.length;
        for (int i = 0; i < n; i++) {
            this.out.write(0);
        }
    }

    /**
     * Encodes and writes pixel data
     */
    protected void writePixels() throws IOException {
        final LZWEncoder encoder = new LZWEncoder(this.width,
                this.height,
                this.indexedPixels,
                this.colorDepth);
        encoder.encode(this.out);
    }

    /**
     * Write 16-bit value to output stream, LSB first
     */
    protected void writeShort(final int value) throws IOException {
        this.out.write(value & 0xff);
        this.out.write((value >> 8) & 0xff);
    }

    /**
     * Writes string to output stream
     */
    protected void writeString(final String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            this.out.write((byte) s.charAt(i));
        }
    }

    /*
     * NeuQuant Neural-Net Quantization Algorithm
     * ------------------------------------------
     * 
     * Copyright (c) 1994 Anthony Dekker
     * 
     * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994. See
     * "Kohonen neural networks for optimal colour quantization" in "Network:
     * Computation in Neural Systems" Vol. 5 (1994) pp 351-367. for a discussion
     * of the algorithm.
     * 
     * Any party obtaining a copy of these files from the author, directly or
     * indirectly, is granted, free of charge, a full and unrestricted
     * irrevocable, world-wide, paid up, royalty-free, nonexclusive right and
     * license to deal in this software and documentation files (the
     * "Software"), including without limitation the rights to use, copy,
     * modify, merge, publish, distribute, sublicense, and/or sell copies of the
     * Software, and to permit persons who receive copies from any such party to
     * do so, with the only requirement being that this copyright notice remain
     * intact.
     */

    // Ported to Java 12/00 K Weiner
    static class NeuQuant {

        protected static final int netsize         = 256;                                  /*
                                                                                             * number
                                                                                             * of
                                                                                             * colours
                                                                                             * used
                                                                                             */

        /* four primes near 500 - assume no image has a length so large */
        /* that it is divisible by all four primes */
        protected static final int prime1          = 499;

        protected static final int prime2          = 491;

        protected static final int prime3          = 487;

        protected static final int prime4          = 503;

        protected static final int minpicturebytes = (3 * prime4);

        /* minimum size for input image */

        /*
         * Program Skeleton ---------------- [select samplefac in range 1..30]
         * [read image from input file] pic = (unsigned char*)
         * malloc(3*width*height); initnet(pic,3*width*height,samplefac);
         * learn(); unbiasnet(); [write output image header, using
         * writecolourmap(f)] inxbuild(); write output image using
         * inxsearch(b,g,r)
         */

        /*
         * Network Definitions -------------------
         */

        protected static final int maxnetpos       = (netsize - 1);

        protected static final int netbiasshift    = 4;                                    /*
                                                                                             * bias
                                                                                             * for
                                                                                             * colour
                                                                                             * values
                                                                                             */

        protected static final int ncycles         = 100;                                  /*
                                                                                             * no.
                                                                                             * of
                                                                                             * learning
                                                                                             * cycles
                                                                                             */

        /* defs for freq and bias */
        protected static final int intbiasshift    = 16;                                   /*
                                                                                             * bias
                                                                                             * for
                                                                                             * fractions
                                                                                             */

        protected static final int intbias         = ((1) << intbiasshift);

        protected static final int gammashift      = 10;                                   /*
                                                                                             * gamma
                                                                                             * =
                                                                                             * 1024
                                                                                             */

        protected static final int gamma           = ((1) << gammashift);

        protected static final int betashift       = 10;

        protected static final int beta            = (intbias >> betashift);               /*
                                                                                             * beta
                                                                                             * =
                                                                                             * 1
                                                                                             * /
                                                                                             * 1024
                                                                                             */

        protected static final int betagamma       = (intbias << (gammashift - betashift));

        /* defs for decreasing radius factor */
        protected static final int initrad         = (netsize >> 3);                       /*
                                                                                             * for
                                                                                             * 256
                                                                                             * cols
                                                                                             * ,
                                                                                             * radius
                                                                                             * starts
                                                                                             */

        protected static final int radiusbiasshift = 6;                                    /*
                                                                                             * at
                                                                                             * 32.0
                                                                                             * biased
                                                                                             * by
                                                                                             * 6
                                                                                             * bits
                                                                                             */

        protected static final int radiusbias      = ((1) << radiusbiasshift);

        protected static final int initradius      = (initrad * radiusbias);               /*
                                                                                             * and
                                                                                             * decreases
                                                                                             * by
                                                                                             * a
                                                                                             */

        protected static final int radiusdec       = 30;                                   /*
                                                                                             * factor
                                                                                             * of
                                                                                             * 1
                                                                                             * /
                                                                                             * 30
                                                                                             * each
                                                                                             * cycle
                                                                                             */

        /* defs for decreasing alpha factor */
        protected static final int alphabiasshift  = 10;                                   /*
                                                                                             * alpha
                                                                                             * starts
                                                                                             * at
                                                                                             * 1.0
                                                                                             */

        protected static final int initalpha       = ((1) << alphabiasshift);

        protected int              alphadec;                                               /*
                                                                                             * biased
                                                                                             * by
                                                                                             * 10
                                                                                             * bits
                                                                                             */

        /* radbias and alpharadbias used for radpower calculation */
        protected static final int radbiasshift    = 8;

        protected static final int radbias         = ((1) << radbiasshift);

        protected static final int alpharadbshift  = (alphabiasshift + radbiasshift);

        protected static final int alpharadbias    = ((1) << alpharadbshift);

        /*
         * Types and Global Variables --------------------------
         */

        protected byte[]           thepicture;                                             /*
                                                                                             * the
                                                                                             * input
                                                                                             * image
                                                                                             * itself
                                                                                             */

        protected int              lengthcount;                                            /*
                                                                                             * lengthcount
                                                                                             * =
                                                                                             * H
                                                                                             * *
                                                                                             * W
                                                                                             * *
                                                                                             * 3
                                                                                             */

        protected int              samplefac;                                              /*
                                                                                             * sampling
                                                                                             * factor
                                                                                             * 1.
                                                                                             * .30
                                                                                             */

        // typedef int pixel[4]; /* BGRc */
        protected int[][]          network;                                                /*
                                                                                             * the
                                                                                             * network
                                                                                             * itself
                                                                                             * -
                                                                                             * [
                                                                                             * netsize
                                                                                             * ]
                                                                                             * [
                                                                                             * 4
                                                                                             * ]
                                                                                             */

        protected int[]            netindex        = new int[256];

        /* for network lookup - really 256 */

        protected int[]            bias            = new int[netsize];

        /* bias and freq arrays for learning */
        protected int[]            freq            = new int[netsize];

        protected int[]            radpower        = new int[initrad];

        /* radpower for precomputation */

        /*
         * Initialise network in range (0,0,0) to (255,255,255) and set
         * parameters
         * ------------------------------------------------------------
         * -----------
         */
        public NeuQuant(final byte[] thepic, final int len, final int sample) {

            int i;
            int[] p;

            this.thepicture = thepic;
            this.lengthcount = len;
            this.samplefac = sample;

            this.network = new int[netsize][];
            for (i = 0; i < netsize; i++) {
                this.network[i] = new int[4];
                p = this.network[i];
                p[0] = p[1] = p[2] = (i << (netbiasshift + 8)) / netsize;
                this.freq[i] = intbias / netsize; /* 1/netsize */
                this.bias[i] = 0;
            }
        }

        public byte[] colorMap() {
            final byte[] map = new byte[3 * netsize];
            final int[] index = new int[netsize];
            for (int i = 0; i < netsize; i++) {
                index[this.network[i][3]] = i;
            }
            int k = 0;
            for (int i = 0; i < netsize; i++) {
                final int j = index[i];
                map[k++] = (byte) (this.network[j][0]);
                map[k++] = (byte) (this.network[j][1]);
                map[k++] = (byte) (this.network[j][2]);
            }
            return map;
        }

        /*
         * Insertion sort of network and building of netindex[0..255] (to do
         * after unbias)
         * --------------------------------------------------------
         * -----------------------
         */
        public void inxbuild() {

            int i, j, smallpos, smallval;
            int[] p;
            int[] q;
            int previouscol, startpos;

            previouscol = 0;
            startpos = 0;
            for (i = 0; i < netsize; i++) {
                p = this.network[i];
                smallpos = i;
                smallval = p[1]; /* index on g */
                /* find smallest in i..netsize-1 */
                for (j = i + 1; j < netsize; j++) {
                    q = this.network[j];
                    if (q[1] < smallval) { /* index on g */
                        smallpos = j;
                        smallval = q[1]; /* index on g */
                    }
                }
                q = this.network[smallpos];
                /* swap p (i) and q (smallpos) entries */
                if (i != smallpos) {
                    j = q[0];
                    q[0] = p[0];
                    p[0] = j;
                    j = q[1];
                    q[1] = p[1];
                    p[1] = j;
                    j = q[2];
                    q[2] = p[2];
                    p[2] = j;
                    j = q[3];
                    q[3] = p[3];
                    p[3] = j;
                }
                /* smallval entry is now in position i */
                if (smallval != previouscol) {
                    this.netindex[previouscol] = (startpos + i) >> 1;
                    for (j = previouscol + 1; j < smallval; j++) {
                        this.netindex[j] = i;
                    }
                    previouscol = smallval;
                    startpos = i;
                }
            }
            this.netindex[previouscol] = (startpos + maxnetpos) >> 1;
            for (j = previouscol + 1; j < 256; j++) {
                this.netindex[j] = maxnetpos; /* really 256 */
            }
        }

        /*
         * Main Learning Loop ------------------
         */
        public void learn() {

            int i, j, b, g, r;
            int radius, rad, alpha, step, delta, samplepixels;
            byte[] p;
            int pix, lim;

            if (this.lengthcount < minpicturebytes) {
                this.samplefac = 1;
            }
            this.alphadec = 30 + ((this.samplefac - 1) / 3);
            p = this.thepicture;
            pix = 0;
            lim = this.lengthcount;
            samplepixels = this.lengthcount / (3 * this.samplefac);
            delta = samplepixels / ncycles;
            alpha = initalpha;
            radius = initradius;

            rad = radius >> radiusbiasshift;
            if (rad <= 1) {
                rad = 0;
            }
            for (i = 0; i < rad; i++) {
                this.radpower[i] = alpha
                        * (((rad * rad - i * i) * radbias) / (rad * rad));
            }

            // fprintf(stderr,"beginning 1D learning: initial radius=%d\n",
            // rad);

            if (this.lengthcount < minpicturebytes) {
                step = 3;
            }
            else if ((this.lengthcount % prime1) != 0) {
                step = 3 * prime1;
            }
            else {
                if ((this.lengthcount % prime2) != 0) {
                    step = 3 * prime2;
                }
                else {
                    if ((this.lengthcount % prime3) != 0) {
                        step = 3 * prime3;
                    }
                    else {
                        step = 3 * prime4;
                    }
                }
            }

            i = 0;
            while (i < samplepixels) {
                b = (p[pix + 0] & 0xff) << netbiasshift;
                g = (p[pix + 1] & 0xff) << netbiasshift;
                r = (p[pix + 2] & 0xff) << netbiasshift;
                j = contest(b, g, r);

                altersingle(alpha, j, b, g, r);
                if (rad != 0) {
                    alterneigh(rad, j, b, g, r); /* alter neighbours */
                }

                pix += step;
                if (pix >= lim) {
                    pix -= this.lengthcount;
                }

                i++;
                if (delta == 0) {
                    delta = 1;
                }
                if (i % delta == 0) {
                    alpha -= alpha / this.alphadec;
                    radius -= radius / radiusdec;
                    rad = radius >> radiusbiasshift;
                    if (rad <= 1) {
                        rad = 0;
                    }
                    for (j = 0; j < rad; j++) {
                        this.radpower[j] = alpha
                                * (((rad * rad - j * j) * radbias) / (rad * rad));
                    }
                }
            }
            // fprintf(stderr,"finished 1D learning: final alpha=%f
            // !\n",((float)alpha)/initalpha);
        }

        /*
         * Search for BGR values 0..255 (after net is unbiased) and return
         * colour index
         * ----------------------------------------------------------
         * ------------------
         */
        public int map(final int b, final int g, final int r) {

            int i, j, dist, a, bestd;
            int[] p;
            int best;

            bestd = 1000; /* biggest possible dist is 256*3 */
            best = -1;
            i = this.netindex[g]; /* index on g */
            j = i - 1; /* start at netindex[g] and work outwards */

            while ((i < netsize) || (j >= 0)) {
                if (i < netsize) {
                    p = this.network[i];
                    dist = p[1] - g; /* inx key */
                    if (dist >= bestd) {
                        i = netsize; /* stop iter */
                    }
                    else {
                        i++;
                        if (dist < 0) {
                            dist = -dist;
                        }
                        a = p[0] - b;
                        if (a < 0) {
                            a = -a;
                        }
                        dist += a;
                        if (dist < bestd) {
                            a = p[2] - r;
                            if (a < 0) {
                                a = -a;
                            }
                            dist += a;
                            if (dist < bestd) {
                                bestd = dist;
                                best = p[3];
                            }
                        }
                    }
                }
                if (j >= 0) {
                    p = this.network[j];
                    dist = g - p[1]; /* inx key - reverse dif */
                    if (dist >= bestd) {
                        j = -1; /* stop iter */
                    }
                    else {
                        j--;
                        if (dist < 0) {
                            dist = -dist;
                        }
                        a = p[0] - b;
                        if (a < 0) {
                            a = -a;
                        }
                        dist += a;
                        if (dist < bestd) {
                            a = p[2] - r;
                            if (a < 0) {
                                a = -a;
                            }
                            dist += a;
                            if (dist < bestd) {
                                bestd = dist;
                                best = p[3];
                            }
                        }
                    }
                }
            }
            return (best);
        }

        public byte[] process() {
            learn();
            unbiasnet();
            inxbuild();
            return colorMap();
        }

        /*
         * Unbias network to give byte values 0..255 and record position i to
         * prepare for sort
         * ------------------------------------------------------
         * -----------------------------
         */
        public void unbiasnet() {

            int i;

            for (i = 0; i < netsize; i++) {
                this.network[i][0] >>= netbiasshift;
                this.network[i][1] >>= netbiasshift;
                this.network[i][2] >>= netbiasshift;
                this.network[i][3] = i; /* record colour no */
            }
        }

        /*
         * Move adjacent neurons by precomputed alpha*(1-((i-j)^2/[r]^2)) in
         * radpower[|i-j|]
         * ------------------------------------------------------
         * ---------------------------
         */
        protected void alterneigh(final int rad, final int i, final int b,
                final int g, final int r) {

            int j, k, lo, hi, a, m;
            int[] p;

            lo = i - rad;
            if (lo < -1) {
                lo = -1;
            }
            hi = i + rad;
            if (hi > netsize) {
                hi = netsize;
            }

            j = i + 1;
            k = i - 1;
            m = 1;
            while ((j < hi) || (k > lo)) {
                a = this.radpower[m++];
                if (j < hi) {
                    p = this.network[j++];
                    try {
                        p[0] -= (a * (p[0] - b)) / alpharadbias;
                        p[1] -= (a * (p[1] - g)) / alpharadbias;
                        p[2] -= (a * (p[2] - r)) / alpharadbias;
                    }
                    catch (final Exception e) {
                    } // prevents 1.3 miscompilation
                }
                if (k > lo) {
                    p = this.network[k--];
                    try {
                        p[0] -= (a * (p[0] - b)) / alpharadbias;
                        p[1] -= (a * (p[1] - g)) / alpharadbias;
                        p[2] -= (a * (p[2] - r)) / alpharadbias;
                    }
                    catch (final Exception e) {
                    }
                }
            }
        }

        /*
         * Move neuron i towards biased (b,g,r) by factor alpha
         * ----------------------------------------------------
         */
        protected void altersingle(final int alpha, final int i, final int b,
                final int g, final int r) {

            /* alter hit neuron */
            final int[] n = this.network[i];
            n[0] -= (alpha * (n[0] - b)) / initalpha;
            n[1] -= (alpha * (n[1] - g)) / initalpha;
            n[2] -= (alpha * (n[2] - r)) / initalpha;
        }

        /*
         * Search for biased BGR values ----------------------------
         */
        protected int contest(final int b, final int g, final int r) {

            /* finds closest neuron (min dist) and updates freq */
            /* finds best neuron (min dist-bias) and returns position */
            /*
             * for frequently chosen neurons, freq[i] is high and bias[i] is
             * negative
             */
            /* bias[i] = gamma*((1/netsize)-freq[i]) */

            int i, dist, a, biasdist, betafreq;
            int bestpos, bestbiaspos, bestd, bestbiasd;
            int[] n;

            bestd = ~((1) << 31);
            bestbiasd = bestd;
            bestpos = -1;
            bestbiaspos = bestpos;

            for (i = 0; i < netsize; i++) {
                n = this.network[i];
                dist = n[0] - b;
                if (dist < 0) {
                    dist = -dist;
                }
                a = n[1] - g;
                if (a < 0) {
                    a = -a;
                }
                dist += a;
                a = n[2] - r;
                if (a < 0) {
                    a = -a;
                }
                dist += a;
                if (dist < bestd) {
                    bestd = dist;
                    bestpos = i;
                }
                biasdist = dist
                        - ((this.bias[i]) >> (intbiasshift - netbiasshift));
                if (biasdist < bestbiasd) {
                    bestbiasd = biasdist;
                    bestbiaspos = i;
                }
                betafreq = (this.freq[i] >> betashift);
                this.freq[i] -= betafreq;
                this.bias[i] += (betafreq << gammashift);
            }
            this.freq[bestpos] += beta;
            this.bias[bestpos] -= betagamma;
            return (bestbiaspos);
        }
    }

    // ==============================================================================
    // Adapted from Jef Poskanzer's Java port by way of J. M. G. Elliott.
    // K Weiner 12/00

    static class LZWEncoder {

        private static final int EOF        = -1;

        private final int        imgW, imgH;

        private final byte[]     pixAry;

        private final int        initCodeSize;

        private int              remaining;

        private int              curPixel;

        // GIFCOMPR.C - GIF Image compression routines
        //
        // Lempel-Ziv compression based on 'compress'. GIF modifications by
        // David Rowley (mgardi@watdcsu.waterloo.edu)

        // General DEFINEs

        static final int         BITS       = 12;

        static final int         HSIZE      = 5003;          // 80% occupancy

        // GIF Image compression - modified 'compress'
        //
        // Based on: compress.c - File compression ala IEEE Computer, June 1984.
        //
        // By Authors: Spencer W. Thomas (decvax!harpo!utah-cs!utah-gr!thomas)
        // Jim McKie (decvax!mcvax!jim)
        // Steve Davies (decvax!vax135!petsd!peora!srd)
        // Ken Turkowski (decvax!decwrl!turtlevax!ken)
        // James A. Woods (decvax!ihnp4!ames!jaw)
        // Joe Orost (decvax!vax135!petsd!joe)

        int                      n_bits;                     // number of
        // bits/code

        int                      maxbits    = BITS;          // user settable
        // max # bits/code

        int                      maxcode;                    // maximum code,
        // given n_bits

        int                      maxmaxcode = 1 << BITS;     // should NEVER
        // generate this
        // code

        int[]                    htab       = new int[HSIZE];

        int[]                    codetab    = new int[HSIZE];

        int                      hsize      = HSIZE;         // for dynamic
        // table sizing

        int                      free_ent   = 0;             // first unused
        // entry

        // block compression parameters -- after all codes are used up,
        // and compression rate changes, start over.
        boolean                  clear_flg  = false;

        // Algorithm: use open addressing double hashing (no chaining) on the
        // prefix code / next character combination. We do a variant of Knuth's
        // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
        // secondary probe. Here, the modular division first probe is gives way
        // to a faster exclusive-or manipulation. Also do block compression with
        // an adaptive reset, whereby the code table is cleared when the
        // compression
        // ratio decreases, but after the table fills. The variable-length
        // output
        // codes are re-sized at this point, and a special CLEAR code is
        // generated
        // for the decompressor. Late addition: construct the table according to
        // file size for noticeable speed improvement on small files. Please
        // direct
        // questions about this implementation to ames!jaw.

        int                      g_init_bits;

        int                      ClearCode;

        int                      EOFCode;

        // output
        //
        // Output the given code.
        // Inputs:
        // code: A n_bits-bit integer. If == -1, then EOF. This assumes
        // that n_bits =< wordsize - 1.
        // Outputs:
        // Outputs code to the file.
        // Assumptions:
        // Chars are 8 bits long.
        // Algorithm:
        // Maintain a BITS character long buffer (so that 8 codes will
        // fit in it exactly). Use the VAX insv instruction to insert each
        // code in turn. When the buffer fills up empty it and start over.

        int                      cur_accum  = 0;

        int                      cur_bits   = 0;

        int                      masks[]    = { 0x0000, 0x0001, 0x0003, 0x0007,
                                                    0x000F, 0x001F, 0x003F,
                                                    0x007F, 0x00FF, 0x01FF,
                                                    0x03FF, 0x07FF, 0x0FFF,
                                                    0x1FFF, 0x3FFF, 0x7FFF,
                                                    0xFFFF };

        // Number of characters so far in this 'packet'
        int                      a_count;

        // Define the storage for the packet accumulator
        byte[]                   accum      = new byte[256];

        // ----------------------------------------------------------------------------
        LZWEncoder(final int width, final int height, final byte[] pixels,
                final int color_depth) {
            this.imgW = width;
            this.imgH = height;
            this.pixAry = pixels;
            this.initCodeSize = Math.max(2, color_depth);
        }

        // Add a character to the end of the current packet, and if it is 254
        // characters, flush the packet to disk.
        void char_out(final byte c, final OutputStream outs) throws IOException {
            this.accum[this.a_count++] = c;
            if (this.a_count >= 254) {
                flush_char(outs);
            }
        }

        // Clear out the hash table

        // table clear for block compress
        void cl_block(final OutputStream outs) throws IOException {
            cl_hash(this.hsize);
            this.free_ent = this.ClearCode + 2;
            this.clear_flg = true;

            output(this.ClearCode, outs);
        }

        // reset code table
        void cl_hash(final int hsize) {
            for (int i = 0; i < hsize; ++i) {
                this.htab[i] = -1;
            }
        }

        void compress(final int init_bits, final OutputStream outs)
                throws IOException {
            int fcode;
            int i /* = 0 */;
            int c;
            int ent;
            int disp;
            int hsize_reg;
            int hshift;

            // Set up the globals: g_init_bits - initial number of bits
            this.g_init_bits = init_bits;

            // Set up the necessary values
            this.clear_flg = false;
            this.n_bits = this.g_init_bits;
            this.maxcode = MAXCODE(this.n_bits);

            this.ClearCode = 1 << (init_bits - 1);
            this.EOFCode = this.ClearCode + 1;
            this.free_ent = this.ClearCode + 2;

            this.a_count = 0; // clear packet

            ent = nextPixel();

            hshift = 0;
            for (fcode = this.hsize; fcode < 65536; fcode *= 2) {
                ++hshift;
            }
            hshift = 8 - hshift; // set hash code range bound

            hsize_reg = this.hsize;
            cl_hash(hsize_reg); // clear hash table

            output(this.ClearCode, outs);

            outer_loop: while ((c = nextPixel()) != EOF) {
                fcode = (c << this.maxbits) + ent;
                i = (c << hshift) ^ ent; // xor hashing

                if (this.htab[i] == fcode) {
                    ent = this.codetab[i];
                    continue;
                }
                else if (this.htab[i] >= 0) // non-empty slot
                {
                    disp = hsize_reg - i; // secondary hash (after G. Knott)
                    if (i == 0) {
                        disp = 1;
                    }
                    do {
                        if ((i -= disp) < 0) {
                            i += hsize_reg;
                        }

                        if (this.htab[i] == fcode) {
                            ent = this.codetab[i];
                            continue outer_loop;
                        }
                    }
                    while (this.htab[i] >= 0);
                }
                output(ent, outs);
                ent = c;
                if (this.free_ent < this.maxmaxcode) {
                    this.codetab[i] = this.free_ent++; // code -> hashtable
                    this.htab[i] = fcode;
                }
                else {
                    cl_block(outs);
                }
            }
            // Put out the final code.
            output(ent, outs);
            output(this.EOFCode, outs);
        }

        // ----------------------------------------------------------------------------
        void encode(final OutputStream os) throws IOException {
            os.write(this.initCodeSize); // write "initial code size" byte

            this.remaining = this.imgW * this.imgH; // reset navigation
            // variables
            this.curPixel = 0;

            compress(this.initCodeSize + 1, os); // compress and write the pixel
            // data

            os.write(0); // write block terminator
        }

        // Flush the packet to disk, and reset the accumulator
        void flush_char(final OutputStream outs) throws IOException {
            if (this.a_count > 0) {
                outs.write(this.a_count);
                outs.write(this.accum, 0, this.a_count);
                this.a_count = 0;
            }
        }

        final int MAXCODE(final int n_bits) {
            return (1 << n_bits) - 1;
        }

        // ----------------------------------------------------------------------------
        // Return the next pixel from the image
        // ----------------------------------------------------------------------------
        private int nextPixel() {
            if (this.remaining == 0) {
                return EOF;
            }

            --this.remaining;

            final byte pix = this.pixAry[this.curPixel++];

            return pix & 0xff;
        }

        void output(final int code, final OutputStream outs) throws IOException {
            this.cur_accum &= this.masks[this.cur_bits];

            if (this.cur_bits > 0) {
                this.cur_accum |= (code << this.cur_bits);
            }
            else {
                this.cur_accum = code;
            }

            this.cur_bits += this.n_bits;

            while (this.cur_bits >= 8) {
                char_out((byte) (this.cur_accum & 0xff), outs);
                this.cur_accum >>= 8;
                this.cur_bits -= 8;
            }

            // If the next entry is going to be too big for the code size,
            // then increase it, if possible.
            if (this.free_ent > this.maxcode || this.clear_flg) {
                if (this.clear_flg) {
                    this.maxcode = MAXCODE(this.n_bits = this.g_init_bits);
                    this.clear_flg = false;
                }
                else {
                    ++this.n_bits;
                    if (this.n_bits == this.maxbits) {
                        this.maxcode = this.maxmaxcode;
                    }
                    else {
                        this.maxcode = MAXCODE(this.n_bits);
                    }
                }
            }

            if (code == this.EOFCode) {
                // At EOF, write the rest of the buffer.
                while (this.cur_bits > 0) {
                    char_out((byte) (this.cur_accum & 0xff), outs);
                    this.cur_accum >>= 8;
                    this.cur_bits -= 8;
                }

                flush_char(outs);
            }
        }
    }
}