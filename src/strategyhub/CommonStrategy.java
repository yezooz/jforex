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

package strategyhub;

import com.dukascopy.api.*;
import com.dukascopy.api.DataType.DataPresentationType;
import com.dukascopy.api.drawings.IOhlcChartObject;
import com.dukascopy.api.drawings.ISignalDownChartObject;
import com.dukascopy.api.drawings.ISignalUpChartObject;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IRangeBar;
import com.dukascopy.api.feed.IRenkoBar;
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import signal.Signal;
import signal.SignalList;
import trade.TradeList;
import trade.TradeManager;

import java.sql.Connection;

public class CommonStrategy implements AStrategy {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CommonStrategy.class);

    protected IContext context;
    protected IChart chart;
    protected TradeManager tradeManager;

    protected IFeedDescriptor fD = null;
    protected OfferSide offerSide = OfferSide.BID;
    protected Instrument pair = null;
    protected Period period = null;
    protected PriceRange range = null;
    protected IBar bar = null;
    protected IRenkoBar renko = null;
    protected IRangeBar rangeBar = null;
    protected ITick tick = null;

    protected SignalList signalList;
    protected TradeList tradeList;
    protected Connection con;
    protected List<Double> zz;
    protected Map<Instrument, Map<Period, Double>> lastZZ;

    // Settings
    protected Object[] pinSettings = new Object[]{1, 33, 15, 0, 100};
    protected Object[] zzSettings = new Object[]{12, 5, 3};
    protected Object[] MACD = {12, 26, 9};

    public CommonStrategy(IContext context, IChart chart) {
        this.context = context;
        this.chart = chart;
        this.tradeManager = new TradeManager(context);

        this.signalList = new SignalList();
        this.tradeList = new TradeList();
        this.zz = new ArrayList<Double>();
        this.lastZZ = new HashMap<Instrument, Map<Period, Double>>();

        if (chart != null) {
            chart.setDataPresentationType(DataPresentationType.CANDLE);
        }
    }

    public void onRangeBar(Instrument _instrument, OfferSide _offerSide, PriceRange _priceRange, IRangeBar _bar, IFeedDescriptor _feedDescriptor) {
        pair = _instrument;
        range = _priceRange;
        bar = _bar;
        rangeBar = _bar;
        fD = _feedDescriptor;

        tradeList.newBar(pair, range, bar);
    }

    public void onRenkoBar(Instrument _instrument, OfferSide offerSide, PriceRange _priceRange, IRenkoBar _bar, IFeedDescriptor _feedDescriptor) {
        pair = _instrument;
        range = _priceRange;
        bar = _bar;
        renko = _bar;
        fD = _feedDescriptor;

        tradeList.newBar(pair, range, bar);
    }

    public void onBar(Instrument _instrument, Period _period, IBar _bar, IFeedDescriptor _feedDescriptor) {
        pair = _instrument;
        period = _period;
        bar = _bar;
        fD = _feedDescriptor;

        tradeList.newBar(pair, period, bar);
    }

    public void onTick(Instrument _instrument, ITick _tick) {
        pair = _instrument;
        tick = _tick;

        tradeList.newTick(pair, tick);
    }

    public void onStart() {
        if (chart != null) {
            IOhlcChartObject ohlc = null;
            for (IChartObject obj : chart.getAll()) {
                if (obj instanceof IOhlcChartObject) {
                    ohlc = (IOhlcChartObject) obj;
                }
            }
            if (ohlc == null) {
                ohlc = chart.getChartObjectFactory().createOhlcInformer();
                ohlc.setPreferredSize(new Dimension(100, 200));
                chart.add(ohlc);
            }
            ohlc.setShowIndicatorInfo(true);
        }
    }

    public void onStop() {
        tradeList.close();

        if (chart == null) {
            System.exit(0);
        }
    }

    public void onTP(IOrder order) {

    }

    public void onSL(IOrder order) {

    }

    public void newSignal(Signal signal) {

    }

    public void signalUP(Signal signal) {
        if (chart != null) {
            ISignalUpChartObject chartSignal = chart.getChartObjectFactory().createSignalUp(Helpers.getKey("up"), signal.getBar().getTime(), signal.getBar().getHigh());
            chart.add(chartSignal);
        }

        newSignal(signal);
    }

    public void signalDOWN(Signal signal) {
        if (chart != null) {
            ISignalDownChartObject chartSignal = chart.getChartObjectFactory().createSignalDown(Helpers.getKey("down"), signal.getBar().getTime(), signal.getBar().getLow());
            chart.add(chartSignal);
        }

        newSignal(signal);
    }

    // --- Common functions

    protected void collectZZPoints() {
        if (zz.size() == 0) {
            zz = Helpers.getZZPoints(context, fD, bar.getTime(), 300, zzSettings);
        }
    }

    protected void reCollectZZPoints() {
        zz = new ArrayList<Double>();
        collectZZPoints();
    }

    protected boolean newZZPointConfirmed() {
        collectZZPoints();

        if (!lastZZ.containsKey(pair)) {
            lastZZ.put(pair, new HashMap<Period, Double>());
        }
        if (!lastZZ.get(pair).containsKey(period)) {
            lastZZ.get(pair).put(period, zz.get(1));
        }

        if (zz.get(1).equals(lastZZ.get(pair).get(period))) {
            return false;
        } else {
            lastZZ.get(pair).put(period, zz.get(1));
            return true;
        }
    }

    protected IBar barOnZZ(IBar startBar, int shift) throws JFException {
        collectZZPoints();

        List<IBar> bars = context.getHistory().getBars(pair, period, OfferSide.BID, Filter.ALL_FLATS, 300, startBar.getTime(), 0);
        Collections.reverse(bars);

        for (IBar b : bars) {
            if (zz.get(shift) > zz.get(shift + 1) && b.getHigh() == zz.get(shift)) {
                return b;
            } else if (zz.get(shift) < zz.get(shift + 1) && b.getLow() == zz.get(shift)) {
                return b;
            }
        }

        return null;
    }

    // --- Helpers

    public double getLot(Instrument pair, double risk, double size) {
        double minLot = 0.001;
        double maxLot = 100.0;

        double amnt = context.getAccount().getBalance() * risk;

        double lot = 0;
        if (pair.getPipScale() == 4)
            lot = (amnt / size) / 1000000;
        else
            lot = (amnt / size) / 10000;

        if (lot > maxLot) return (maxLot);
        if (lot < minLot) return (minLot);

        return lot;
    }

}
