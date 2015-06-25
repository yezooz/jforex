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

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IRenkoBar;
import helper.Helpers;

public class Signal {

    public int divider;
    protected String strategy;
    protected Instrument instrument;
    protected IBar bar;
    protected PriceRange priceRange;
    protected Period period;
    protected int direction;
    protected IContext context;
    private double rr = 1.0;
    private double risk = 0.001;
    private int expireAfter = 3;

    public Signal(String strategy, Instrument instrument, PriceRange priceRange, IBar bar, int direction, IContext context) {
        this.strategy = strategy;
        this.instrument = instrument;
        this.period = null;
        this.priceRange = priceRange;
        this.bar = bar;
        this.context = context;
        this.direction = direction;

        this.divider = 10000;
        if (instrument.getPipScale() == 2)
            this.divider = 100;
        if (instrument == Instrument.XAUUSD)
            this.divider = 10;
    }

    public Signal(String strategy, Instrument instrument, PriceRange priceRange, IRenkoBar bar, int direction, IContext context) {
        this.strategy = strategy;
        this.instrument = instrument;
        this.period = null;
        this.priceRange = priceRange;
        this.bar = bar;
        this.context = context;
        this.direction = direction;

        this.divider = 10000;
        if (instrument.getPipScale() == 2)
            this.divider = 100;
        if (instrument == Instrument.XAUUSD)
            this.divider = 10;
    }

    public Signal(String strategy, Instrument instrument, Period period, IBar bar, int direction, IContext context) {
        this.strategy = strategy;
        this.instrument = instrument;
        this.period = period;
        this.priceRange = null;
        this.bar = bar;
        this.context = context;
        this.direction = direction;

        this.divider = 10000;
        if (instrument.getPipScale() == 2)
            this.divider = 100;
        if (instrument == Instrument.XAUUSD)
            this.divider = 10;
    }

    public Signal(String strategy, Instrument instrument, PriceRange priceRange, IBar bar1, IBar bar2, int direction, IContext context) {

        GBar bar;
        if (direction == 1) {
            if (bar1.getLow() < bar2.getLow())
                bar = new GBar(bar2.getHigh(), bar1.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
            else
                bar = new GBar(bar2.getHigh(), bar2.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
        } else {
            if (bar1.getHigh() > bar2.getHigh())
                bar = new GBar(bar1.getHigh(), bar2.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
            else
                bar = new GBar(bar2.getHigh(), bar2.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
        }

        this.strategy = strategy;
        this.instrument = instrument;
        this.priceRange = priceRange;
        this.period = null;
        this.bar = bar;
        this.context = context;
        this.direction = direction;

        this.divider = 10000;
        if (instrument.getPipScale() == 2)
            this.divider = 100;
        if (instrument == Instrument.XAUUSD)
            this.divider = 10;
    }

    public Signal(String strategy, Instrument instrument, Period period, IBar bar1, IBar bar2, int direction, IContext context) {

        GBar bar;
        if (direction == 1) {
            if (bar1.getLow() < bar2.getLow())
                bar = new GBar(bar2.getHigh(), bar1.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
            else
                bar = new GBar(bar2.getHigh(), bar2.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
        } else {
            if (bar1.getHigh() > bar2.getHigh())
                bar = new GBar(bar1.getHigh(), bar2.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
            else
                bar = new GBar(bar2.getHigh(), bar2.getLow(), bar2.getOpen(), bar2.getClose(), bar2.getTime());
        }

        this.strategy = strategy;
        this.instrument = instrument;
        this.priceRange = null;
        this.period = period;
        this.bar = bar;
        this.context = context;
        this.direction = direction;

        this.divider = 10000;
        if (instrument.getPipScale() == 2)
            this.divider = 100;
        if (instrument == Instrument.XAUUSD)
            this.divider = 10;
    }

    public boolean isBullish() {
        return direction == 1;
    }

    public boolean isBearish() {
        return direction == -1;
    }

    // For Trading

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String _strategy) {
        strategy = _strategy;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public PriceRange getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(PriceRange priceRange) {
        this.priceRange = priceRange;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public String getTf() {
        if (priceRange != null)
            return priceRange.getPipCount() + "P";
        else if (period != null)
            return Helpers.tfToString(period);
        return "";
    }

    public int getPoint() {
        return divider;
    }

    public double getSlippage() {
        return 5;
    }

    public double getRR() {
        return this.rr;
    }

    public void setRR(double rr) {
        this.rr = rr;
    }

    public double getSpread() {
        try {
            return context.getHistory().getTick(instrument, 0).getAsk() - context.getHistory().getTick(instrument, 0).getBid();
        } catch (JFException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public double getCommission() {
        return 0;
    }

    public double getEntryPoint() {
        return ((bar.getHigh() - bar.getLow()) * 0.1);
    }

    public double getEntry() {
        if (isBullish()) return bar.getHigh() + getEntryPoint();
        if (isBearish()) return bar.getLow() - getEntryPoint();
        return 0.0;
    }

    public double getExit() {
        return ((bar.getHigh() - bar.getLow()) * 0.1);
    }

    public double getExit(boolean getV) {
        if (isBullish()) return bar.getLow() - getExit();
        if (isBearish()) return bar.getHigh() + getExit();
        return 0.0;
    }

    public double getTP() {
        if (isBullish())
            return bar.getHigh() + ((bar.getHigh() - bar.getLow()) + getSpread() + getEntryPoint() + getExit()) * getRR();
        if (isBearish())
            return bar.getLow() - ((bar.getHigh() - bar.getLow()) + getSpread() + getEntryPoint() + getExit()) * getRR();

        return 0.0;
    }

    public double getSL() {
        return getExit(true);
    }

    public double getBEOffset() {
        if (isBullish())
            return bar.getHigh() + ((bar.getHigh() - bar.getLow()) + getEntry() + getSpread() + getCommission() + (2 / divider));
        if (isBearish())
            return bar.getLow() - ((bar.getHigh() - bar.getLow()) + getEntry() + getSpread() + getCommission() + (2 / divider));
        return 0.0;
    }

    public double getBE() {
        if (isBullish()) return bar.getHigh() + ((bar.getHigh() - bar.getLow()) * getRR());
        if (isBearish()) return bar.getLow() - ((bar.getHigh() - bar.getLow()) * getRR());
        return 0.0;
    }

    public IBar getBar() {
        return bar;
    }

    public void setBar(IBar bar) {
        this.bar = bar;
    }

    public double getRisk() {
        return this.risk;
    }

    public void setRisk(double risk) {
        this.risk = risk;
    }

    public int getExpireAfter() {
        return this.expireAfter;
    }

    public void setExpireAfter(int bars) {
        this.expireAfter = bars;
    }
}
