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

package trade;

import com.dukascopy.api.*;

import java.util.ArrayList;
import java.util.List;

public class TradeList {

    private List<Trade> list;
    private List<Trade> removeList;

    public TradeList() {
        this.list = new ArrayList<Trade>();
        this.removeList = new ArrayList<Trade>();
    }

    public void add(Trade trade) {
        list.add(trade);
    }

    public void close() {
        for (Trade t : list) {
            t.closeTrade();
        }
    }

    public void newTick(Instrument pair, ITick tick) {
        for (Trade t : list) {
            if (t.getInstrument() != pair) continue;
            if (t.getMode() == TradeMode.CLOSED) {
                removeList.add(t);
                continue;
            }
            t.newTick(tick);
        }

        for (Trade t : removeList) {
            list.remove(t);
        }
        removeList = new ArrayList<Trade>();
    }

    public void newBar(Instrument pair, PriceRange range, IBar bar) {
        for (Trade t : list) {
            if (pair != t.getInstrument()) continue;
            if (range != t.getTf()) continue;
            t.newBar(bar);
        }
    }

    public void newBar(Instrument pair, Period period, IBar bar) {
        for (Trade t : list) {
            if (pair != t.getInstrument()) continue;
            if (period != t.getPeriod()) continue; // TODO
            t.newBar(bar);
        }
    }
}
