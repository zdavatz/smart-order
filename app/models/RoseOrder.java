/*
Copyright (c) 2016 ML <cybrmx@gmail.com>

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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by maxl on 14.07.2016.
 */
public class RoseOrder {

    public RoseOrder(String hash, String timestamp, String glncode) {
        this.hash = hash;
        this.timestamp = timestamp;
        this.glncode = glncode;
    }

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("time")
    private String timestamp;

    @JsonProperty("glncode")
    private String glncode;

    @JsonProperty("order")
    private List<RoseArticle> list_of_rose_articles;

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
}
