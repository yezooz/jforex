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
import com.dukascopy.api.IEngine.OrderCommand;
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Report;
import signal.Signal;
import trade.Trade;
import trade.TradeManager;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@RequiresFullAccess
public class TestStrategy extends CommonStrategy {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TestStrategy.class);

    protected Report report;

    protected List<IBar> bars;

    protected boolean saveSignalInDB = true;
    protected boolean manualConfirmation = false;

    protected String strategyPrefix = "";

    protected int tpCount = 0;
    protected int slCount = 0;

    public TestStrategy(IContext context, IChart chart) {
        super(context, chart);

        this.report = new Report();
    }

    public void onStop() {
        report.generateReport();

        LOGGER.error("TP\t" + tpCount);
        LOGGER.error("SL\t" + slCount);
        if (tpCount - slCount > 0)
            LOGGER.error("+" + (tpCount - slCount));
        else
            LOGGER.error("" + (tpCount - slCount));
        LOGGER.error("" + context.getAccount().getBalance());

        super.onStop();
    }

    public void onTP(IOrder order) {
        super.onTP(order);

        tpCount++;

        if (order.getComment() == null) return;

        String[] c = order.getComment().split("__");
        if (c.length == 4) {
            report.addTrade(strategyPrefix + c[0], c[1], c[2], order);
        }

        LOGGER.info("TP! $" + context.getAccount().getBalance());
    }

    public void onSL(IOrder order) {
        super.onSL(order);

        slCount++;

        if (order.getComment() == null) return;

        String[] c = order.getComment().split("__");
        if (c.length == 4) {
            report.addTrade(strategyPrefix + c[0], c[1], c[2], order);
        }

        LOGGER.info("SL! $" + context.getAccount().getBalance());
    }

    public void newSignal(Signal signal) {
        tradeList.add(new Trade(signal, context, TradeManager.getInstance().getSettingsForSignal(signal)));
        if (saveSignalInDB) {
            report.addSignal(signal);
        }
    }

    // --- Round Number

    public boolean matchRoundLevel(IBar bar, Instrument pair, int roundNumber) {
        int h = (int) (bar.getHigh() * Math.pow(10, pair.getPipScale() + 1));
        while (h >= (int) (bar.getLow() * Math.pow(10, pair.getPipScale() + 1))) {
            if (h % (roundNumber * 10) == 0) {
                return true;
            }

            h--;
        }

        return false;
    }

    public void drawRoundLevel(double level) {
        IBar bar;
        try {
            bar = context.getHistory().getBar(context.getLastActiveChart().getInstrument(), Period.FIVE_MINS, OfferSide.BID, 0);
        } catch (JFException e) {
            e.printStackTrace();
            return;
        }

        double multiplier = Math.pow(10, context.getLastActiveChart().getInstrument().getPipScale());
        int h = (int) (bar.getHigh() * multiplier);

        while (true) {
            if (h % level == 0) {

                double pip = context.getLastActiveChart().getInstrument().getPipValue();
                double draw = pip * 1;
                double v = h / multiplier;

                System.out.println("Drawing " + v);
                Helpers.drawRectangle(chart, bar.getTime(), v, tradeManager.getEndDate().getTime(), v + draw, Color.DARK_GRAY);

                for (int i = 1; i <= 100; i++) {
                    double vv = v + ((level * pip) * i);
                    if ((vv * multiplier) % (level * 10) == 0)
                        Helpers.drawRectangle(chart, bar.getTime(), vv, tradeManager.getEndDate().getTime(), vv + draw, Color.RED);
                    else
                        Helpers.drawRectangle(chart, bar.getTime(), vv, tradeManager.getEndDate().getTime(), vv + draw, Color.DARK_GRAY);
                }

                for (int i = 1; i <= 100; i++) {
                    double vv = v - ((level * pip) * i);
                    if ((vv * multiplier) % (level * 10) == 0)
                        Helpers.drawRectangle(chart, bar.getTime(), vv, tradeManager.getEndDate().getTime(), vv + draw, Color.RED);
                    else
                        Helpers.drawRectangle(chart, bar.getTime(), vv, tradeManager.getEndDate().getTime(), vv + draw, Color.DARK_GRAY);
                }

                break;

            }

            h--;
        }
    }

    public boolean waitForResponse() {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch ok = new CountDownLatch(1);

        KeyEventDispatcher dispatcher = new KeyEventDispatcher() {
            // Anonymous class invoked from EDT
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    latch.countDown();
                    ok.countDown();
                } else if (e.getKeyCode() == KeyEvent.VK_N) {
                    latch.countDown();
                }
                return false;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);

        return ok.getCount() == 1;
    }

    @SuppressWarnings("unused")
    protected void sendMarketOrder(OrderCommand direction) {
        String hash = String.format("%s__%s__%s__%s", "RENKO_PIN", pair.name(), "X" + range.getPipCount(), UUID.randomUUID().toString().replace('-', '0'));
        int xSL = 1;
        int xTP = 1;

        try {
            if (direction == OrderCommand.BUY) {
                IOrder order = context.getEngine().submitOrder(hash, pair, OrderCommand.BUY, 0.01);
                order.waitForUpdate(2000, IOrder.State.FILLED);
                // SL
                double slPrice = bar.getHigh() - (pair.getPipValue() * range.getPipCount() * xSL);
                order.setStopLossPrice(slPrice);
                // TP
                double tpPrice = bar.getHigh() + (pair.getPipValue() * range.getPipCount() * xTP);
                order.setTakeProfitPrice(tpPrice);
            } else {
                IOrder order = context.getEngine().submitOrder(hash, pair, OrderCommand.SELL, 0.01);
                order.waitForUpdate(2000, IOrder.State.FILLED);
                // SL
                double slPrice = bar.getLow() + (pair.getPipValue() * range.getPipCount() * xSL);
                order.setStopLossPrice(slPrice);
                // TP
                double tpPrice = bar.getLow() - (pair.getPipValue() * range.getPipCount() * xTP);
                order.setTakeProfitPrice(tpPrice);
            }
        } catch (JFException e) {
            e.printStackTrace();
        }
    }
}