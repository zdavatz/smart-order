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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;

/**
 * Created by maxl on 28.06.2016.
 */
public class RoseArticle {

    @JsonProperty("gtin")
    private String gtin;

    @JsonProperty("pharma")
    private String pharma;

    @JsonProperty("title")
    private String title;

    @JsonProperty("supplier")
    private String supplier;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("swissmed")
    String swissmed;

    @JsonProperty("rose_price")
    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal rose_price;

    @JsonProperty("public_price")
    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal public_price;

    @JsonProperty("cash_rebate")
    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal cash_rebate;

    @JsonProperty("prefs")
    private String prefs;

    @JsonProperty("shipping")
    private String shipping;

    @JsonProperty("npl")
    private boolean netto_price_list;

    @JsonProperty("alternatives")
    public LinkedList<RoseArticle> alternatives;

    private String size;

    private String galen;

    private String unit;

    private String availability;

    @JsonIgnore
    public String getGtin() { return gtin; }

    @JsonIgnore
    public String getTitle() { return title; }

    @JsonIgnore
    public String getRosePrice() { return rose_price.setScale(2, BigDecimal.ROUND_HALF_UP).toString(); }

    @JsonIgnore
    public String getPublicPrice() { return public_price.setScale(2, BigDecimal.ROUND_HALF_UP).toString(); }

    @JsonIgnore
    public String getQuantity() { return Integer.toString(quantity); }

    @JsonIgnore
    public String getCashRebate() { return cash_rebate.toString(); }

    @JsonIgnore
    public String getPreferences() { return prefs; }

    @JsonIgnore
    public String getShippingStatus() { return shipping; }

    @JsonIgnore
    public String getSize() { return size; }

    @JsonIgnore
    public String getGalen() { return galen; }

    @JsonIgnore
    public String getUnit() { return unit; }

    @JsonIgnore
    public String getAvailability() { return availability; }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public void setPharma(String pharma) {
        this.pharma = pharma;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setSwissmed(String swissmed) {
        this.swissmed = swissmed;
    }

    public void setRosePrice(float rose_price) {
        this.rose_price = new BigDecimal(rose_price);
    }

    public void setPublicPrice(float public_price) {
        this.public_price = new BigDecimal(public_price);
    }

    public void setCashRebate(float cash_rebate) {
        this.cash_rebate = new BigDecimal(cash_rebate);
    }

    public void setPreferences(String prefs) {
        this.prefs = prefs;
    }

    public void setShippingStatus(String shipping) {
        this.shipping = shipping;
    }

    public void setNettoPriceList(boolean netto_price_list) { this.netto_price_list = netto_price_list; }

    public void setSize(String size) { this.size = size; }

    public void setGalen(String galen) { this.galen = galen; }

    public void setUnit(String unit) { this.unit = unit; }

    public void setAvailability(String availability) { this.availability = availability; }
}

class PriceSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        // Desired price style here
        jgen.writeString(value.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
    }
}
