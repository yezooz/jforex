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
import helper.Helpers;
import helper.SendEmail;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;
import signal.Signal;
import sr.Store;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;

public class TradeManager {
    private static TradeManager instance = null;
    public ShardedJedisPool redisPool;
    private IEngine engine;
    private IAccount account;
    @SuppressWarnings("unused")
    private IContext context;
    private double balance;
    private DecimalFormat df = new DecimalFormat("0.00000");
    private Connection con = null;
    private Set<Instrument> instruments;
    private Set<PriceRange> priceRanges;
    private Set<PriceRange> renkoSizes;
    private Set<Period> periods;
    private String[] args;
    private Date startDate = defaultStartDate();
    private Date endDate = defaultEndDate();
    private double risk = 0.02;
    private boolean send_email = false;

    public TradeManager(IContext context) {
        this.context = context;
        this.engine = context.getEngine();
        this.account = context.getAccount();

        this.balance = this.account.getBalance();

        this.instruments = defaultInstruments();
        this.priceRanges = defaultPriceRanges();
        this.renkoSizes = defaultRenkoRanges();
        this.periods = defaultPeriods();
        this.args = null;

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        JedisShardInfo si = new JedisShardInfo("127.0.0.1", 6379);
        si.setTimeout(0);
        shards.add(si);

        redisPool = null; //new ShardedJedisPool(new Config(), shards);
    }

    public static TradeManager getInstance(IContext context) {
        if (instance == null) {
            instance = new TradeManager(context);
        }
        return instance;
    }

    public static TradeManager getInstance() {
        return instance;
    }

    public static Set<Instrument> defaultInstruments() {
        Instrument[] pairs = {
                Instrument.EURUSD,
                Instrument.GBPUSD,
                Instrument.USDCHF,
                Instrument.AUDUSD,
                Instrument.EURGBP,
                Instrument.NZDUSD,
                Instrument.EURJPY,
                Instrument.USDJPY,
                Instrument.USDCAD,
                Instrument.GBPJPY,
                Instrument.GBPAUD,

                Instrument.EURCAD,
                Instrument.AUDCAD,
                Instrument.CADJPY,
                Instrument.CHFJPY,
                Instrument.EURAUD,
                Instrument.GBPCAD,
                Instrument.GBPCHF,
                Instrument.NZDCHF,
                Instrument.AUDJPY,
                Instrument.GBPNZD,
                Instrument.AUDNZD,
                Instrument.NZDCAD,
                Instrument.EURNZD,
                Instrument.CADCHF,
                Instrument.NZDJPY,
                Instrument.AUDCHF,

                Instrument.XAGUSD,
                Instrument.XAUUSD,
                Instrument.EURPLN,
                Instrument.USDPLN,
                Instrument.EURNOK,
                Instrument.SGDJPY,
                Instrument.USDDKK,
                Instrument.USDNOK,
                Instrument.USDTRY,
                Instrument.AUDSGD,
                Instrument.USDSEK,
                Instrument.USDSGD,
                Instrument.USDZAR,
                Instrument.USDMXN,
                Instrument.USDHKD,
                Instrument.HKDJPY,
                Instrument.EURSEK,
                Instrument.EURSGD,
                Instrument.EURTRY,
                Instrument.EURHKD,
                Instrument.EURDKK,
                Instrument.CHFSGD,
                Instrument.CADHKD,
        };

        Set<Instrument> allPairs = new HashSet<Instrument>();
        for (Instrument pair : pairs) {
            allPairs.add(pair);
        }

        return allPairs;
    }

