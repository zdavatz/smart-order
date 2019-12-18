/*
Copyright (c) 2016 ywesee GmbH, created by ML <cybrmx@gmail.com>

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

import com.maxl.java.shared.NotaPosition;
import com.maxl.java.shared.User;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by maxl on 29.06.2016.
 */
public class ShoppingRose {

    private HashMap<String, Float> m_sales_figures_map = null;
    private HashMap<String, String> m_rose_ids_map = null;
    private HashMap<String, String> m_rose_direct_subst_map = null;
    private HashMap<String, List<NotaPosition>> m_rose_nota_map = null;
    private ArrayList<String> m_auto_generika_list = null;
    private ArrayList<String> m_auth_keys_list = null;
    private HashMap<String, List<GenericArticle>> m_map_similar_articles = null;
    private HashMap<String, Pair<Integer, Integer>> m_stock_map = null;
    private float m_total_dlk_costs = 0.0f;
    private User m_user_preference = null;

    private Map<String, GenericArticle> m_shopping_basket = null;

    private String m_customer_gln_code = "";

    private MessageDigest m_message_digest;

    /**
     * Constructor!
     * @param customer_id
     */
    public ShoppingRose(String customer_id) {
        // Get rose id map
        RoseData rd = RoseData.getInstance();

        m_rose_ids_map = rd.rose_ids_map();
        // "Normalize" id
        if (customer_id.length()==6) {
            if (m_rose_ids_map!=null && m_rose_ids_map.containsKey(customer_id))
                m_customer_gln_code = m_rose_ids_map.get(customer_id);
        } else if (customer_id.length()==13) {
            m_customer_gln_code = customer_id;
        }
        if (!m_customer_gln_code.isEmpty())
            loadRoseData();
        else
            System.out.println(">> Error: customer glncode or roseid is missing or wrong!");

        m_stock_map = rd.rose_stock_map();
        if (m_stock_map==null)
            System.out.println(">> Error: stock map is missing or corrupted!");
    }

    public String getCustomerGlnCode() {
        return m_customer_gln_code;
    }

    public boolean checkAuthKey(String auth_key) {
        if (m_auth_keys_list!=null)
            return m_auth_keys_list.contains(auth_key);
        return false;
    }

