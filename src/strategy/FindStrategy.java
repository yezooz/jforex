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
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import signal.PinBar;
import signal.Signal;
import sr.*;
import strategyhub.TestStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class FindStrategy extends TestStrategy {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FindStrategy.class);
    protected Store store;
    protected Divergence div;
    protected SR sr;
    int tests = 0;

    public FindStrategy(IContext context, IChart chart) {
        super(context, chart);
    }

    public void onStart() {
        super.onStart();

        try {
            context.getIndicators().registerCustomIndicator(new File("." + System.getProperty("file.separator") + "Pin.jfx"));
        } catch (JFException e) {
            e.printStackTrace();
        }

        if (chart != null) {
            chart.add(context.getIndicators().getIndicator("ZIGZAG"), zzSettings);
            chart.add(context.getIndicators().getIndicator("Pin"), pinSettings);
            chart.add(context.getIndicators().getIndicator("SMA"), new Object[]{200});
        }

        store = new Store();
    }

    public void onBar(Instrument instrument, Period period, IBar bar, IFeedDescriptor feedDescriptor) {
        super.onBar(instrument, period, bar, feedDescriptor);

        if (bar.getVolume() == 0) {
            return;
        }

        zz = new ArrayList<Double>();
        bars = new ArrayList<IBar>();

        try {
            saveBars();
            saveSR();

            drawSR();
            playSR();

//			tester();
            pin();
        } catch (JFException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private void drawSR() throws JFException {
        if (tests > 0) return;

        if (sr == null) {
            sr = new SR(pair, period);
        }

        for (Level lvl : sr.getLevels()) {
            Date endDate = lvl.getEndDate();
            if (endDate == null) {
                endDate = new Date();
            }
            Helpers.drawRectangle(chart, lvl.getStartDate().getTime(), lvl.getHigh(), endDate.getTime(), lvl.getLow(), (lvl.getDir().equals(Type.SUPPORT) ? Color.GREEN : Color.RED), Helpers.tfToString(lvl.getTF()));
        }
        tests++;
    }

    @SuppressWarnings("unused")
    private void playSR() throws JFException {
        sr = new SR(pair, period);

        if (chart != null) {
            drawSR();
        }

        Object[] pinFeed = context.getIndicators().calculateIndicator(fD, new OfferSide[]{offerSide}, "Pin", new AppliedPrice[]{AppliedPrice.CLOSE}, pinSettings, 1, bar.getTime(), 0);
        int[] pins = (int[]) pinFeed[0];
        int pin = pins[0];

        if (pin != 0) {
//			Divergence div = new Divergence(context, pair, period);

            if (pin == 100) { // && div.isBullish(bar)) {
                PinBar s = new PinBar("pin_sr", pair, period, bar, 1, context);
                if (sr.inArea(s)) {
                    signalUP(s);
                }
            } else if (pin == -100) { // && div.isBearish(bar)) {
                PinBar s = new PinBar("pin_sr", pair, period, bar, -1, context);
                if (sr.inArea(s)) {
                    signalDOWN(s);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void saveSR() throws JFException {
        SR sr = new SR(pair, period);
        sr.runGenerator();
    }

    @SuppressWarnings("unused")
    private void saveBars() throws JFException {
        store.saveAllBars(context);
        System.exit(0);
    }

    private void tester() throws JFException {

    }

    @SuppressWarnings("unused")
    private void pin() throws JFException {
        Object[] pinFeed = context.getIndicators().calculateIndicator(fD, new OfferSide[]{offerSide}, "Pin", new AppliedPrice[]{AppliedPrice.CLOSE}, pinSettings, 1, bar.getTime(), 0);
        int[] pins = (int[]) pinFeed[0];
        int pin = pins[0];

        double sma = context.getIndicators().sma(pair, period, offerSide, AppliedPrice.CLOSE, 200, 1);

        if (pin != 0) {
//			collectZZPoints();
//			zz.get(0) == bar.getLow() &&
            if (pin == 100 && bar.getHigh() > sma) {
                Signal s = new PinBar("pin_sma", pair, period, bar, 1, context);
                signalUP(s);
            } else if (pin == -100 && bar.getLow() < sma) {
                Signal s = new PinBar("pin_sma", pair, period, bar, -1, context);
                signalDOWN(s);
            }
        }
    }

    public void signalUP(Signal signal) {
        if (chart != null) {
            IBar bar0 = null;
            try {
                bar0 = context.getHistory().getBar(pair, period, offerSide, 0);
            } catch (JFException e) {
                e.printStackTrace();
            }
            ISignalUpChartObject chartSignal = chart.getChartObjectFactory().createSignalUp(Helpers.getKey("up"), bar0.getTime(), signal.getBar().getHigh());
            chart.add(chartSignal);
        }
    }

    public void signalDOWN(Signal signal) {
        if (chart != null) {
            IBar bar0 = null;
            try {
                bar0 = context.getHistory().getBar(pair, period, offerSide, 0);
            } catch (JFException e) {
                e.printStackTrace();
            }

            ISignalDownChartObject chartSignal = chart.getChartObjectFactory().createSignalDown(Helpers.getKey("down"), bar0.getTime(), signal.getBar().getLow());
            chart.add(chartSignal);
        }
    }

    public void newSignal(Signal signal) {
        report.addSignal(signal);
    }
}
