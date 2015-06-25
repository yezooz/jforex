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

package jforex;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.*;
import com.dukascopy.api.feed.util.RangeBarFeedDescriptor;
import com.dukascopy.api.feed.util.RenkoFeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import helper.Helpers;
import strategy.*;
import strategyhub.AStrategy;
import trade.TradeManager;

import java.util.HashSet;
import java.util.Set;

public class Strategy implements IStrategy {

    //	private IEngine engine;
    private IAccount account;
    private IChart chart;
    private IContext context;
    private String[] args;

    private AStrategy aStrategy;

    public Strategy() {

    }

    public Strategy(String[] args) {
        this.args = args;
    }

    public void onStart(IContext context) throws JFException {

        TradeManager tm = TradeManager.getInstance(context);

        if (args != null && args.length > 0) {

            Set<Instrument> instruments = new HashSet<Instrument>();
            for (String pair : args[0].split(",")) {
                instruments.add(Instrument.valueOf(pair));
            }
            tm.setInstruments(instruments);

            Set<Period> periods = new HashSet<Period>();
            Set<PriceRange> priceRanges = new HashSet<PriceRange>();
            Set<PriceRange> renkoRanges = new HashSet<PriceRange>();

            for (String tf : args[3].split(",")) {
                if (tf.contains("M")) {
                    periods.add(Period.createCustomPeriod(Unit.Minute, Integer.parseInt(tf.replace("M", ""))));
                } else if (tf.contains("H")) {
                    periods.add(Period.createCustomPeriod(Unit.Hour, Integer.parseInt(tf.replace("H", ""))));
                } else if (tf.contains("D")) {
                    periods.add(Period.createCustomPeriod(Unit.Day, Integer.parseInt(tf.replace("D", ""))));
                } else if (tf.contains("W")) {
                    periods.add(Period.createCustomPeriod(Unit.Week, Integer.parseInt(tf.replace("W", ""))));
                } else if (tf.contains("P")) {
                    priceRanges.add(PriceRange.valueOf(Integer.parseInt(tf.replace("P", ""))));
                } else if (tf.contains("X")) {
                    renkoRanges.add(PriceRange.valueOf(Integer.parseInt(tf.replace("X", ""))));
                }
            }
            tm.setPeriods(periods);
            tm.setPriceRange(priceRanges);
            tm.setRenkoRange(renkoRanges);

            tm.setStartDate(Helpers.dateFromString(args[1]));
            tm.setEndDate(Helpers.dateFromString(args[2]));

            tm.setArgs(args);
        }

        this.context = context;
        this.account = context.getAccount();
        try {
            this.chart = context.getChart((Instrument) tm.playableInstruments().toArray()[0]);
        } catch (Exception e) {
            this.chart = null;
        }

        // DEMO
        this.aStrategy = new SignalFinderStrategy(context, chart);

        // TESTING
//        this.aStrategy = new FindStrategy(context, chart);

        for (Instrument pair : tm.playableInstruments()) {
//	        for (PriceRange period : tm.playableRangeBarPeriods()) {
//	        	if (tm.playableInstrumentAndPriceRangeCombination(pair, period))
//	        		context.subscribeToRangeBarFeed(pair, OfferSide.BID, period, new RangeBarFeedListener());
//	        }
//	        for (PriceRange brick : tm.playableRenkoBarPeriods()) {
//	        	context.subscribeToRenkoBarFeed(pair, OfferSide.BID, brick, new RenkoBarFeedListener());
//	        }
            for (Period period : tm.playablePeriods()) {
                context.subscribeToBarsFeed(pair, period, OfferSide.BID, new BarFeedListener());
            }
        }

        aStrategy.onStart();
    }

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {

        if (message.getType() == IMessage.Type.ORDER_SUBMIT_REJECTED) {
            System.out.println(message.toString());
        } else if (message.getType() == IMessage.Type.ORDER_FILL_OK) {
            System.out.println(message.toString());
        } else if (message.getType() == IMessage.Type.ORDER_FILL_REJECTED) {
            System.out.println(message.toString());
        } else if (message.getType() == IMessage.Type.ORDER_SUBMIT_OK) {
            System.out.println(message.toString());
        } else if (message.getType() == IMessage.Type.ORDER_CHANGED_OK) {
            System.out.println(message.toString());
        } else if (message.getType() == IMessage.Type.ORDER_CLOSE_OK) {
            IOrder order = message.getOrder();

            if (order.getProfitLossInPips() == 0) return;

            if (order.getProfitLossInPips() > 0) {
                onTP(order);
            } else {
                onSL(order);
            }
        } else if (message.getType() == IMessage.Type.CALENDAR || message.getType() == IMessage.Type.INSTRUMENT_STATUS) {
            return;
        } else {
            System.out.println(message.toString());
        }
    }

    public void onStop() throws JFException {
        aStrategy.onStop();

//    	TradeManager.getInstance(context).redisPool.destroy();

        System.out.println("Final Balance: $" + account.getBalance());
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
        aStrategy.onTick(instrument, tick);
    }

    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

    }

    public void onTP(IOrder order) {
        aStrategy.onTP(order);
    }

    public void onSL(IOrder order) {
        aStrategy.onSL(order);
    }

    public void onBar(Instrument instrument, Period period, OfferSide offerSide, IBar bar) {

    }

    public class RangeBarFeedListener implements IRangeBarFeedListener {

        public void onBar(Instrument instrument, OfferSide offerSide, PriceRange priceRange, IRangeBar bar) {
            aStrategy.onRangeBar(instrument, offerSide, priceRange, bar, new RangeBarFeedDescriptor(instrument, priceRange, OfferSide.BID));
        }
    }

    public class RenkoBarFeedListener implements IRenkoBarFeedListener {

        public void onBar(Instrument instrument, OfferSide offerSide, PriceRange brickSize, IRenkoBar bar) {
            aStrategy.onRenkoBar(instrument, offerSide, brickSize, bar, new RenkoFeedDescriptor(instrument, brickSize, offerSide));
        }
    }

    public class BarFeedListener implements IBarFeedListener {

        public void onBar(Instrument instrument, Period period, OfferSide offerSide, IBar bar) {
            aStrategy.onBar(instrument, period, bar, new TimePeriodAggregationFeedDescriptor(
                    instrument,
                    period,
                    OfferSide.BID,
                    Filter.ALL_FLATS
            ));
        }
    }
}
