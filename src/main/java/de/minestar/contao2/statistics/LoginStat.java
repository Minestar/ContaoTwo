/*
 * Copyright (C) 2012 MineStar.de 
 * 
 * This file is part of Contao2.
 * 
 * Contao2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * Contao2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Contao2.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.minestar.contao2.statistics;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

import de.minestar.contao2.core.Core;
import de.minestar.minestarlibrary.stats.Statistic;
import de.minestar.minestarlibrary.stats.StatisticType;

public class LoginStat implements Statistic {

    private String player;
    private String group;
    private boolean successFul;
    private Timestamp date;

    public LoginStat() {
        // EMPTY CONSTRUCTOR FOR REFLECTION ACCESS
    }

    public LoginStat(String player, String group, boolean successFul) {
        this.player = player;
        this.group = group;
        this.successFul = successFul;
        this.date = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public String getPluginName() {
        return Core.NAME;
    }

    @Override
    public String getName() {
        return "Login";
    }

    @Override
    public LinkedHashMap<String, StatisticType> getHead() {

        LinkedHashMap<String, StatisticType> head = new LinkedHashMap<String, StatisticType>();

        head.put("player", StatisticType.STRING);
        head.put("group", StatisticType.STRING);
        head.put("successFul", StatisticType.INT);
        head.put("date", StatisticType.DATETIME);

        return head;
    }

    @Override
    public Queue<Object> getData() {

        Queue<Object> data = new LinkedList<Object>();

        data.add(player);
        data.add(group);
        data.add(successFul ? 1 : 0);
        data.add(date);

        return data;
    }
}