    public static Set<Period> defaultPeriods() {
        Period[] periods = {
                Period.createCustomPeriod(Unit.Minute, 5),
                Period.createCustomPeriod(Unit.Minute, 6),
                Period.createCustomPeriod(Unit.Minute, 8),
                Period.createCustomPeriod(Unit.Minute, 10),
                Period.createCustomPeriod(Unit.Minute, 12),
                Period.createCustomPeriod(Unit.Minute, 15),
                Period.createCustomPeriod(Unit.Minute, 16),
                Period.createCustomPeriod(Unit.Minute, 18),
                Period.createCustomPeriod(Unit.Minute, 20),
                Period.createCustomPeriod(Unit.Minute, 24),
                Period.createCustomPeriod(Unit.Minute, 30),
                Period.createCustomPeriod(Unit.Minute, 32),
                Period.createCustomPeriod(Unit.Minute, 36),
                Period.createCustomPeriod(Unit.Minute, 40),
                Period.createCustomPeriod(Unit.Minute, 45),
                Period.createCustomPeriod(Unit.Minute, 48),
                Period.createCustomPeriod(Unit.Hour, 1),
                Period.createCustomPeriod(Unit.Hour, 2),
                Period.createCustomPeriod(Unit.Hour, 3),
                Period.createCustomPeriod(Unit.Hour, 4),
                Period.createCustomPeriod(Unit.Hour, 6),
                Period.createCustomPeriod(Unit.Hour, 8),
                Period.createCustomPeriod(Unit.Hour, 12),
                Period.createCustomPeriod(Unit.Day, 1),
                Period.createCustomPeriod(Unit.Day, 2),
                Period.createCustomPeriod(Unit.Day, 3),
                Period.createCustomPeriod(Unit.Day, 4),
                Period.createCustomPeriod(Unit.Day, 5),
                Period.createCustomPeriod(Unit.Day, 6),
                Period.createCustomPeriod(Unit.Week, 1),
        };

        Set<Period> allPeriods = new HashSet<Period>();
        for (Period p : periods) {
            allPeriods.add(p);
        }

        return allPeriods;
    }

    public static Date defaultStartDate() {
        return Helpers.dateFromString("2013-01-01");
    }

    public static Date defaultEndDate() {
        return Helpers.dateFromString("2014-01-01");
    }

    public static String getValidTFs(String tf) {
        String[] tfs = null;

        if (tf.equals("M5")) {
            tfs = new String[]{"H1"};
        } else if (tf.equals("M6")) {
            tfs = new String[]{"H1"};
        } else if (tf.equals("M8")) {
            tfs = new String[]{"H1"};
        } else if (tf.equals("M10")) {
            tfs = new String[]{"H1"};
        } else if (tf.equals("M12")) {
            tfs = new String[]{"H1"};
        } else if (tf.equals("M15")) {
            tfs = new String[]{"H1"};
        } else if (tf.equals("M16")) {
            tfs = new String[]{"H1", "H2"};
        } else if (tf.equals("M18")) {
            tfs = new String[]{"H1", "H2"};
        } else if (tf.equals("M20")) {
            tfs = new String[]{"H1", "H2"};
        } else if (tf.equals("M24")) {
            tfs = new String[]{"H1", "H2"};
        } else if (tf.equals("M30")) {
            tfs = new String[]{"H1", "H2", "H3", "H4"};
        } else if (tf.equals("M32")) {
            tfs = new String[]{"H2", "H3", "H4"};
        } else if (tf.equals("M36")) {
            tfs = new String[]{"H2", "H3", "H4"};
        } else if (tf.equals("M40")) {
            tfs = new String[]{"H2", "H3", "H4"};
        } else if (tf.equals("M45")) {
            tfs = new String[]{"H2", "H3", "H4"};
        } else if (tf.equals("M48")) {
            tfs = new String[]{"H2", "H3", "H4"};
        } else if (tf.equals("H1")) {
            tfs = new String[]{"H4"};
        } else if (tf.equals("H2")) {
            tfs = new String[]{"H4", "H6", "H8", "H12"};
        } else if (tf.equals("H3")) {
            tfs = new String[]{"H6", "H8", "H12"};
        } else if (tf.equals("H4")) {
            tfs = new String[]{"D1"};
        } else if (tf.equals("H6")) {
            tfs = new String[]{"H12", "D1", "D2", "D3", "D4", "D5", "D6", "W1"};
        } else if (tf.equals("H8")) {
            tfs = new String[]{"D1", "D2", "D3", "D4", "D5", "D6", "W1"};
        } else if (tf.equals("H12")) {
            tfs = new String[]{"D1", "D2", "D3", "D4", "D5", "D6", "W1"};
        } else if (tf.equals("D1")) {
            tfs = new String[]{"W1"};
        } else if (tf.equals("D2")) {
            tfs = new String[]{"D5", "D6", "W1"};
        } else if (tf.equals("D3")) {
            tfs = new String[]{"D6", "W1"};
        } else if (tf.equals("D4")) {
            tfs = new String[]{"W1"};
        } else if (tf.equals("D5")) {
            tfs = new String[]{"W1"};
        } else if (tf.equals("D6")) {
            tfs = new String[]{"W1"};
        }

        // Overwrite previous values
        tfs = new String[]{tf};

        return "'" + StringUtils.join(tfs, "','") + "'";
    }

