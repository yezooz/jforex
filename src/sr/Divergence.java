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

package sr;

import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class Divergence {
    private static final Logger LOGGER = LoggerFactory.getLogger(Divergence.class);

    protected IContext context;
    protected Instrument pair;
    protected Period tf;
    protected List<IBar> bars;

    protected double currentMACD;
    protected double[] signal;
    protected double[] macd;

    public Divergence(IContext _context, Instrument _pair, Period _tf) {
        context = _context;
        pair = _pair;
        tf = _tf;
    }

    public boolean isBullish(IBar bar) {
        calculate(bar);

        int peak = getLastTrough();
        if (peak == -1) {
            return false;
        }

        if (currentMACD > macd[peak] && bar.getLow() < bars.get(peak).getLow()) {
            return true;
        }

        return false;
    }

    public boolean isBearish(IBar bar) {
        calculate(bar);

        int peak = getLastPeak();
        if (peak == -1) {
            return false;
        }

        if (currentMACD < macd[peak] && bar.getHigh() > bars.get(peak).getHigh()) {
            return true;
        }

        return false;
    }

    protected void calculate(IBar bar) {
        int limit = 500;

        try {
            bars = context.getHistory().getBars(pair, tf, OfferSide.BID, Filter.ALL_FLATS, limit, bar.getTime(), 0);
            Collections.reverse(bars);

            double[][] macdArr = context.getIndicators().macd(pair, tf, OfferSide.BID, AppliedPrice.CLOSE, 12, 26, 9, Filter.ALL_FLATS, limit, bar.getTime(), 0);
            macd = macdArr[0];
            signal = macdArr[1];
            ArrayUtils.reverse(macd);
            ArrayUtils.reverse(signal);
            currentMACD = macd[0];
        } catch (JFException e) {
            LOGGER.error(e.getMessage());
        }
    }

    protected int getLastPeak() {
        for (int i = 5; i < bars.size(); i++) {
            if (signal[i] >= signal[i + 1] && signal[i] >= signal[i + 2] && signal[i] >= signal[i - 1] && signal[i] >= signal[i - 2]) {
                for (int j = i; j < bars.size(); j++) {
                    if (macd[j] >= macd[j + 1] && macd[j] > macd[j + 2] && macd[j] >= macd[j - 1] && macd[j] > macd[j - 2]) {
                        return j;
                    }
                }
            }
        }
        return -1;
    }

    protected int getLastTrough() {
        for (int i = 5; i < bars.size(); i++) {
            if (signal[i] <= signal[i + 1] && signal[i] <= signal[i + 2] && signal[i] <= signal[i - 1] && signal[i] <= signal[i - 2]) {
                for (int j = i; j < bars.size(); j++) {
                    if (macd[j] <= macd[j + 1] && macd[j] < macd[j + 2] && macd[j] <= macd[j - 1] && macd[j] < macd[j - 2]) {
                        return j;
                    }
                }
            }
        }
        return -1;
    }
}
