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

import helper.Helpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Date;

public class Touch {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Touch.class);

    private String dir;
    private Double high;
    private Double low;
    private Date date;
    private int sr_id = 0;
    private int zz_id = 0;

    public Touch(String _dir, Date _date, double _high, double _low, int _zz_id) {
        dir = _dir;
        date = _date;
        high = _high;
        low = _low;
        zz_id = _zz_id;
    }

    public static Touch fromLevel(Level level) {
        return new Touch(level.getDir(), level.getStartDate(), level.getHigh(), level.getLow(), level.getRowId());
    }

    public String getSQL() {
        if (sr_id == 0) {
            LOGGER.error("sr_id cannot be null!");
        }

        return String.format("INSERT INTO sr_touch (zz_id, sr_id, dir, high, low, time, created_at) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                zz_id,
                sr_id,
                dir,
                high,
                low,
                Helpers.formatDateTime(date),
                Helpers.formatDateTime(new Date())
        );
    }

    public void save() {
        Store store = new Store();
        try {
            store.runSQL(getSQL());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setSrId(int _sr_id) {
        sr_id = _sr_id;
    }
}