    public String randomHashCode(String hash_str) {
        // Initialize crypto hash for basket
        try {
            m_message_digest = MessageDigest.getInstance("SHA-256");
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Random rand = new Random();
        String hash_code = "";
        hash_str += rand.nextLong();
        try {
            byte[] digest = m_message_digest.digest(hash_str.getBytes("UTF-8"));
            digest = Arrays.copyOf(digest, 16); // 16 character hash!
            BigInteger bigInt = new BigInteger(1, digest);
            hash_code = bigInt.toString(16);    // 0-9a-f (hexadecimal)
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return hash_code;
    }

    public void updateShippingStatus(GenericArticle article) {
        int shipping_status = shippingStatus(article, article.getQuantity());
        article.setShippingStatus(shipping_status);
    }

    private void loadRoseData() {
        RoseData rd = RoseData.getInstance();
        // Load sales figures file
        m_sales_figures_map = rd.sales_figs_map();
        // Load auto generika file
        m_auto_generika_list = rd.autogenerika_list();
        // Load direct substition map
        m_rose_direct_subst_map = rd.rose_direct_subst_map();
        // Load nota position amp
        m_rose_nota_map = rd.rose_nota_map();
        // Retrieve authorization keys
        m_auth_keys_list= rd.auth_keys_list();
        // Retrieve user-related information
        HashMap<String, User> user_map = rd.user_map();
        for (User user : user_map.values()) {
            if (user.gln_code.equals(m_customer_gln_code)) {
                m_user_preference = user;
                break;
            }
        }
        if (m_user_preference == null) {
            System.out.println(">> Error: Cannot find customer preferences");
        }
    }

    /**
     * Calculates minimum stock (MB) for the given article
     * Return -1 if article is not listed
     *
     * @param article
     * @return min stock/inventory
     */
    private int minStock(GenericArticle article) {
        /*
        float sales_figure = 0.0f;
        // Average of sales figure over last 12 days times safety margin
        if (m_sales_figures_map.containsKey(article.getPharmaCode()))
            sales_figure = 2.5f * m_sales_figures_map.get(article.getPharmaCode()) / 12.0f + 1.0f;
        else
            return -1;
        return (int)sales_figure;
        */
        return (int)(article.getItemsOnStock() * 0.9f); // 90%
    }

    /**
     * Returns items on stock (first: zur Rose, second: Voigt)
     * @param pharma_code
     * @return
     */
    private Pair<Integer, Integer> getItemsOnStock(String pharma_code) {
        if (!pharma_code.isEmpty()) {
            if (m_stock_map != null && m_stock_map.containsKey(pharma_code))
                return m_stock_map.get(pharma_code);
        }
        return null;
    }

    private Pair<Integer, Integer> stockInfo(Pair<Integer, Integer> current_stock) {
        /*
           INPUT
           - first: Rose
           - second: Voigt
           OUTPUT
           - first: total stock
           - second: min stock
        */
        if (current_stock!=null) {
            // Sum Rose and Voigt stock
            int total_stock = current_stock.first + current_stock.second;
            return new Pair<>(total_stock, (int)(total_stock * 0.9f));
            /*
            if (current_stock.first > 0) {
                // zur Rose stock (current stock + min stock)
                return new Pair<>(current_stock.first, (int) (current_stock.first * 0.9f));
            } else if (current_stock.second > 0) {
                // Voigt stock (current stock + min stock)
                return new Pair<>(current_stock.second, (int) (current_stock.second * 0.85));
            }
            */
        }
        return new Pair<>(0, 0);
    }

    /**
     * Calculates shipping status as an integer given an article an arbitrary quantity
     *
     * @param article
     * @param quantity
     * @return shipping status
     */
    private int shippingStatus(GenericArticle article, int quantity) {
        // New on 01.Jun.2017: items on stock are retrieved using the pharmacode
        Pair<Integer, Integer> itemsOnStock = getItemsOnStock(article.getPharmaCode());    // returns (zur Rose, Voigt)

        // The following function sums zur Rose and Voigt stocks...
        Pair<Integer, Integer> stockInfo = stockInfo(itemsOnStock);     // returns (current, minimum)
        int curstock = stockInfo.first; // Current
        int mstock = stockInfo.second;  // Minumum

        // Diese Artikel sind ausser handel -> SCHWARZ
        if (article.isOffMarket() && curstock <= 0)
            return 10;
        // Diese Artikel fehlen auf unbestimmte Zeit -> ROT
        if (article.isNotAvailable() && curstock <= 0)
            return 5;
        // Beschaffungsartikel sind immer ORANGE
        if (article.isNotInStockData() /* || article.getSupplier().toLowerCase().contains("voigt") */) {
            return 4;
        }

        // @maxl 18.Jan.2016: empirical rule (see emails)
        if (mstock < 0 && curstock >= 0)
            mstock = 12;

        // @maxl 2.Feb.2018
        if (curstock == 0)
            return 4;   // ORANGE

        if (curstock < 0)
                return 5; //  RED (ein negativer Lagerbestand soll schlechter als Lagerbestand = 0 behandelt werden)

        if (mstock >= 0) {
            if (curstock >= mstock && curstock >= quantity && mstock >= quantity)
                return 1;    // GREEN
            else if (curstock < mstock && curstock >= quantity && mstock > quantity)
                return 2;    // YELLOW/GOLD
            else if (curstock > mstock && curstock >= quantity && mstock < quantity)
                return 3;    // YELLOW/GOLD
            else if (curstock <= mstock && curstock < quantity)
                return 4;    // ORANGE
            else
                return 5;    // RED
        }

        return 10;           // BLACK
    }

    private Pair<String, Integer> shippingStatusColor(int status) {
        switch (status) {
            case 1:
                return new Pair<>("green", 1);
            case 2:
                return new Pair<>("gold", 2);
            case 3:
                return new Pair<>("gold", 3);
            case 4:
                return new Pair<>("orange", 4);
            case 5:
                return new Pair<>("red", 5);
        }
        return new Pair<>("black", -1);
    }

    private int rosePreference(GenericArticle article) {
        String name = GenericArticle.eanNameMap.get(article.getEanCode());
        if (name != null && Constants.rosePreferences.containsKey(name)) {
            return Constants.rosePreferences.get(name);
        }

        String supplier = article.getSupplier().toLowerCase();
        for (String p : Constants.rosePreferences.keySet()) {
            if (supplier.contains(p))
                return Constants.rosePreferences.get(p);
        }
        // Else return a number bigger than 5!
        return 10;
    }

    private boolean isAutoGenerikum(String ean) {
        return m_auto_generika_list.contains(ean);
    }

    private boolean isPreferredByRose(GenericArticle article) {
        return rosePreference(article) < 10;
    }

    private int sortCustomerPreference(GenericArticle a1, GenericArticle a2) {
        boolean is1Preferred = m_user_preference.isEanPreferred(a1.getEanCode());
        boolean is2Preferred = m_user_preference.isEanPreferred(a2.getEanCode());
        if (is1Preferred && !is2Preferred) {
            return -1;
        } else if (!is1Preferred && is2Preferred) {
            return 1;
        }
        return 0;
    }

    private int sortSameArticleWithDifferentSize(GenericArticle mainArticle, GenericArticle a1, GenericArticle a2) {
        boolean is1Same = mainArticle.isSimilarByTitle(a1);
        boolean is2Same = mainArticle.isSimilarByTitle(a2);
        if (is1Same && !is2Same) {
            return -1;
        } else if (!is1Same && is2Same) {
            return 1;
        }
        return 0;
    }

    private int sortShippingStatus(GenericArticle a1, GenericArticle a2, final int quantity) {
        int value1 = shippingStatus(a1, quantity);
        int value2 = shippingStatus(a2, quantity);
        // Returns
        //  = 0 if value1 = value2
        // 	< 0 if value1 < value2
        //  > 0 if value1 > value2
        return Integer.valueOf(value1)
                .compareTo(value2);
    }

    private int sortOriginals(GenericArticle a1, GenericArticle a2) {
        int value1 = a1.isOriginal() ? 1 : -1;
        int value2 = a2.isOriginal() ? 1 : -1;
        // Returns
        //  = 0 if value1 = value2
        // 	< 0 if value1 < value2
        //  > 0 if value1 > value2
        return -Integer.valueOf(value1)
                .compareTo(value2);
    }

    private int sortSize(GenericArticle a1, GenericArticle a2, String sizeStr) {
        try {
            int size1 = Integer.parseInt(a1.getPackSize());
            int size2 = Integer.parseInt(a2.getPackSize());
            int size = Integer.parseInt(sizeStr);
            return Math.abs(size - size1) - Math.abs(size - size2);
        } catch (Exception e) {
            return 0;
        }
    }

    private int sortDosage(GenericArticle a1, GenericArticle a2, String dosage) {
        int value1 = a1.getPackUnit().equals(dosage) ? 1 : -1;
        int value2 = a2.getPackUnit().equals(dosage) ? 1 : -1;
        // Returns
        //  = 0 if value1 = value2
        // 	< 0 if value1 < value2
        //  > 0 if value1 > value2
        return -Integer.valueOf(value1)
                .compareTo(value2);
    }

    private int sortAutoGenerika(GenericArticle a1, GenericArticle a2) {
        int value1 = isAutoGenerikum(a1.getEanCode()) ? 1 : -1;
        int value2 = isAutoGenerikum(a2.getEanCode()) ? 1 : -1;

        // System.out.println("AG: " + a1.getSupplier() + " -> " + value1 + " | " + a2.getSupplier() + " -> " + value2);

        // Returns
        //  = 0 if value1 = value2
        // 	< 0 if value1 < value2
        //  > 0 if value1 > value2
        return -Integer.valueOf(value1)
                .compareTo(value2);
    }

    private int sortRosePreference(GenericArticle a1, GenericArticle a2) {
        int value1 = rosePreference(a1);
        int value2 = rosePreference(a2);
        // Returns
        //  = 0 if value1 = value2
        // 	< 0 if value1 < value2
        //  > 0 if value1 > value2
        return Integer.valueOf(value1)
                .compareTo(value2);
    }

    private void sortSimilarArticles(GenericArticle article) {
        int quantity = article.getQuantity();
        String size = article.getPackSize();
        String dosage = article.getPackUnit();
        boolean isOrangeOrRed = article.getShippingStatus() == 4 || article.getShippingStatus() == 5;
        // Loop through all entries and sort/filter
        for (Map.Entry<String, List<GenericArticle>> entry : m_map_similar_articles.entrySet()) {
            // Ean code of "key" article
            String ean_code = entry.getKey();
            // Copy list of similar/comparable articles
            LinkedList<GenericArticle> list_of_similar_articles = new LinkedList<GenericArticle>(entry.getValue());
            // Remove the key article itself
            list_of_similar_articles.removeIf(a -> a.getEanCode().equals(ean_code));

            boolean isCase6 = !m_user_preference.isEanPreferred(article.getEanCode()) &&
                list_of_similar_articles
                    .stream()
                    .anyMatch(a -> m_user_preference.isEanPreferred(a.getEanCode()) && a.isOriginal());
            boolean isCase7 = isOrangeOrRed && m_user_preference.isPreferenceEmpty();
            boolean isCase8 = article.isNotaArticle();

            if (list_of_similar_articles.size() > 0) {
                Collections.sort(list_of_similar_articles, new Comparator<GenericArticle>() {
                    @Override
                    /*  Sortierlogik: Zuerst wird nach Lieferbarkeit sortiert (1. Priorität).
                        Falls zwei Produkte dieselbe Lieferbarkeit haben (z.B. GREEN sind), dann
                        wird nach Original/Nicht-Original sortiert. Nun kommen die Ärzte-Präferenzen zum Zug.
                        Falls zwei Produkte Nicht-Originale,
                        d.h. Generika sind, dann wird nach Autogenerika sortiert - die kommen zuoberst.
                        Zu guter letzt wird nach den Generika Präferenzen zur Rose sortiert.

                        v1.3:
                        Case6. Hat der Kunde eine Präferenz und gibt es gemäss BAG ein Original von diesem Produkt,
                        zuerst Kundenpräferenz und dann Original gemäss BAG.

                        Case7. Ist das bestellte Produkt vom Kunden ohne Präferenz nicht an Lager (orange, rot),
                        zur Rose sortiert.

                        Case8. Die Sortierung bei der Nota-Präferenz ist immer zuerst gleiches
                        Produkt (andere Packungsgrösse), dann Kundenpräferenz,
                        dann Original gemäss BAG falls vorhanden dann zur Rose Präferenz.
                     */
                    public int compare(GenericArticle a1, GenericArticle a2) {
                        int c = 0;
                        // v1.3 Case 6.
                        if (isCase6) {
                            if (c == 0) {
                                c = sortCustomerPreference(a1, a2);
                            }
                            if (c == 0) {
                                c = sortOriginals(a1, a2);
                            }
                        }
                        if (isCase7) {
                            if (c == 0) {
                                c = sortRosePreference(a1, a2);
                            }
                        }
                        if (isCase8) {
                            if (c == 0) {
                                c = sortSameArticleWithDifferentSize(article, a1, a2);
                            }
                            if (c == 0) {
                                c = sortCustomerPreference(a1, a2);
                            }
                            if (c == 0) {
                                c = sortOriginals(a1, a2);
                            }
                            if (c == 0) {
                                c = sortRosePreference(a1, a2);
                            }
                        }

                        // PRIO 1: Lieferbarkeit
                        if (c == 0) {
                            c = sortShippingStatus(a1, a2, quantity);
                        }
                        // PRIO 2: Original
                        if (c == 0) {
                            c = sortOriginals(a1, a2);
                        }
                        // PRIO 3:
                        if (c == 0) {
                            c = sortDosage(a1, a2, dosage);
                        }
                        // PRIO 4:
                        if (c == 0) {
                            c = sortSize(a1, a2, size);
                        }
                        // PRIO 5: AG - Autogenerikum
                        if (c == 0) {
                            c = sortAutoGenerika(a1, a2);
                        }
                        // PRIO 6: ZRP - Generikum Präferenz zur Rose
                        if (c == 0) {
                            c = sortRosePreference(a1, a2);
                        }
                        return c;
                    }
                });
                // Assert
                assert (list_of_similar_articles.size() > 0);
            }

            m_map_similar_articles.put(ean_code, list_of_similar_articles);
        }
    }

    public void setShoppingBasket(Map<String, GenericArticle> shopping_basket) {
        synchronized(this) {
            m_shopping_basket = shopping_basket;
        }
    }

    public void updateMapSimilarArticles(Map<String, List<GenericArticle>> similar_articles) {
        // Make a copy of the hash map
        m_map_similar_articles = new HashMap<>(similar_articles);
    }

    public String hasDirectSubstitute(String pharma_code) {
        if (!pharma_code.isEmpty()) {
            if (m_rose_direct_subst_map != null && m_rose_direct_subst_map.containsKey(pharma_code))
                return m_rose_direct_subst_map.get(pharma_code);
        }
        return null;
    }

    private String generatePreferences(GenericArticle ga) {
        String preference_str = "";

        if (isAutoGenerikum(ga.getEanCode())) {
            preference_str += "AG";
        }
        if (isPreferredByRose(ga)) {
            if (!preference_str.isEmpty())
                preference_str += ", ";
            preference_str += "ZRP";
        }

        return preference_str;
    }

    private Integer generateAlt(RoseArticle article, GenericArticle ga) {
        // Generate the default number of alternatives displayed
        boolean isOrangeOrRed = ga.getShippingStatus() == 4 || ga.getShippingStatus() == 5;
        if (ga.isNplArticle()) {
            if (ga.getShippingStatus() == 1) {
                // Green
                return 0;
            } else if (isOrangeOrRed) {
                // Orange or Red
                return 2;
            }
        }
        if (m_user_preference.isPreferenceEmpty()
            && isPreferredByRose(ga)
            && ga.getShippingStatus() == 1
        ) {
            return 0;
        }

        if (!m_user_preference.isPreferenceEmpty()
            && !m_user_preference.isEanPreferred(article.getGtin())
            && ga.getShippingStatus() == 1
        ) {
            boolean hasPreferredAlternative = article.alternatives
                .stream()
                .anyMatch(a -> m_user_preference.isEanPreferred(a.getGtin()));
            if (hasPreferredAlternative) {
                return 1;
            }
        }

        if (!m_user_preference.isPreferenceEmpty() && isOrangeOrRed) {
            boolean hasBetterAlternative = article.alternatives
                .stream()
                .anyMatch(ra -> ra.isOriginal() && m_user_preference.isEanPreferred(ra.getGtin()));
            if (hasBetterAlternative) {
                return 2;
            }
        }

        if (m_user_preference.isPreferenceEmpty() && isOrangeOrRed) {
            return 2;
        }
        if (article.isNota()) {
            return 2;
        }

        // Default value of Alt is 2
        return 2;
    }

    private boolean generateCoreAssort(GenericArticle article) {
        if (article.isNplArticle()) {
            return true;
        }
        String ean = article.getEanCode();
        String name = GenericArticle.eanNameMap.get(ean);
        if (m_user_preference.isEanPreferred(ean) || "mepha".equals(name)) {
            if (!article.isBiotechnologica()) {
                return true;
            }
        }
        return false;
    }

    public List<RoseArticle> updateShoppingCart() {

        List<RoseArticle> list_rose_articles = new ArrayList<>();

        if (m_shopping_basket!=null && m_shopping_basket.size()>0) {
            // Loop through articles in shopping basket
            for (Map.Entry<String, GenericArticle> entry : m_shopping_basket.entrySet()) {
                GenericArticle article = entry.getValue();

                RoseArticle rose_article = new RoseArticle();
                rose_article.alternatives = new LinkedList<>();
                rose_article.setAlt(2);

                // Set buying price
                float rose_price = article.getRoseBasisPriceAsFloat();
                article.setBuyingPrice(rose_price);

                int quantity = article.getQuantity();

                String pharm_code = article.getPharmaCode();
                String ean_code = article.getEanCode();
                String flags_str = article.getFlags();

                String preference_str = generatePreferences(article);

                // Shipping status
                int shipping_ = shippingStatus(article, quantity);
                Pair<String, Integer> shipping_status = shippingStatusColor(shipping_);

                rose_article.setGtin(ean_code);
                rose_article.setPharma(pharm_code);
                rose_article.setTitle(article.getPackTitle());
                rose_article.setTitleFR(article.getPackTitleFR());
                rose_article.setSize(article.getPackSize());
                rose_article.setGalen(article.getPackGalen());
                rose_article.setUnit(article.getPackUnit());
                rose_article.setSupplier(article.getSupplier());
                rose_article.setRoseBasisPrice(rose_price);
                rose_article.setPublicPrice(article.getPublicPriceAsFloat());
                rose_article.setExfactoryPrice(article.getExfactoryPriceAsFloat());
                rose_article.setQuantity(article.getQuantity());
                rose_article.setSwissmed(flags_str);
                rose_article.setPreferences(preference_str);
                rose_article.setAvailDate(article.getAvailDate());
                rose_article.setShippingStatus(shipping_status.first);
                rose_article.setOffMarket(article.isOffMarket());
                rose_article.setDlkFlag(article.getDlkFlag());
                rose_article.setNettoPriceList(article.isNplArticle());
                rose_article.setNota(article.isNotaArticle());
                rose_article.setNotaStatus(article.getNotaStatus());
                rose_article.setLastOrder(article.getLastOrder());
                rose_article.setIsOriginal(article.isOriginal());
                rose_article.setCoreAssortment(generateCoreAssort(article));

                // article points to object which was inserted last...
                if (m_map_similar_articles.containsKey(ean_code)) {
                    sortSimilarArticles(article);

                    // Loop through all alternatives 'a' of 'article'
                    List<GenericArticle> la = m_map_similar_articles.get(ean_code);
                    for (GenericArticle a : la) {
                        String alter_ean_code = a.getEanCode();

                        if (!alter_ean_code.equals(ean_code)) {
                            if (article.isOriginal()
                                    || (article.isGenerikum() && a.isGenerikum())
                                    || (article.isGenerikum() && !isAutoGenerikum(alter_ean_code))
                                    || a.isReplacementArticle()) {

                                if (a.isAvailable() && !a.isOffMarket()) {
                                    RoseArticle ra = new RoseArticle();

                                    rose_price = a.getRoseBasisPriceAsFloat();

                                    a.setBuyingPrice(rose_price);
                                    a.setQuantity(quantity);

                                    flags_str = a.getFlags();

                                    preference_str = generatePreferences(a);

                                    shipping_ = shippingStatus(a, a.getQuantity());
                                    shipping_status = shippingStatusColor(shipping_);

                                    if (a.isReplacementArticle())
                                        ra.setReplacesArticle(article.getEanCode() + ", " + article.getPackTitle() + ", " + article.getPackTitleFR());

                                    ra.setGtin(alter_ean_code);
                                    ra.setPharma(a.getPharmaCode());
                                    ra.setTitle(a.getPackTitle());
                                    ra.setTitleFR(a.getPackTitleFR());
                                    ra.setSize(a.getPackSize());
                                    ra.setGalen(a.getPackGalen());
                                    ra.setUnit(a.getPackUnit());
                                    ra.setSupplier(a.getSupplier());
                                    ra.setRoseBasisPrice(rose_price);
                                    ra.setPublicPrice(a.getPublicPriceAsFloat());
                                    ra.setExfactoryPrice(a.getExfactoryPriceAsFloat());
                                    ra.setQuantity(a.getQuantity());
                                    ra.setSwissmed(flags_str);
                                    ra.setPreferences(preference_str);
                                    ra.setAvailDate(a.getAvailDate());
                                    ra.setShippingStatus(shipping_status.first);
                                    ra.setOffMarket(a.isOffMarket());
                                    ra.setDlkFlag(a.getDlkFlag());
                                    ra.setNettoPriceList(a.isNplArticle());
                                    ra.setNota(a.isNotaArticle());
                                    ra.setNotaStatus(a.getNotaStatus());
                                    ra.setLastOrder(a.getLastOrder());
                                    ra.setIsOriginal(a.isOriginal());

                                    ra.setCoreAssortment(generateCoreAssort(a));
                                    ra.setAlt(null);

                                    rose_article.alternatives.add(ra);
                                }
                            }
                        }
                    }
                }
                if (rose_article.alternatives == null) {
                    rose_article.setAlt(null);
                } else {
                    rose_article.setAlt(Math.min(
                        rose_article.alternatives.size(),
                        generateAlt(rose_article, article)
                    ));
                }
                list_rose_articles.add(rose_article);
            }
        }

        return list_rose_articles;
    }
}
