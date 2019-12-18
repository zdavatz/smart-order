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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;

class PriceSerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        // Desired price style here
        jgen.writeString(value.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
    }
}

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

    @JsonProperty("title_FR")
    private String title_FR;

    @JsonProperty("replaces_article")
    private String replaces_article = "";

    @JsonProperty("core_assort")
    private boolean core_assort;

    @JsonProperty("supplier")
    private String supplier;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("swissmed")
    String swissmed;

    @JsonProperty("rose_price")
    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal rose_basis_price;

    @JsonProperty("public_price")
    @JsonSerialize(using = PriceSerializer.class)
    private BigDecimal public_price;

    @JsonIgnore
    private BigDecimal exfactory_price;

    @JsonProperty("prefs")
    private String prefs;

    @JsonProperty("avail_date")
    private String avail_date;

    @JsonProperty("shipping")
    private String shipping;

    @JsonProperty("off_market")
    private boolean off_market;

    @JsonProperty("dlk_flag")
    private boolean dlk_flag;

    @JsonProperty("npl")
    private boolean netto_price_list;

    @JsonProperty("is_nota")
    private boolean is_nota;

    @JsonProperty("nota_status")
    private String nota_status;

    @JsonProperty("last_order")
    private String last_order;

    /**
     * The default number of alternatives to display by GUI
     */
    @JsonProperty("alt")
    private Integer alt = 2;

    @JsonProperty("alternatives")
    public LinkedList<RoseArticle> alternatives;

    private String size;

    private String galen;

    private String unit;

    private String availability;

    private Boolean isOriginal = false;

    private String author_gln_code = "";

    @JsonIgnore
    public String getGtin() { return gtin; }

    @JsonIgnore
    public String getTitle() { return title; }

    @JsonIgnore
    public String getTitleFR() { return title_FR; }

    @JsonIgnore
    public String getRoseBasisPrice() { return rose_basis_price.setScale(2, BigDecimal.ROUND_HALF_UP).toString(); }

    @JsonIgnore
    public String getPublicPrice() { return public_price.setScale(2, BigDecimal.ROUND_HALF_UP).toString(); }

    @JsonIgnore
    public String getExFactoryPrice() { return exfactory_price.setScale(2, BigDecimal.ROUND_HALF_UP).toString(); }

    @JsonIgnore
    public String getQuantity() { return Integer.toString(quantity); }

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

    @JsonIgnore
    public Integer getAlt() { return alt; }

    @JsonIgnore
    public boolean isOriginal() { return isOriginal; }

    @JsonIgnore
    public boolean isNota() { return is_nota; }

    @JsonIgnore
    public String getAuthorGlnCode() { return author_gln_code; }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public void setPharma(String pharma) {
        this.pharma = pharma;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitleFR(String title_FR) { this.title_FR = title_FR; }

    public void setReplacesArticle(String article) { this.replaces_article = article; }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setSwissmed(String swissmed) {
        this.swissmed = swissmed;
    }

    public void setRoseBasisPrice(float rose_basis_price) {
        this.rose_basis_price = new BigDecimal(rose_basis_price);
    }

    public void setPublicPrice(float public_price) {
        this.public_price = new BigDecimal(public_price);
    }

    public void setExfactoryPrice(float exfactory_price) { this.exfactory_price = new BigDecimal(exfactory_price); }

    public void setPreferences(String prefs) {
        this.prefs = prefs;
    }

    public void setAvailDate(String avail_date) { this.avail_date = avail_date; }

    public void setShippingStatus(String shipping) {
        this.shipping = shipping;
    }

    public void setOffMarket(boolean off_market) { this.off_market = off_market; }

    public void setDlkFlag(boolean dlk_flag) { this.dlk_flag = dlk_flag; }

    public void setNettoPriceList(boolean netto_price_list) { this.netto_price_list = netto_price_list; }

    public void setSize(String size) { this.size = size; }

    public void setGalen(String galen) { this.galen = galen; }

    public void setUnit(String unit) { this.unit = unit; }

    public void setAvailability(String availability) { this.availability = availability; }

    public void setCoreAssortment(boolean core_assort) { this.core_assort = core_assort; }

    public void setNota(boolean is_nota) { this.is_nota = is_nota; }

    public void setNotaStatus(String status) { this.nota_status = status; }

    public void setLastOrder(String last_order) { this.last_order = last_order; }

    public void setAlt(Integer alt) { this.alt = alt; }

    public void setIsOriginal(Boolean isOriginal) { this.isOriginal = isOriginal; }

    public void setAuthorGlnCode(String gln) {
        this.author_gln_code = gln;
    }
}
