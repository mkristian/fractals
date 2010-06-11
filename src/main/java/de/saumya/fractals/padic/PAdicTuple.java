package de.saumya.fractals.padic;

public class PAdicTuple {

    private final PAdic[] tuple;

    private final short[] orders;

    public PAdicTuple(final int n, final short s) {
        this(n, new short[] { s });
    }

    public PAdicTuple(final int n, final short... p) {
        this.tuple = new PAdic[n * p.length];
        int max = 0;
        for (final int pp : p) {
            max = pp > max ? pp : max;
        }
        int cursor = 1 << max;
        this.orders = new short[cursor];
        while (--cursor >= 0) {
            this.orders[cursor] = order(cursor, 32);
        }
        for (int i = 0; i < n * p.length;) {
            for (final int pp : p) {
                this.tuple[i++] = new PAdic(pp);
            }
        }
    }

    long maxValue() {
        return maxValue(0, 1);
    }

    long maxValue(final int offset, final int dimension) {
        long result = 1;
        for (int i = offset; i < this.tuple.length; i += dimension) {
            result *= this.tuple[i].p();
        }
        return result;
    }

    long xValue() {
        return value(0, 2);
    }

    long yValue() {
        return value(1, 2);
    }

    long value() {
        return value(0, 1);
    }

    void set(long value) {
        for (int i = 0; i < this.tuple.length; i++) {
            this.tuple[i].q = (int) value % this.tuple[i].p();
            value /= this.tuple[i].p();
        }
    }

    long value(final int offset, final int dimension) {
        long base = 1l;
        long result = 0;
        for (int i = offset; i < this.tuple.length; i += dimension) {
            result += base * this.tuple[i].q;
            base *= this.tuple[i].p();
        }
        return result;
    }

    boolean increment() {
        final int n = this.tuple.length;
        for (int i = 0; i < n; i++) {
            if (increment(i)) {
                return true;
            }
        }
        return false;
    }

    boolean increment(final int i) {
        return this.tuple[i].increment();
    }

    long alphabet() {
        long mask = 0l;
        for (final PAdic p : this.tuple) {
            mask |= 1 << p.q;
        }
        return mask;
    }

    short order() {
        return this.orders[(int) alphabet()];
    }

    private short order(final long mask, final int digits) {
        short order = 0;
        long cursor = 1l << digits;
        while (cursor != 0) {
            if ((mask & cursor) != 0) {
                order++;
            }
            cursor >>= 1;
        }
        return order;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("[");
        for (final PAdic p : this.tuple) {
            buf.append(p.q).append(",");
        }
        buf.deleteCharAt(buf.length() - 1);
        buf.append("]");
        return buf.toString();
    }
}
