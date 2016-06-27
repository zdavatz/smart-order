package models;

/**
 * Created by maxl on 26.06.2016.
 */
public class Article {

    private long id;
    private String pack_title;
    private String pack_size;
    private String pack_unit;
    private String pack_galen;
    private String active_substance;
    private String atc_code;
    private String atc_class;
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
    private String replace_ean_code = "";
    private String replace_pharma_code = "";
    private String flags = "";
    private float margin = -1.0f;	// <0.0f -> not initialized
    private float buying_price = 0.0f;
    private float selling_price = 0.0f;;
    private int quantity = 1;
    private int assorted_quantity = 0;
    private int draufgabe = 0;
    private float cash_rebate = 0.0f; // [%]
    private int onstock;
    private int likes;
    private int visible;
    private int free_samples;

    public Article() {
        //
    }

    public Article(String[] entry, String author) {
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

    public String getEanCode() {
        return ean_code;
    }

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

    public String getPublicPrice() {
        return public_price;
    }

    public void setPublicPrice(String public_price) {
        this.public_price = public_price;
    }

    public String getExfactoryPrice() {
        return exfactory_price;
    }

    public void setExfactoryPrice(String exfactory_price) {
        this.exfactory_price = exfactory_price;
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

    public boolean isOffMarket() {
        return availability.equals("-1");
    }

    public void setReplaceEan(String ean) {
        this.replace_ean_code = ean;
    }

    public String getReplaceEan() {
        return replace_ean_code;
    }

    public void setReplacePharma(String pharma) {
        this.replace_pharma_code = pharma;
    }

    public String getReplacePharma() {
        return replace_pharma_code;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public String getFlags() {
        return flags;
    }
}
