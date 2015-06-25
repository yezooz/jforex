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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LevelList {
    protected static final Logger LOGGER = LoggerFactory.getLogger(LevelList.class);

    protected List<Level> levels;
    protected boolean levelAdded = false;

    public LevelList() {
        levels = new ArrayList<Level>();
    }

    public List<Level> getList() {
        return levels;
    }

    public void addLevel(Level level) {
        for (Level l : levels) {
            if (l.getDir().equals(level.getDir()) && l.isWithinLevel(level)) {
                l.addTouch(Touch.fromLevel(level));
                levelAdded = false;
                return;
            }
        }

        if (level.getFoundDate() == null) {
            Level nextZZ = Generator.loadNextZZ(level.getPair(), level.getTF(), level.getStartDate());
            if (nextZZ != null && !level.getDir().equals(nextZZ.getDir())) {
                level.setFoundDate(nextZZ.getStartDate());
            }
        }

        levels.add(level);
        levelAdded = true;
    }

    public boolean hasAddedLevel() {
        return levelAdded;
    }

    public void save() {
        for (Level level : levels) {
            level.save();
        }
    }

    public void remove(Level level) {
        levels.remove(level);
    }
}
