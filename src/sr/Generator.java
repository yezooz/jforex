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

import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trade.TradeManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Generator {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    public Generator() {

    }

    public static Level loadZZ(Instrument pair, Period tf, Date time) {
        try {
            String sql = String.format("SELECT dir, level_high, level_low, id FROM zz WHERE pair = '%s' AND tf = '%s' AND time = '%s'",
                    pair.name(),
                    Helpers.tfToString(tf),
                    Helpers.formatDateTime(time)
            );

            LOGGER.debug(sql);

            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String dir = rs.getString(1);
                double levelHigh = rs.getDouble(2);
                double levelLow = rs.getDouble(3);
                int rowId = rs.getInt(4);

                Level lvl = new Level(pair, tf, dir, time, levelHigh, levelLow);
                lvl.setRowId(rowId);
                return lvl;
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }

        return null;
    }

    public static Level loadNextZZ(Instrument pair, Period tf, Date time) {
        try {
            String sql = String.format("SELECT dir, level_high, level_low, time FROM zz WHERE pair = '%s' AND tf = '%s' AND time > '%s' ORDER BY time LIMIT 1",
                    pair.name(),
                    Helpers.tfToString(tf),
                    Helpers.formatDateTime(time)
            );

            LOGGER.debug(sql);

            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String dir = rs.getString(1);
                double levelHigh = rs.getDouble(2);
                double levelLow = rs.getDouble(3);
                time = rs.getTimestamp(4);

                return new Level(pair, tf, dir, time, levelHigh, levelLow);
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }

        return null;
    }

    public void runAll() {
        for (Instrument pair : TradeManager.defaultInstruments()) {
            for (Period tf : TradeManager.defaultPeriods()) {
                runPairTF(pair, tf);
            }
        }
        System.exit(0);
    }

    public void runPairTF(Instrument pair, Period tf) {
        LevelList openLevels = new LevelList();
        List<Level> levelsToClose = new ArrayList<Level>();

        LOGGER.info("Running " + pair.name() + "/" + Helpers.tfToString(tf));

        try {
            String sql = String.format("SELECT close, is_zz, time FROM bar WHERE pair = '%s' AND tf = '%s' ORDER BY time",
                    pair.name(),
                    Helpers.tfToString(tf)
            );

            LOGGER.debug(sql);

            Statement s = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                double close = rs.getDouble(1);
                boolean isZZ = rs.getBoolean(2);
                Date time = rs.getTimestamp(3);

                for (Level level : openLevels.getList()) {
                    if (level.getDir().equals(Type.SUPPORT) && close < level.getLevelBoundry()) {
                        levelsToClose.add(level);
                    } else if (level.getDir().equals(Type.RESISTANCE) && close > level.getLevelBoundry()) {
                        levelsToClose.add(level);
                    }
                }

                if (isZZ) {
                    Level zz = loadZZ(pair, tf, time);
                    if (zz == null) {
                        LOGGER.error("Cannot find ZZ for " + time.toString());
                        continue;
                    }
                    openLevels.addLevel(zz);
                }

                for (Level level : levelsToClose) {
                    level.setEndDate(time);
                    level.save();
                    openLevels.remove(level);
                }
                levelsToClose = new ArrayList<Level>();
            }

            openLevels.save();

            generateMW(pair, tf);
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error(ex.getMessage());
        }
    }

    private void generateMW(Instrument pair, Period tf) {
        try {
            String sql = String.format("SELECT sr.id, COUNT(*) AS levels FROM sr JOIN sr_touch AS touch ON touch.sr_id = sr.id WHERE pair = '%s' AND tf = '%s' GROUP BY sr.id HAVING levels > 0",
                    pair.name(),
                    Helpers.tfToString(tf)
            );

            LOGGER.debug(sql);

            Statement s = (Statement) TradeManager.getInstance().getCon().createStatement();

            // Get list of ids of levels with at least one peak (MW)
            ResultSet rs = s.executeQuery(sql);

            List<Integer> srIds = new ArrayList<Integer>();
            while (rs.next()) {
                srIds.add(rs.getInt(1));
            }

            // Get MW's second leg
            for (Integer sr_id : srIds) {
                sql = String.format("SELECT zz.time, sr_touch.id FROM sr_touch LEFT JOIN zz ON zz.id = sr_touch.zz_id WHERE sr_touch.sr_id = '%s' ORDER BY sr_touch.time LIMIT 1", sr_id);
                LOGGER.debug(sql);
                rs = s.executeQuery(sql);

                Date zz2_time = null;
                int touch_id = 0;
                while (rs.next()) {
                    zz2_time = rs.getTimestamp(1);
                    touch_id = rs.getInt(2);
                }

                Level sr1 = Level.loadById(sr_id);
                Level sr2 = loadNextZZ(pair, tf, zz2_time);

                if (sr2 == null) {
                    continue;
                }

                sql = String.format("INSERT INTO sr_mw VALUES (NULL, '%s', '%s', '%s', '%s', '%s', '%s', '%s', %s, '%s', '%s', '%s')",
                        sr1.getPair().name(),
                        Helpers.tfToString(sr1.getTF()),
                        sr1.getDir(),
                        sr1.getHigh(),
                        sr1.getLow(),
                        Helpers.formatDateTime(sr1.getStartDate()),
                        Helpers.formatDateTime(sr2.getStartDate()),
                        (sr1.getEndDate() == null ? null : "'" + Helpers.formatDateTime(sr1.getEndDate()) + "'"),
                        sr_id,
                        touch_id,
                        Helpers.formatDateTime(new Date())
                );

                LOGGER.debug(sql);

                s.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error(ex.getMessage());
        }
    }
}
