/**
 * 
 */
package de.saumya.fractals.padic;

class PAdic {

    final int p;

    int       q;

    public PAdic(final int p) {
        this(p, 0);
    }

    public PAdic(final int p, final int q) {
        this.p = p - 1;
        this.q = q;
    }

    int p() {
        return this.p + 1;
    }

    boolean increment() {
        if (this.q == this.p) {
            this.q = 0;
            return false;
        }
        else {
            this.q++;
            return true;
        }
    }

    boolean decrement() {
        if (this.q == 0) {
            this.q = this.p;
            return false;
        }
        else {
            this.q--;
            return true;
        }
    }
}