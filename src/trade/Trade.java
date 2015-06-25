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
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IOrder.State;
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import signal.Signal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Trade {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Trade.class);

    private Instrument pair;
    private PriceRange rangeTf;
    private Period periodTf;
    private Signal signal;
    private IContext context;
    private String hash;
    private TradeMode mode;
    private Date killDate;

    private Double entry;
    private Double exit;
    private Double tp;
    private Integer expireAfter = 3;
    @SuppressWarnings("unused")
    private Double profit = 0.0;
    private Double rr;
    private Double risk = 0.001;
    private boolean reverseSignal = false;

    private Date openTime;
    private Date closeTime;

    private double size;

    public Trade(Signal _signal, IContext _context, Double[] settings) {
        signal = _signal;
        pair = signal.getInstrument();
        rangeTf = signal.getPriceRange();
        periodTf = signal.getPeriod();
        context = _context;
        if (this.periodTf != null)
            hash = String.format("%s__%s__%s__%s", signal.getStrategy(), signal.getInstrument().name(), Helpers.tfToString(signal.getPeriod()), UUID.randomUUID().toString().replace('-', '0'));
        else if (rangeTf != null)
            hash = String.format("%s__%s__%sP__%s", signal.getStrategy(), signal.getInstrument().name(), signal.getPriceRange().getPipCount(), UUID.randomUUID().toString().replace('-', '0'));
        else
            hash = "T" + UUID.randomUUID().toString().replace('-', '0');
        mode = TradeMode.AWAITING;
        expireAfter = settings[0].intValue(); //signal.getExpireAfter();

        size = signal.getBar().getHigh() - signal.getBar().getLow();
        if (signal.isBullish()) {
            entry = signal.getBar().getHigh() + (size * 0.1);
            exit = signal.getBar().getLow() - (size * 0.1);
        } else {
            entry = signal.getBar().getLow() - (size * 0.1);
            exit = signal.getBar().getHigh() + (size * 0.1);
        }
        setRR(signal.getRR());

        // kill date - for testing purposes
        if (rangeTf != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(signal.getBar().getTime()));
            cal.add(Calendar.DAY_OF_MONTH, rangeTf.getPipCount() * 7);
            killDate = cal.getTime();
        }
    }

    public void newBar(IBar bar) {
        if (mode == TradeMode.AWAITING && expireAfter <= 0) {
            mode = TradeMode.CLOSED;
        } else if (mode == TradeMode.AWAITING) {
            expireAfter--;
        } else if (mode == TradeMode.OPENED && closeAfter <= 0) {
            try {
                IOrder order = context.getEngine().getOrder(hash);
                if (order != null && order.getProfitLossInAccountCurrency() > 0) {
                    onTP();
                    System.out.println("Over 50 candles. Closing profitable trade");
                }
            } catch (JFException e) {
                e.printStackTrace();
            }
        } else if (mode == TradeMode.OPENED) {
            closeAfter--;
        }
    }

    public void newTick(ITick tick) {
        if (mode == TradeMode.AWAITING) {
            if (signal.isBullish() && tick.getBid() > this.entry || signal.isBearish() && tick.getBid() < this.entry) {
                try {
                    onTrade();
                } catch (JFException e) {
                    e.printStackTrace();
                }
            }
        } else if (mode == TradeMode.OPENED) {
            try {
                profit = context.getEngine().getOrder(hash).getProfitLossInPips();
            } catch (JFException e) {
                e.printStackTrace();
            }

            if ((signal.isBullish() && tick.getBid() < this.exit) || (signal.isBearish() && tick.getBid() > this.exit)) { // second was tick.getAsk()
                closeTrade();
            }
            if (this.tp > 0 && ((signal.isBullish() && tick.getBid() >= this.tp) || (signal.isBearish() && tick.getAsk() <= this.tp))) {
                closeTrade();
            }
        }
    }

    public void onTrade() throws JFException {
        openTime = new Date(context.getHistory().getLastTick(pair).getTime());

        IOrder order = null;
        if (reverseSignal) {
            if (signal.isBearish())
                order = context.getEngine().submitOrder(hash, pair, OrderCommand.BUY, getLot(pair, risk, Math.abs(this.entry.doubleValue() - this.exit.doubleValue())), context.getHistory().getLastTick(pair).getAsk(), 1, 0.0, 0.0, 0, hash);
            if (signal.isBullish())
                order = context.getEngine().submitOrder(hash, pair, OrderCommand.SELL, getLot(pair, risk, Math.abs(this.entry.doubleValue() - this.exit.doubleValue())), context.getHistory().getLastTick(pair).getBid(), 1, 0.0, 0.0, 0, hash);
        } else {
            if (signal.isBearish())
                order = context.getEngine().submitOrder(hash, pair, OrderCommand.SELL, getLot(pair, risk, Math.abs(this.entry.doubleValue() - this.exit.doubleValue())), context.getHistory().getLastTick(pair).getBid(), 1, 0.0, 0.0, 0, hash);
            if (signal.isBullish())
                order = context.getEngine().submitOrder(hash, pair, OrderCommand.BUY, getLot(pair, risk, Math.abs(this.entry.doubleValue() - this.exit.doubleValue())), context.getHistory().getLastTick(pair).getAsk(), 1, 0.0, 0.0, 0, hash);
        }

        order.waitForUpdate(State.OPENED, State.FILLED, State.CANCELED);

        if (order.getState() == State.CANCELED)
            mode = TradeMode.CLOSED;
        else
            mode = TradeMode.OPENED;
    }

    public void closeTrade() {
        try {
            IOrder order = context.getEngine().getOrder(hash);
            if (order != null) {
                order.close();
                order.waitForUpdate(IOrder.State.CLOSED);

                closeTime = new Date(context.getHistory().getTick(pair, 0).getTime());
            }
        } catch (JFException e) {
            e.printStackTrace();
        }

        mode = TradeMode.CLOSED;
    }

    public double getSpread() {
        try {
            return context.getHistory().getTick(pair, 0).getAsk() - context.getHistory().getTick(pair, 0).getBid();
        } catch (JFException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public void setRR(double _rr) {
        if (_rr == 0.5) {
            if (signal.isBullish()) {
                tp = signal.getBar().getHigh() + size;
            } else {
                tp = signal.getBar().getLow() - size;
            }

            return;
        } else {
            rr = _rr;
        }

        if (signal.isBullish()) {
            tp = entry + ((Math.abs(entry - exit) + getSpread()) * rr);
        } else {
            tp = entry - ((Math.abs(entry - exit) + getSpread()) * rr);
        }
    }

    @SuppressWarnings("unused")
    private void saveCSV(String csv) {
        try {
            FileWriter f = new FileWriter("C:\\experts\\files\\" + pair.name() + ".html");
            f.append(csv);
            f.append('\n');
            f.flush();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---

    public Instrument getInstrument() {
        return pair;
    }

    public PriceRange getTf() {
        return rangeTf;
    }

    public Period getPeriod() {
        return periodTf;
    }

    public String getHash() {
        return hash;
    }

    public TradeMode getMode() {
        return mode;
    }

    public Date getOpenTime() {
        return openTime;
    }

    public Date getCloseTime() {
        return closeTime;
    }

    public Double getEntry() {
        return entry;
    }

    public void setEntry(Double entry) {
        this.entry = entry;
    }

    public Double getExit() {
        return exit;
    }

    public void setExit(Double exit) {
        this.exit = exit;
    }

    public String getSettings(int x, String y) {
        double pip = 5;
        if (signal.getPriceRange() != null)
            pip = (double) signal.getPriceRange().getPipCount();

        String[] margin = new String[]{
                "0.0000",
                "" + (pip * 0.00002),
                "" + (pip * 0.00005),
                "" + (pip * 0.00010),
        };

        return margin[Integer.parseInt(y)];
    }

    public Date getKillDate() {
        return killDate;
    }

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

    // ---

    private void storeResults() {
        String key = "zz:" + pair.name() + ":" + settings[0] + ":" + settings[1] + ":" + settings[2];

        Double slMargin = Math.abs(entry - exit);
        Double rr = maxProfit.doubleValue() / (slMargin.doubleValue() * signal.getPoint());

        PriceRange tf = rangeTf;

        if (tf == null) return;

        System.out.println("maxProfit=" + maxProfit + " | slMargin=" + slMargin.toString() + " | rr=" + rr.toString());

        if (openTime == null || openSession == null || closeTime == null || closeSession == null) return;

        int hour = openTime.getDay();
        int year = openTime.getYear();
        int month = openTime.getMonth();

        ShardedJedis redis = redisPool.getResource();

        // all profits
        redis.rpush(key + ":profit", maxProfit.toString());
        redis.rpush(key + ":profit:TF=" + tf.getPipCount() + "P", maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month, maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month + ":TF=" + tf.getPipCount() + "P", maxProfit.toString());
        // all profits per hour
        redis.rpush(key + ":profit:HOUR=" + hour, maxProfit.toString());
        redis.rpush(key + ":profit:TF=" + tf.getPipCount() + "P:HOUR=" + hour, maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month + ":HOUR=" + hour, maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month + ":TF=" + tf.getPipCount() + "P:HOUR=" + hour, maxProfit.toString());
        // all profits per session
        redis.rpush(key + ":profit:SESSION=" + openSession, maxProfit.toString());
        redis.rpush(key + ":profit:TF=" + tf.getPipCount() + "P:SESSION=" + openSession, maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month + ":SESSION=" + openSession, maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month + ":TF=" + tf.getPipCount() + "P:SESSION=" + openSession, maxProfit.toString());
        // wszystkie zyski w zakresie sesji
        redis.rpush(key + ":profit:OPEN_SESSION=" + openSession + ":CLOSE_SESSION=" + closeSession, maxProfit.toString());
        redis.rpush(key + ":profit:TF=" + tf.getPipCount() + "P:OPEN_SESSION=" + openSession + ":CLOSE_SESSION=" + closeSession, maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month + ":OPEN_SESSION=" + openSession + ":CLOSE_SESSION=" + closeSession, maxProfit.toString());
        redis.rpush(key + ":profit:YEAR=" + year + ":MONTH=" + month + ":TF=" + tf.getPipCount() + "P:OPEN_SESSION=" + openSession + ":CLOSE_SESSION=" + closeSession, maxProfit.toString());

        redisPool.returnResource(redis);
    }
}
