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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by maxl on 26.06.2016.
 */
public class GenericArticle {

    private long id;
    private String pack_title;
    private String pack_title_fr;
    private String pack_size;
    private String pack_unit;
    private String pack_galen;
    private String active_substance;
    private String atc_code;
    private String atc_class;
    private String rose_basis_price = "";
    private String exfactory_price = "";
    private String public_price = "";
    private String fap_price = "";
    private String fep_price = "";
    private String value_added_tax = "";
    private String additional_info;
    private String regnr = "";
    private String ean_code = "";
    private String pharma_code = "";
    private String therapy_code = "";
    private String author = "";
    private String supplier = "";
    private String availability = "";
    private String dropdown_str;
    private String author_code = "";
    private String author_gln_code = "";
    private String replace_pharma_code = "";
    private String replace_ean_code = "";
    private String flags = "";
    private float margin = -1.0f;	// <0.0f -> not initialized
    private float buying_price = 0.0f;
    private float selling_price = 0.0f;;
    private int quantity = 1;
    private int draufgabe = 0;
    private float cash_rebate = 0.0f; // [%]
    private int onstock;
    private int likes;
    private int visible;
    private int free_samples;
    private int shipping_status = 1;
    private boolean npl_article;
    private boolean dlk_flag = false;
    private boolean is_replacement_article = false;
    private boolean is_nota_article = false;
    private String nota_status = "";
    private String last_order = "";

    public GenericArticle() {
        //
    }

