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

import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;
import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import signal.PinBar;
import trade.TradeManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SR {
    protected static final Logger LOGGER = LoggerFactory.getLogger(SR.class);

    protected Instrument pair;
    protected Period tf;
    protected LevelList levels;

    public SR(Instrument _pair, Period _tf) {
        pair = _pair;
        tf = _tf;
        levels = new LevelList();
    }

    public void runGenerator() {
        Generator g = new Generator();
        g.runAll();
    }

    public void newBar(IBar bar, IBar bar0, boolean isNewZZ) throws JFException {

        Store store = new Store();
        store.saveBar(pair, tf, bar, 0.0, isNewZZ);

        try {
            String sql = String.format("SELECT id FROM sr WHERE pair = '%s' AND tf = '%s' AND start_date < '%s' AND end_date IS NULL AND ((dir = 'SUPPORT' AND low > '%s') OR dir = 'RESISTANCE' AND high < '%s')",
                    pair.name(),
                    Helpers.tfToString(tf),
                    Helpers.formatDateTime(bar0.getTime()),
                    bar.getClose(),
                    bar.getClose()
            );

            LOGGER.debug(sql);

            Statement s = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                int id = rs.getInt(1);

                Level level = Level.loadById(id);
                if ((level.getDir().equals(Type.SUPPORT) && bar.getClose() < level.getLevelBoundry()) || (level.getDir().equals(Type.RESISTANCE) && bar.getClose() > level.getLevelBoundry())) {
                    level.setEndDate(new Date(bar0.getTime()));
                    level.save();
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void newZZ(IBar bar, IBar zzBar, String dir) {
        double[] shadow = null;
        if (dir.equals(Type.RESISTANCE)) {
            shadow = Helpers.getUpperShadow(zzBar);
        } else {
            shadow = Helpers.getLowerShadow(zzBar);
        }

        try {
            String sql = String.format("SELECT COUNT(*) FROM sr WHERE pair = '%s' AND tf = '%s' AND dir = '%s' AND end_date IS NULL AND (high BETWEEN %s AND %s OR low BETWEEN %s AND %s OR %s BETWEEN high AND low OR %s BETWEEN high AND low)",
                    pair.name(),
                    Helpers.tfToString(tf),
                    dir,
                    shadow[0],
                    shadow[1],
                    shadow[0],
                    shadow[1],
                    shadow[0],
                    shadow[1]
            );

            LOGGER.debug(sql);

            Statement s = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                if (rs.getInt(1) == 0) {
                    Date foundAt = new Date(bar.getTime());

                    Level level = new Level(pair, tf, dir, new Date(zzBar.getTime()), shadow[0], shadow[1]);
                    level.setFoundDate(foundAt);
                    level.save();

                    Store store = new Store();
                    store.saveZZ(pair, tf, dir, zzBar, foundAt);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    public boolean inArea(PinBar pin) {
        return findLevels(pin).getList().size() > 0;
    }

    public List<Level> getLevels() {
        String sql = String.format("SELECT high, low, dir, start_date, found_date, end_date, tf FROM sr WHERE pair = '%s' AND tf IN (%s)", pair.name(), TradeManager.getValidTFs(Helpers.tfToString(tf)));

        LOGGER.debug(sql);

        List<Level> foundLevels = new ArrayList<Level>();

        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                double high = rs.getDouble(1);
                double low = rs.getDouble(2);
                String dir = rs.getString(3);
                Date startDate = rs.getTimestamp(4);
                Date foundDate = rs.getTimestamp(5);
                Date endDate = rs.getTimestamp(6);
                String rowTF = rs.getString(7);

                if (foundDate != null) {
                    startDate = foundDate;
                }

                Level lvl = new Level(pair, Helpers.StringToTF(rowTF), dir, startDate, high, low);
                if (endDate != null) {
                    lvl.setEndDate(endDate);
                }
                foundLevels.add(lvl);
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }

        return foundLevels;
    }

    public List<Level> getMWLevels() {
        String sql = String.format("SELECT high, low, dir, start_date, found_date, end_date, tf FROM sr_mw WHERE pair = '%s' AND tf IN (%s) ", pair.name(), TradeManager.getValidTFs(Helpers.tfToString(tf)));

        LOGGER.info(sql);

        List<Level> foundLevels = new ArrayList<Level>();

        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                double high = rs.getDouble(1);
                double low = rs.getDouble(2);
                String dir = rs.getString(3);
                Date startDate = rs.getTimestamp(4);
                Date foundDate = rs.getTimestamp(5);
                Date endDate = rs.getTimestamp(6);
                String rowTF = rs.getString(7);

                if (foundDate != null) {
                    startDate = foundDate;
                }

                Level lvl = new Level(pair, Helpers.StringToTF(rowTF), dir, startDate, high, low);
                if (endDate != null) {
                    lvl.setEndDate(endDate);
                }
                foundLevels.add(lvl);
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }

        return foundLevels;
    }

    public LevelList findLevels(PinBar pin) {

        String pin_date = Helpers.formatDateTime(pin.getBar().getTime());
        String startDateKey = "found_date";
        String sql = String.format("SELECT high, low, dir, %s, tf FROM sr_mw WHERE pair = '%s' AND tf IN (%s) AND %s IS NOT NULL AND ((end_date IS NOT NULL AND '%s' BETWEEN %s AND end_date) OR (end_date IS NULL AND %s <= '%s'))",
                startDateKey,
                pair.name(),
                TradeManager.getValidTFs(Helpers.tfToString(tf)),
                startDateKey,
                pin_date,
                startDateKey,
                startDateKey,
                pin_date);

        LOGGER.debug(sql);

        LevelList foundLevels = new LevelList();
        List<Period> foundTFs = new ArrayList<Period>();
        try {
            Statement st = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                double high = rs.getDouble(1);
                double low = rs.getDouble(2);
                String dir = rs.getString(3);
                Date startDate = rs.getTimestamp(4);

                if (dir.equals(Type.SUPPORT) && pin.isBullish()) {
                    double[] shadow = Helpers.getLowerShadow(pin.getBar());
                    if (Helpers.areBetweenValues(high, low, shadow[0], shadow[1])) {
                        foundLevels.addLevel(new Level(pair, tf, dir, startDate, high, low));
                        foundTFs.add(Helpers.StringToTF(rs.getString(5)));
                    }
                }
                if (dir.equals(Type.RESISTANCE) && pin.isBearish()) {
                    double[] shadow = Helpers.getUpperShadow(pin.getBar());
                    if (Helpers.areBetweenValues(high, low, shadow[0], shadow[1])) {
                        foundLevels.addLevel(new Level(pair, tf, dir, startDate, high, low));
                        foundTFs.add(Helpers.StringToTF(rs.getString(5)));
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("SQL Error: " + ex.getMessage());
        }

        if (foundTFs.size() == 1) {
            return foundLevels;
        }

        return foundLevels;
    }
}
