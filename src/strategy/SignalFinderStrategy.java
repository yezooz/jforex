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

package strategy;


import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;
import com.dukascopy.api.feed.IFeedDescriptor;
import jforex.SignalStorage;
import signal.PinBar;
import sr.SR;
import strategyhub.TestStrategy;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

public class SignalFinderStrategy extends TestStrategy {

    protected IBar bar0;
    protected SignalStorage sig;
    protected SR sr;

    public SignalFinderStrategy(IContext context, IChart chart) {
        super(context, chart);
    }

    public void onStart() {
        super.onStart();

        try {
            context.getIndicators().registerCustomIndicator(new File("." + System.getProperty("file.separator") + "Pin.jfx"));
        } catch (JFException e) {
            e.printStackTrace();
        }
    }

    public void onBar(Instrument instrument, Period period, IBar bar, IFeedDescriptor feedDescriptor) {
        super.onBar(instrument, period, bar, feedDescriptor);

        if (bar.getVolume() == 0) {
            return;
        }

        SendBar.send(instrument, period, bar);

        if (period == Period.ONE_HOUR) {
            PairUpdate.send(instrument, context);
        }

        sr = new SR(instrument, period);

        try {
            bar0 = context.getHistory().getBar(pair, period, offerSide, 0);
            sig = new SignalStorage(pair, period, bar, new Date(bar0.getTime()));
            reCollectZZPoints();

            boolean isNewZZ = false;
            if (newZZPointConfirmed()) {
                sr.newZZ(bar0, barOnZZ(bar, 1), (zz.get(1) > zz.get(2) ? Type.RESISTANCE : Type.SUPPORT));
                isNewZZ = true;
            }

            sr.newBar(bar, bar0, isNewZZ);

            this.pin();

            sig.store();

        } catch (JFException e) {
            e.printStackTrace();
        }
    }

    private void star() throws JFException {
        int[] m = context.getIndicators().cdlMorningStar(pair, period, offerSide, 0, Filter.ALL_FLATS, 1, bar.getTime(), 0);
        int[] e = context.getIndicators().cdlEveningStar(pair, period, offerSide, 0, Filter.ALL_FLATS, 1, bar.getTime(), 0);

        if (m[0] != 0 || e[0] != 0) {
            // TODO
            sig.isStar();
            if (m[0] != 0) sig.setDirection(100);
            if (e[0] != 0) sig.setDirection(-100);
        }
    }

    private void pin() throws JFException {
        Object[] pinFeed = context.getIndicators().calculateIndicator(fD, new OfferSide[]{offerSide}, "Pin", new AppliedPrice[]{AppliedPrice.CLOSE}, pinSettings, 1, bar.getTime(), 0);

        int[] pins = (int[]) pinFeed[0];

        int pin = pins[0];
        if (pin == 0) return;

        PinBar p = new PinBar("pin", pair, period, bar, (pin / 100), context);
        if (!sr.inArea(p)) {
            return;
        }

        sig.isPin();
        sig.setDirection(pin);
    }

    private void engulfing() throws JFException {
        int[] ens = context.getIndicators().cdlEngulfing(pair, period, offerSide, Filter.ALL_FLATS, 1, bar.getTime(), 0);
        if (ens[0] != 0) {
            // TODO
            sig.isEngulfing();
            sig.setDirection(ens[0]);
        }
    }

    private void dblm() throws JFException {
        int[] ms = context.getIndicators().cdlMarubozu(pair, period, offerSide, Filter.ALL_FLATS, 3, bar.getTime(), 0);
        int m1 = ms[2];
        int m2 = ms[1];
        int m3 = ms[0];

        if (m1 != 0 && m2 != 0 && m3 != 0 && m2 == m3 && m1 != m2) {
            // TODO
            sig.isTripleMarubozu();
            sig.setDirection(m1);
        } else if (m1 != 0 && m2 != 0 && m1 != m2) {
            // TODO
            sig.isDblMarubozu();
            sig.setDirection(m1);
        } else {
            ms = context.getIndicators().cdlClosingMarubozu(pair, period, offerSide, Filter.ALL_FLATS, 3, bar.getTime(), 0);
            m1 = ms[2];
            m2 = ms[1];
            m3 = ms[0];

            if (m1 != 0 && m2 != 0 && m3 != 0 && m2 == m3 && m1 != m2) {
                sig.isTripleClosingMarubozu();
                sig.setDirection(m1);
            } else if (m1 != 0 && m2 != 0 && m1 != m2) {
                sig.isDblClosingMarubozu();
                sig.setDirection(m1);
            }
        }
    }

    // ---

    private boolean isWeekend() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(bar.getTime()));

        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            return true;
        }
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY && cal.get(Calendar.HOUR_OF_DAY) > 21) {
            return true;
        }
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && cal.get(Calendar.HOUR_OF_DAY) < 22) {
            return true;
        }

        return false;
    }
}