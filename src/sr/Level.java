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

public class Level {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Level.class);

    private Instrument pair;
    private Period tf;
    private String dir;
    private Double high;
    private Double low;

    private Date startDate;
    private Date endDate = null;
    private Date foundDate = null;
    private List<Touch> touchList;

    private int rowId = 0;

    public Level(Instrument _pair, Period _tf, String _dir, Date _startDate, double _high, double _low) {
        pair = _pair;
        tf = _tf;
        dir = _dir;

        startDate = _startDate;
        high = _high;
        low = _low;

        touchList = new ArrayList<Touch>();
    }

    public static Level loadById(int id) {
        try {
            String sql = String.format("SELECT pair, tf, dir, high, low, start_date, end_date, found_date FROM sr WHERE id = %s",
                    id
            );

            LOGGER.debug(sql);

            Statement s = (Statement) TradeManager.getInstance().getCon().createStatement();
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                String pair = rs.getString(1);
                String tf = rs.getString(2);
                String dir = rs.getString(3);
                double high = rs.getDouble(4);
                double low = rs.getDouble(5);
                Date startDate = rs.getTimestamp(6);
                Date endDate = rs.getTimestamp(7);
                Date foundDate = rs.getTimestamp(8);

                Level lvl = new Level(Instrument.fromString(String.format("%s/%s", pair.substring(0, 3), pair.substring(3))), Helpers.StringToTF(tf), dir, startDate, high, low);
                if (endDate != null) {
                    lvl.setEndDate(endDate);
                }
                if (foundDate != null) {
                    lvl.setFoundDate(foundDate);
                }
                lvl.setRowId(id);
                return lvl;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Instrument getPair() {
        return pair;
    }

    public Period getTF() {
        return tf;
    }

    public String getDir() {
        return dir;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getFoundDate() {
        return foundDate;
    }

    public void setFoundDate(Date date) {
        foundDate = date;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date date) {
        endDate = date;
    }

    public Double getLevelBoundry() {
        if (dir.equals(Type.SUPPORT)) {
            return low;
        }
        return high;
    }

    public Double getHigh() {
        return high;
    }

    public Double getLow() {
        return low;
    }

    public boolean isWithinLevel(Level level) {
        return Helpers.areBetweenValues(high, low, level.getHigh(), level.getLow());
    }

    public boolean isWithinLevel(double lvlHigh, double lvlLow) {
        return Helpers.areBetweenValues(high, low, lvlHigh, lvlLow);
    }

    public void addTouch(Touch touch) {
        touchList.add(touch);
    }

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int id) {
        rowId = id;
    }

    public int getNumTouches() {
        return touchList.size();
    }

    public void save() {
        Store store = new Store();

        try {
            rowId = store.runSQL(getSQL());
        } catch (SQLException e) {
            if (rowId != 0 && endDate != null) {
                try {
                    store.runSQL(getUpdateEndDateSQL());
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        }

        for (Touch t : touchList) {
            t.setSrId(rowId);
            t.save();
        }
    }

    public String getSQL() {
        return String.format("INSERT INTO sr (pair, tf, dir, high, low, start_date, end_date, found_date, created_at, zz_id) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %s, %s, '%s', '%s')",
                pair.name(),
                Helpers.tfToString(tf),
                dir,
                high,
                low,
                Helpers.formatDateTime(startDate),
                (endDate == null ? null : "'" + Helpers.formatDateTime(endDate) + "'"),
                (foundDate == null ? null : "'" + Helpers.formatDateTime(foundDate) + "'"),
                Helpers.formatDateTime(new Date()),
                rowId
        );
    }

    public String getUpdateEndDateSQL() {
        return String.format("UPDATE sr SET end_date = '%s' WHERE id = %s",
                Helpers.formatDateTime(endDate),
                rowId
        );
    }

}
