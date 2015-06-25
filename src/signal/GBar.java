/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Marek Mikuliszyn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package signal;

import com.dukascopy.api.IBar;
import helper.Helpers;
import helper.Helpers.DIRECTION;

public class GBar implements IBar {
    private double high;
    private double low;
    private double open;
    private double close;
    private long time;

    public GBar(double high, double low, double open, double close, long time) {
        this.high = high;
        this.low = low;
        this.open = open;
        this.close = close;
        this.time = time;
    }

    public GBar(IBar bar1, IBar bar2) {
        this.open = bar2.getOpen();
        this.close = bar2.getClose();
        this.time = bar2.getTime();

        if (bar1.getHigh() > bar2.getHigh())
            this.high = bar1.getHigh();
        else
            this.high = bar2.getHigh();

        if (bar1.getLow() < bar2.getLow())
            this.low = bar1.getLow();
        else
            this.low = bar2.getLow();
    }

    public static GBar GBarFromMarubozus(IBar bar1, IBar bar2) {
        double open = bar2.getOpen();
        double close = bar2.getClose();
        long time = bar2.getTime();
        double high;
        double low;

        if (Helpers.getCandleDirection(bar2) == DIRECTION.BUY) {
            if (bar1.getLow() < bar2.getLow())
                low = bar1.getLow();
            else
                low = bar2.getLow();
        } else {
            if (bar1.getHigh() > bar2.getHigh())
                high = bar1.getHigh();
            else
                high = bar2.getHigh();
        }

        high = bar2.getHigh();
        low = bar2.getLow();

        return new GBar(high, low, open, close, time);
    }

    public long getTime() {
        return time;
    }

    public double getOpen() {
        return open;
    }

    public double getClose() {
        return close;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getVolume() {
        return 0;
    }

    public String toString() {
        return String.format("OHLC (%s, \t%s,\t%s,\t%s)\t[%s]", Double.toString(getOpen()), Double.toString(getHigh()), Double.toString(getLow()), Double.toString(getClose()), Helpers.formatDateTime(getTime()));
    }

    public double candleSize() {
        return getHigh() - getLow();
    }

    public double bodySize() {
        return Math.abs(getOpen() - getClose());
    }

}
