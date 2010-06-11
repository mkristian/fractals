/**
 * 
 */
package de.saumya.fractals.padic;

class PAdicTupleProjection {

    private final int len;
    final PAdicTuple  tuple;

    long              maxValue;

    private long      cursor;
    double            step;
    private double    position;
    private long      start;
    private long      end;

    PAdicTupleProjection(final PAdicTuple tuple, final int len) {
        this.len = len;
        this.tuple = tuple;
        setup(0, tuple.maxValue());
    }

    void setup(final long startValue, final long endValue) {
        this.tuple.set(startValue);
        this.maxValue = endValue;
        this.step = ((double) (endValue - startValue)) / this.len;
        this.start = startValue;
        this.end = endValue;
        this.cursor = 0;
        this.position = 0.;
    }

    void reset() {
        setup(this.start, this.end);
    }

    boolean hasNext() {
        final boolean hasNext = this.cursor < this.maxValue;
        if (!hasNext) {
            System.err.println("overflow");
            setup(this.start, this.end);
        }
        return hasNext;
    }

    double nextOrder() {
        double result = 0;
        this.position += this.step;
        final long next = Math.round(this.position);
        final long diff = next - this.cursor;
        for (; this.cursor < next; this.cursor++) {
            result += this.tuple.order();
            this.tuple.increment();
        }
        return result / diff;
    }

    double nextAlphabet() {
        double result = 0;
        this.position += this.step;
        final long next = Math.round(this.position);
        final long diff = next - this.cursor;
        for (; this.cursor < next; this.cursor++) {
            result += this.tuple.alphabet();
            this.tuple.increment();
        }
        return result / diff;
    }
}