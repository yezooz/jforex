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

package sr;

import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trade.TradeManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

public class Store {
    public static final String URL = "jdbc:mysql://localhost:3306/forex";
    public static final String USER = "root";
    public static final String PASSWORD = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(Store.class);

    public Store() {

    }

    public void saveMW(Instrument pair, String tf, String zz_1_id, String zz_2_id, Double high, Double low) {
        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

            String sql = String.format("INSERT INTO sr_mw (pair, tf, zz_1, zz_2, high, low, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                    pair.name(),
                    tf,
                    zz_1_id,
                    zz_2_id,
                    high,
                    low,
                    Helpers.formatDateTime(new Date())
            );

            st.execute(sql);
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }
    }

    public void saveBrokenMW(Instrument pair, Period tf, String dir, Date start_time, Date end_time, Double high, Double low) {
        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

            String sql = String.format("INSERT INTO sr_broken_level (pair, tf, dir, start_time, end_time, high, low, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                    pair.name(),
                    Helpers.tfToString(tf),
                    dir,
                    Helpers.formatDateTime(start_time),
                    Helpers.formatDateTime(end_time),
                    high,
                    low,
                    Helpers.formatDateTime(new Date())
            );

            st.execute(sql);
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }
    }

    public void saveMWInteraction(String zzId, String mwId, String dir, Date time, Double high, Double low) {
        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

            String sql = String.format("INSERT INTO sr_touch (zz_id, sr_id, dir, time, high, low, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                    zzId,
                    mwId,
                    dir,
                    Helpers.formatDateTime(time),
                    high,
                    low,
                    Helpers.formatDateTime(new Date())
            );

            LOGGER.debug(sql);

            st.execute(sql);
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }
    }

    // --- BARS

    public void saveAllBars(IContext context) throws JFException {
        LOGGER.info("--> " + new Date().toString());
        for (Instrument pair : TradeManager.defaultInstruments()) {
            for (Period period : TradeManager.defaultPeriods()) {
                LOGGER.info(pair.name() + " " + Helpers.tfToString(period));

                try {
                    saveAllBarForCombination(context, pair, period, 10000);
                } catch (Exception e) {
                    // Keep trying with lower limits
                    try {
                        LOGGER.warn("Try 10k");
                        saveAllBarForCombination(context, pair, period, 10000);
                    } catch (Exception e1) {
                        try {
                            LOGGER.warn("Try 1k");
                            saveAllBarForCombination(context, pair, period, 1000);
                        } catch (Exception e2) {
                            try {
                                LOGGER.warn("Try 500");
                                saveAllBarForCombination(context, pair, period, 500);
                            } catch (Exception e3) {
                                try {
                                    LOGGER.warn("Try 100");
                                    saveAllBarForCombination(context, pair, period, 100);
                                } catch (Exception e4) {
                                    e4.printStackTrace();
                                    LOGGER.error("FAILED");
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.info("<-- " + new Date().toString());
    }

    public void saveAllBarForCombination(IContext context, Instrument pair, Period period, int limit) throws Exception {
        IBar bar0 = context.getHistory().getBar(pair, period, OfferSide.BID, 0);

        List<IBar> bars = context.getHistory().getBars(pair, period, OfferSide.BID, Filter.ALL_FLATS, limit, bar0.getTime(), 0);

        double[][] macds = context.getIndicators().macd(pair, period, OfferSide.BID, AppliedPrice.CLOSE, 12, 26, 9, Filter.ALL_FLATS, limit, bar0.getTime(), 0);

        double zigzag[] = context.getIndicators().zigzag(pair, period, OfferSide.BID, 12, 5, 3, Filter.ALL_FLATS, limit, bar0.getTime(), 0);

        for (int i = 0; i < bars.size(); i++) {
            boolean isZZ = false;
            if (!Double.isNaN(zigzag[i])) {
                saveZZ(pair, period, (bars.get(i).getHigh() == zigzag[i] ? "RESISTANCE" : "SUPPORT"), bars.get(i), null);
                isZZ = true;
            }
            saveBar(pair, period, bars.get(i), macds[0][i], isZZ);
        }
    }

    public void saveBar(Instrument pair, Period tf, IBar bar, double macd, boolean isZZ) {

        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

            String sql = String.format("INSERT INTO bar (pair, tf, high, low, open, close, time, macd, is_zz, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', %s, '%s')",
                    pair.name(),
                    Helpers.tfToString(tf),
                    bar.getHigh(),
                    bar.getLow(),
                    bar.getOpen(),
                    bar.getClose(),
                    Helpers.formatDateTime(bar.getTime()),
                    macd,
                    (isZZ ? '1' : '0'),
                    Helpers.formatDateTime(new Date())
            );

            LOGGER.debug(sql);

            st.execute(sql);
        } catch (SQLException ex) {
            if (ex.getMessage().startsWith("Duplicate entry")) {
                return;
            }
            LOGGER.error("SQL Error: " + ex.getMessage());
        }
    }

    public void saveZZ(Instrument pair, Period tf, String dir, IBar bar, Date foundAt) {

        double shadow[] = null;

        if (dir.equals("SUPPORT")) {
            shadow = Helpers.getLowerShadow(bar);
        } else if (dir.equals("RESISTANCE")) {
            shadow = Helpers.getUpperShadow(bar);
        } else {
            LOGGER.warn("Unknown dir");
            System.exit(0);
        }

        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();

            String sql = String.format("INSERT INTO zz (pair, tf, dir, time, high, low, open, close, level_high, level_low, found_at, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', %s, '%s')",
                    pair.name(),
                    Helpers.tfToString(tf),
                    dir,
                    Helpers.formatDateTime(bar.getTime()),
                    bar.getHigh(),
                    bar.getLow(),
                    bar.getOpen(),
                    bar.getClose(),
                    shadow[0],
                    shadow[1],
                    (foundAt != null ? "'" + Helpers.formatDateTime(foundAt) + "'" : null),
                    Helpers.formatDateTime(new Date())
            );

            LOGGER.debug(sql);

            st.execute(sql);
        } catch (SQLException ex) {
            if (ex.getMessage().startsWith("Duplicate entry")) {
                return;
            }
            LOGGER.error("SQL Error: " + ex.getMessage());
        }
    }

    // ---

    public int runSQL(String sql) throws SQLException {
        LOGGER.debug(sql);

        Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();
        st.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);

        int id = 0;
        ResultSet rs = st.getGeneratedKeys();
        if (rs.next()) {
            id = rs.getInt(1);
        }
        rs.close();

        return id;
    }

}
