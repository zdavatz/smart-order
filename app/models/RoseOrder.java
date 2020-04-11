/*
Copyright (c) 2016, ywesee GmbH, created by ML <cybrmx@gmail.com>

This file is part of AmikoRose.

AmikoRose is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by maxl on 14.07.2016.
 */
public class RoseOrder {

    public RoseOrder(String hash, String timestamp, String glncode, String customerId) {
        this.hash = hash;
        this.timestamp = timestamp;
        this.glncode = glncode;
        this.customerId = customerId;
    }

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("time")
    private String timestamp;

    @JsonProperty("glncode")
    private String glncode;

    @JsonIgnore
    private String customerId;

    @JsonProperty("order")
    private List<RoseArticle> list_of_rose_articles;

    @JsonProperty("special_group")
    private String specialGroup() {
        RoseData rd = RoseData.getInstance();
        if (rd.isMedixUser(this.customerId)) {
            return "medix";
        }
        return null;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    public void setGlncode(String glncode) {
        this.glncode = glncode;
    }

    public void setListArticles(List<RoseArticle> list_of_rose_articles) {
        this.list_of_rose_articles = list_of_rose_articles;
    }

    public String getGlncode() {
        return glncode;
    }

    @JsonIgnore
    public String getOrderCSV() {
        String line_separator = System.getProperty("line.separator");
        String order_csv = "";
        order_csv += timestamp + ";" + hash + ";" + glncode + ";" + line_separator;
        // List every article in the basket ("input")
        int index = 0;
        for (RoseArticle a : list_of_rose_articles) {
            order_csv += (++index) + ";" + articleStr(a) + ";" + line_separator;
            // Add first item on list of alternatives
            if (a.alternatives.size()>0) {
                RoseArticle alt = a.alternatives.get(0);
                order_csv += index + ";" + articleStr(alt) + ";" + line_separator;
            }
        }
        return order_csv;
    }

    @JsonIgnore
    private String articleStr(RoseArticle a) {
        String article_str = a.getGtin() + ";"
                + a.getTitle() + ";"
                + a.getTitleFR() + ";"
                + a.getSize() + ";"
                + a.getUnit() + ";"
                + a.getGalen() + ";"
                + a.getRoseBasisPrice() + ";"
                + a.getPublicPrice() + ";"
                + a.getQuantity() + ";"
                + a.getShippingStatus() + ";"
                + a.getPreferences();
        return article_str;
    }
}