    public static Set<PriceRange> defaultPriceRanges() {
//		int[] periods = { 10,20,30,40,50,60,70,80,90,100 };
        int[] periods = {};

        Set<PriceRange> allPeriods = new HashSet<PriceRange>();
        for (int p : periods) {
            allPeriods.add(PriceRange.valueOf(p));
        }

        return allPeriods;
    }

    // RENKO
    public static Set<PriceRange> defaultRenkoRanges() {
//		int[] periods = { 20,30,40,50,60,70,80,90,100 };
//		int[] periods = { 10,11,12,13,14,15,16,17,18,19,20 };
        int[] periods = {};

        Set<PriceRange> allPeriods = new HashSet<PriceRange>();
        for (int p : periods) {
            allPeriods.add(PriceRange.valueOf(p));
        }

        return allPeriods;
    }

    public static Instrument[] allInstruments() {
        Instrument[] instruments = {
                Instrument.EURUSD,
                Instrument.AUDUSD,
                Instrument.EURGBP,
                Instrument.GBPUSD,
                Instrument.NZDUSD,
                Instrument.USDCAD,
                Instrument.USDCHF,
                Instrument.EURJPY,
                Instrument.USDJPY,

                Instrument.EURCAD,
                Instrument.AUDCAD,
                Instrument.CADCHF,
                Instrument.CADJPY,
                Instrument.CHFJPY,
                Instrument.EURAUD,
                Instrument.EURNZD,
                Instrument.GBPCAD,
                Instrument.GBPCHF,
                Instrument.GBPJPY,
                Instrument.GBPNZD,
                Instrument.NZDCAD,
                Instrument.NZDCHF,
                Instrument.NZDJPY,
                Instrument.AUDJPY,
                Instrument.AUDCHF,
                Instrument.AUDNZD,
                Instrument.XAGUSD,
                Instrument.XAUUSD,

                Instrument.EURPLN,
                Instrument.USDPLN,
                Instrument.EURNOK,
                Instrument.SGDJPY,
                Instrument.USDDKK,
                Instrument.USDNOK,
                Instrument.USDTRY,
                Instrument.AUDSGD,
                Instrument.USDSEK,
                Instrument.USDSGD,
                Instrument.USDZAR,
                Instrument.USDMXN,
                Instrument.USDHKD,
                Instrument.HKDJPY,
                Instrument.GBPAUD,
                Instrument.EURSEK,
                Instrument.EURSGD,
                Instrument.EURTRY,
                Instrument.EURHKD,
                Instrument.EURDKK,
                Instrument.CHFSGD,
                Instrument.CADHKD,
        };

        return instruments;
    }