    public GenericArticle(String[] entry, String author) {
        if (entry!=null) {
            ean_code = pharma_code = pack_title = pack_size
                     = pack_unit = public_price = exfactory_price
                     = additional_info = "k.A.";
            if (author!=null && !author.isEmpty())
                this.author = author;
            // efp + "|" + pup + "|" + fap + "|" + fep + "|" + vat
            if (entry.length>10) {
                if (!entry[0].isEmpty())
                    pack_title = entry[0];
                if (!entry[1].isEmpty())
                    pack_size = entry[1];
                if (!entry[2].isEmpty())
                    pack_unit = entry[2];
                if (!entry[3].isEmpty())
                    exfactory_price = entry[3];
                if (!entry[4].isEmpty())
                    public_price = entry[4];
                if (!entry[5].isEmpty())
                    fap_price = entry[5];
                if (!entry[6].isEmpty())
                    fep_price = entry[6];
                if (!entry[7].isEmpty())
                    value_added_tax = entry[7];
                if (!entry[8].isEmpty())
                    additional_info = entry[8];
                if (!entry[9].isEmpty())
                    ean_code = entry[9];
                if (!entry[10].isEmpty())
                    pharma_code = entry[10];
                if (entry.length>11) {
                    if (!entry[11].isEmpty()) {
                        visible = Integer.parseInt(entry[11]);
                    }
                }
                if (entry.length>12) {
                    if (!entry[12].isEmpty())
                        free_samples = Integer.parseInt(entry[12]);
                }
            }
            quantity = 1;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPackTitle() {
        return pack_title;
    }

    public void setPackTitle(String pack_title) {
        this.pack_title = pack_title;
    }

    public String getPackTitleFR() { return pack_title_fr; }

    public void setPackTitleFR(String pack_title_fr) { this.pack_title_fr = pack_title_fr; }

    public String getPackSize() {
        return pack_size;
    }

    public void setPackSize(String pack_size) {
        this.pack_size = pack_size;
    }

    public String getPackUnit() {
        return pack_unit;
    }

    public void setPackUnit(String pack_unit) {
        this.pack_unit = pack_unit;
    }

    public String getPackGalen() {
        return pack_galen;
    }

    public void setPackGalen(String pack_galen) {
        this.pack_galen = pack_galen;
    }

    public void setRegnr(String regnr) {
        this.regnr = regnr;
    }

    public String getRegnr() {
        return regnr;
    }

    public String getEanCode() { return ean_code; }

    public void setEanCode(String ean_code) {
        this.ean_code = ean_code;
    }

    public String getPharmaCode() {
        return pharma_code;
    }

    public void setPharmaCode(String pharma_code) {
        this.pharma_code = pharma_code;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getCode() {
        return author_code;
    }

    public void setCode(String code) {
        this.author_code = code;
    }

    public String getActiveSubstance() {
        return active_substance;
    }

    public void setActiveSubstance(String active_substance) {
        this.active_substance = active_substance;
    }

    public String getAtcCode() {
        return atc_code;
    }

    public void setAtcCode(String atc_code) {
        this.atc_code = atc_code;
    }

    public String getAtcClass() {
        return atc_class;
    }

    public void setAtcClass(String atc_class) {
        this.atc_class = atc_class;
    }

    public String getTherapyCode() {
        return therapy_code;
    }

    public void setTherapyCode(String therapy_code) {
        this.therapy_code = therapy_code;
    }

    public String getDropDownStr() {
        return dropdown_str;
    }

    public void setDropDownStr(String dropdown_str) {
        this.dropdown_str = dropdown_str;
    }

    public String getAdditionalInfo() {
        return additional_info;
    }

    public void setAdditionalInfo(String additional_info) {
        this.additional_info = additional_info;
    }

    private float getPriceAsFloat(String price_as_str) {
        DecimalFormat df = new DecimalFormat("#.##");
        float price_as_float = 0.0f;
        String price_pruned = price_as_str.replaceAll("[^\\d.]", "");
        if (!price_pruned.isEmpty() && !price_pruned.equals("..")) {
            price_as_float = Float.parseFloat(price_pruned);
        }
        return Float.valueOf(df.format(price_as_float));
    }

    public String getRoseBasisPrice() { return rose_basis_price; }

    public float getRoseBasisPriceAsFloat() { return getPriceAsFloat(rose_basis_price); }

    public void setRoseBasisPrice(String rose_basis_price) { this.rose_basis_price = rose_basis_price; }

    public String getPublicPrice() {
        return public_price;
    }

    public float getPublicPriceAsFloat() {
        DecimalFormat df = new DecimalFormat("#.##");
        float public_as_float = 0.0f;
        String price_pruned = public_price.replaceAll("[^\\d.]", "");
        if (!price_pruned.isEmpty() && !price_pruned.equals("..")) {
            public_as_float = Float.parseFloat(price_pruned);
        } else {
            public_as_float = getExfactoryPriceAsFloat() * 1.80f;
        }
        return Float.valueOf(df.format(public_as_float));
    }

    public void setPublicPrice(String public_price) {
        this.public_price = public_price;
    }

    public String getExfactoryPrice() {
        return exfactory_price;
    }

    public float getExfactoryPriceAsFloat() { return getPriceAsFloat(exfactory_price); }

    public void setExfactoryPrice(String exfactory_price) {
        this.exfactory_price = exfactory_price;
    }

    public void setBuyingPrice(float buying_price) {
        if (margin>=0.0f)
            selling_price = (1.0f+margin)*buying_price;
        else	// default
            selling_price = 1.8f*buying_price;
        this.buying_price = buying_price;
    }

    public void setCashRebate(float cash_rebate) {
        if (cash_rebate>0)
            draufgabe = 0;
        this.cash_rebate = cash_rebate;
    }

    public float getCashRebate() {
        if ((cash_rebate>=0.0f && cash_rebate<0.01f) || (cash_rebate<=0.0f && cash_rebate>-0.01f))
            cash_rebate = 0.0f;
        if (draufgabe>0)
            return (100.0f*(float)draufgabe/(draufgabe+quantity));
        else
            return cash_rebate;
    }

    public void setMargin(float margin) {
        this.margin = margin;
    }

    public float getMargin() {
        return margin;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setItemsOnStock(int onstock) {
        this.onstock = onstock;
    }

    public int getItemsOnStock() {
        return onstock;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getLikes() {
        return likes;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAuthorGln(String gln) {
        this.author_gln_code = gln;
    }

    public String getAuthorGln() {
        return author_gln_code;
    }

    public void setReplacePharma(String pharma) {
        this.replace_pharma_code = pharma;
    }

    public String getReplacePharma() {
        return replace_pharma_code;
    }

    public void setReplaceEan(String ean) { this.replace_ean_code = ean; }

    public String getReplaceEan() { return replace_ean_code; }

    public void setShippingStatus(int shipping_status) { this.shipping_status = shipping_status; }

    public int getShippingStatus() { return shipping_status; }

    public void setNplArticle(boolean npl_article) { this.npl_article = npl_article; }

    public boolean isNplArticle() { return npl_article; }

    public void setDlkFlag(boolean dlk_flag) { this.dlk_flag = dlk_flag; }

    public boolean getDlkFlag() { return this.dlk_flag; }

    public void setFlags(String flags) { this.flags = flags; }

    public String getFlags() {
        return flags;
    }

    public ArrayList<String> getFlagsArray() {
        return Arrays.stream(this.getFlags().split(","))
            .map(s -> s.trim())
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean isOriginal() {
        return this.getFlagsArray().contains("O");
    }

    public boolean isGenerikum() {
        return this.getFlagsArray().contains("G");
    }

    public boolean isAvailable() { return !isNotAvailable(); }

    public boolean isNotAvailable()  { return availability.matches("(.*)[0-9]{4}"); } // { return availability.contains(".2153"); }

    public boolean isOffMarket() { return availability.equals("-1"); }

    public boolean isNotInStockData() { return availability.contains("-2"); }

    public void setReplacementArticle(boolean is_replacement) { is_replacement_article = is_replacement; }

    public boolean isReplacementArticle() { return is_replacement_article; }

    public String getAvailDate() {
        try {
            DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
            // Get current date
            Calendar now = Calendar.getInstance();
            // Get Ausstandsdatum
            Calendar avail = Calendar.getInstance();
            avail.setTime(df.parse(availability));
            // Difference in ms -> days
            long diff_ms = avail.getTime().getTime() - now.getTime().getTime();
            long diff_days = diff_ms / (1000 * 60 * 60 * 24);
            if (diff_days < 10 * 365)   // This are 10 years! Neg. difference are also accepted.
                return availability;
            else
                return "";

        } catch (ParseException e) {
            return "";
        }
    }

    public void setNotaArticle(boolean is_nota) { is_nota_article = is_nota; }

    public boolean isNotaArticle() { return is_nota_article; }

    public void setNotaStatus(String status) { nota_status = status; }

    public String getNotaStatus() { return nota_status; }

    public void setLastOrder(String order) { last_order = order; }

    public String getLastOrder() { return last_order; }
}
