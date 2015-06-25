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

package server;

import com.dukascopy.api.IOrder;
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import signal.Signal;
import trade.TradeManager;

import java.sql.SQLException;
import java.sql.Statement;

public class Report {
    private static final Logger LOGGER = LoggerFactory.getLogger(Report.class);

    protected Map<String, Integer> tp;
    protected Map<String, Integer> sl;
    protected List<String> orderDates;

    public Report() {
        redisPool = pool;
        tp = new HashMap<String, Integer>();
        sl = new HashMap<String, Integer>();
        orderDates = new ArrayList<String>();
    }

    protected String getKey(String strategy, String pair, String period) {
        return String.format("%s__%s__%s", strategy, pair, period);
    }

    public void addTrade(String strategy, String pair, String priceRange, IOrder order) {
        String key = getKey(strategy, pair, priceRange);
        if (order.getProfitLossInUSD() >= 0) {
            if (tp.containsKey(key))
                tp.put(key, tp.get(key) + 1);
            else
                tp.put(key, 1);

            orderDates.add(Helpers.formatDateTime(order.getCreationTime()) + " (" + pair + " / " + priceRange + ") (PROFIT)");
        } else {
            if (sl.containsKey(key))
                sl.put(key, sl.get(key) + 1);
            else
                sl.put(key, 1);

            orderDates.add(Helpers.formatDateTime(order.getCreationTime()) + " (" + pair + " / " + priceRange + ") (LOSS)");
        }

        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

            String rr = null;
            if (strategy.contains("_RR")) {
                if (strategy.contains("_RR05"))
                    rr = "0.5";
                else
                    rr = strategy.substring(strategy.indexOf("_RR") + 3);
                strategy = strategy.substring(0, strategy.indexOf("_RR"));
            }

            String sql = String.format("INSERT INTO trade (strategy, pair, period, date, pips, RR, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', %s, '%s')",
                    strategy,
                    pair,
                    priceRange,
                    Helpers.formatDateTime(order.getCreationTime()),
                    order.getProfitLossInPips(),
                    (rr != null ? "'" + rr + "'" : null),
                    Helpers.formatDateTime(new Date())
            );

            LOGGER.debug(sql);

            st.executeUpdate(sql);
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }
    }

    public void addSignal(Signal signal) {
        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

            String sql = String.format("INSERT INTO `signal` (strategy, pair, period, dir, entry, tp, sl, date, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                    signal.getStrategy(),
                    signal.getInstrument().name(),
                    Helpers.tfToString(signal.getPeriod()),
                    (signal.isBullish() ? "BUY" : "SELL"),
                    signal.getEntry(),
                    signal.getTP(),
                    signal.getSL(),
                    Helpers.formatDateTime(signal.getBar().getTime()),
                    Helpers.formatDateTime(new Date())
            );

            LOGGER.info(sql);

            st.executeUpdate(sql);
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }
    }

    public void generateReport() {
        List<String> strategies = new ArrayList<String>();
        List<String> pairs = new ArrayList<String>();
        List<String> tfs = new ArrayList<String>();

        for (String k : tp.keySet()) {
            String[] key = k.split("__");

            if (!strategies.contains(key[0]))
                strategies.add(key[0]);
            if (!pairs.contains(key[1]))
                pairs.add(key[1]);
            if (!tfs.contains(key[2]))
                tfs.add(key[2]);
        }

        for (String k : sl.keySet()) {
            String[] key = k.split("__");

            if (!strategies.contains(key[0]))
                strategies.add(key[0]);
            if (!pairs.contains(key[1]))
                pairs.add(key[1]);
            if (!tfs.contains(key[2]))
                tfs.add(key[2]);
        }

        for (String str : orderDates) {
            LOGGER.warn(str);
        }

        File f = new File("c:\\Users\\marek\\Dropbox\\Forex\\Reports\\report_" + sdf_report.format(new Date()) + ".csv");
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(f), 32768);
            out.write(String.format("%s - %s\n", sdf.format(TradeManager.getInstance().getStartDate()), sdf.format(TradeManager.getInstance().getEndDate())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO miesiaca i lata

        // generate report
        for (String strategy : strategies) {
            LOGGER.warn("================================================================================================================");
            LOGGER.warn(strategy);

            Collections.sort(pairs);
            Collections.sort(tfs);

            for (String pair : pairs) {
                for (String tf : tfs) {

                    String key = getKey(strategy, pair, tf);
                    Integer tp_value = 0;
                    if (tp.containsKey(key))
                        tp_value = tp.get(key);

                    Integer sl_value = 0;
                    if (sl.containsKey(key))
                        sl_value = sl.get(key);

                    if (tp_value.intValue() == 0 && sl_value.intValue() == 0)
                        continue;

                    String diff = " 0";
                    if (tp_value > sl_value)
                        diff = String.format("+%d", tp_value.intValue() - sl_value.intValue());
                    else if (tp_value < sl_value)
                        diff = String.format("%d", tp_value.intValue() - sl_value.intValue());

                    LOGGER.warn(String.format("%s / %s => %s (%d/%d)", pair, tf, diff, tp_value.intValue(), sl_value.intValue()));

                    try {
                        out.write(String.format("%s,%s,%s,%d,%d,%d\n", strategy, pair, tf, (tp_value.intValue() - sl_value.intValue()), tp_value.intValue(), sl_value.intValue()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // MySQL
                    if (TradeManager.getInstance().getArgs() != null && TradeManager.getInstance().getArgs().length > 0) // only if running from console
                    {
                        try {
                            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

                            String sql = String.format("INSERT INTO report (strategy, pair, period, date_from, date_to, tp, sl, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', %d, %d, '%s')",
                                    strategy,
                                    pair,
                                    tf,
                                    Helpers.formatDate(TradeManager.getInstance().getStartDate()),
                                    Helpers.formatDate(TradeManager.getInstance().getEndDate()),
                                    tp_value.intValue(),
                                    sl_value.intValue(),
                                    Helpers.formatDateTime(new Date())
                            );

                            LOGGER.debug(sql);

                            st.executeUpdate(sql);
                        } catch (SQLException ex) {
                            LOGGER.error("SQL Error: " + ex.getMessage());
                        }
                    }
                }
            }
        }

        LOGGER.warn("================================================================================================================");
    }
}