    public Connection getCon() {
        if (con == null) {
            try {
                con = DriverManager.getConnection(Store.URL, Store.USER, Store.PASSWORD);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return con;
    }

    public Set<Instrument> playableInstruments() {
        return instruments;
    }

    public Set<PriceRange> playableRangeBarPeriods() {
        return priceRanges;
    }

    public Set<PriceRange> playableRenkoBarPeriods() {
        return renkoSizes;
    }

    public Set<Period> playablePeriods() {
        return periods;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date date) {
        startDate = date;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date date) {
        endDate = date;
    }

    public void setInstruments(Set<Instrument> instruments) {
        this.instruments = instruments;
    }

    public void setPriceRange(Set<PriceRange> priceRanges) {
        this.priceRanges = priceRanges;
    }

    public void setRenkoRange(Set<PriceRange> renkoRanges) {
        this.renkoSizes = renkoRanges;
    }

    public void setPeriods(Set<Period> periods) {
        this.periods = periods;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public double getRisk() {
        return risk;
    }

    public boolean playableInstrumentAndPriceRangeCombination(Instrument pair, PriceRange period) {

//		if ((pair == Instrument.EURPLN || pair == Instrument.USDPLN) && period < 20)
//			return false;

//		EUR/PLN (20)
//		USD/PLN (20)
//		EUR/NOK (30)
//		SGD/JPY (10)
//		USD/DKK (30)
//		USD/NOK (40)
//		USD/TRY (10)
//		AUD/SGD (10)
//		USD/SEK (50)
//		USD/ZAR (80)
//		USD/MXN (90)
//		HKD/JPY (70)
//		GBP/AUD (10)
//		EUR/SEK (40)
//		EUR/SGD (10)
//		EUR/TRY (20)
//		EUR/HKD (60)
//		CHF/SGD (10)
//		CAD/HKD (30)

        return true;
    }

    public double getTargetProfit(Instrument pair, int period) {
        return period * 3;
    }

    public Object[] getZZSettings() {
        return new Object[]{12, 5, 3};
    }

    public Double[] getSettingsForSignal(Signal signal) {
        return new Double[]{3.0, 0.1, 0.1};
    }

    // Helpers

    protected String getLabel(Instrument instrument) {
        String label = instrument.name() + "_" + UUID.randomUUID().toString().replace('-', '0');
        return label;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void onStop() {

        try {
            closeAllOrders(false);
        } catch (JFException e) {
            e.printStackTrace();
        }
    }

    public void onOpen(IOrder order) {
        if (send_email)
            SendEmail.send("Order opened, " + order.getInstrument().toString(), order.toString() + ". " + order.getComment() + ". " + "ENTRY=" + df.format(order.getOpenPrice()) + ", TP=" + df.format(order.getTakeProfitPrice()) + ", SL=" + df.format(order.getStopLossPrice()));
    }

    public void onFill(IOrder order) {
        if (send_email)
            SendEmail.send("Order filled, " + order.getInstrument().toString(), order.toString() + ". " + order.getComment() + ". " + "ENTRY=" + df.format(order.getOpenPrice()) + ", TP=" + df.format(order.getTakeProfitPrice()) + ", SL=" + df.format(order.getStopLossPrice()));
    }

    public void onClose(IOrder order) {
        if (send_email)
            SendEmail.send("SL - " + order.getInstrument().toString() + " | -$" + order.getProfitLossInUSD() * -1, "");
    }

    public void onSubmitFail(IOrder order) {
        if (send_email)
            SendEmail.send("Failed to submit order", order.toString());
    }

    public void onFillFail(IOrder order) {
        if (send_email)
            SendEmail.send("Failed to fill order", order.toString());
    }

    public void onTP(IOrder order) {
        System.out.println("+$" + order.getProfitLossInUSD());
        if (send_email)
            SendEmail.send("TP - " + order.getInstrument().toString() + ", +$" + order.getProfitLossInUSD(), order.toString());
    }

    public void onSL(IOrder order) {
        System.out.println("-$" + (order.getProfitLossInUSD() * -1));
        if (send_email)
            SendEmail.send("SL - " + order.getInstrument().toString() + ", -$" + order.getProfitLossInUSD() * -1, order.toString());
    }

    // helpers

    private void closeAllOrders(boolean onlyPending) throws JFException {

        for (IOrder order : engine.getOrders()) {
            switch (order.getOrderCommand()) {
                case BUY:
                case SELL:
                    if (!onlyPending) {
                        order.close();
                        order.waitForUpdate(IOrder.State.CLOSED);
                    }
                    break;
                case BUYSTOP_BYBID:
                case SELLSTOP:
                    order.close();
                    order.waitForUpdate(IOrder.State.CANCELED);

                    SendEmail.send("Closed Pending Order Before Weekend", "LOT=" + order.getAmount() + ", ENTRY=" + order.getOpenPrice() + ", TP=" + order.getTakeProfitPrice() + ", SL=" + order.getStopLossPrice() + ". " + order.toString());

                    break;
                default:
                    System.out.println(order.getLabel() + " is a " + (order.isLong() ? "LONG" : "SHORT") + " " + order.getOrderCommand() + " conditional order");
            }
        }
    }
}
