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
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import com.dukascopy.api.PriceRange;
import helper.Helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SignalList {

    private List<SignalInstrumentPeriod> list;

    public SignalList() {
        this.list = new ArrayList<SignalInstrumentPeriod>();
    }

    public List<SignalInstrumentPeriod> getList() {
        return list;
    }

    public void add(Instrument pair, PriceRange period, Signal signal) {
        list.add(new SignalInstrumentPeriod(pair, period, signal));
    }

    public void add(Instrument pair, Period period, Signal signal) {
        list.add(new SignalInstrumentPeriod(pair, period, signal));
    }

    public void remove(Instrument pair, PriceRange period, Signal signal) {
        list.remove(new SignalInstrumentPeriod(pair, period, signal));
    }

    public void remove(Instrument pair, Period period, Signal signal) {
        list.remove(new SignalInstrumentPeriod(pair, period, signal));
    }

    public void reverse() {
        Collections.reverse(list);
    }

    // ---

    public Signal getMatchingSignal(Instrument pair, PriceRange priceRange, IBar bar, boolean onHigh) {
        return getMatchingSignalWithMargin(pair, priceRange, bar, onHigh, 0);
    }

    public Signal getMatchingSignalWithMargin(Instrument pair, PriceRange priceRange, IBar bar, boolean onHigh, double margin) {

        for (SignalInstrumentPeriod s : list) {
            if (s.getInstrument() != pair || s.getPriceRange() != priceRange)
                continue;
            if (s.getSignal().getBar() == bar)
                continue;

            if (!onHigh && Helpers.isBetweenValues(s.getSignal().getBar().getLow(), bar.getLow() + margin, bar.getLow() - margin)) {
                list.remove(s);
                return s.getSignal();
            } else if (onHigh && Helpers.isBetweenValues(s.getSignal().getBar().getHigh(), bar.getHigh() + margin, bar.getHigh() - margin)) {
                list.remove(s);
                return s.getSignal();
            }
        }

        return null;
    }

    public Signal getMatchingSignal(Instrument pair, Period period, IBar bar, boolean onHigh) {
        return getMatchingSignalWithMargin(pair, period, bar, onHigh, 0);
    }

    public Signal getMatchingSignalWithMargin(Instrument pair, Period period, IBar bar, boolean onHigh, double margin) {

        for (SignalInstrumentPeriod s : list) {
            if (s.getInstrument() != pair || s.getPeriod() != period)
                continue;
            if (s.getSignal().getBar() == bar)
                continue;

            if (!onHigh && Helpers.isBetweenValues(s.getSignal().getBar().getLow(), bar.getLow() + margin, bar.getLow() - margin)) {
                list.remove(s);
                return s.getSignal();
            } else if (onHigh && Helpers.isBetweenValues(s.getSignal().getBar().getHigh(), bar.getHigh() + margin, bar.getHigh() - margin)) {
                list.remove(s);
                return s.getSignal();
            }
        }

        return null;
    }

    public Double getMargin(IBar bar) {
        return (bar.getHigh() - bar.getLow()) * 0.2;
    }
}
