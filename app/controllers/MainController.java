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

package controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import models.*;
import myactors.OrderLogActor;
import play.db.NamedDatabase;
import play.db.Database;
import play.libs.Json;
import play.mvc.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import static play.libs.Json.*;

public class MainController extends Controller {

    private static final String KEY_ROWID = "_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_AUTH = "auth";
    private static final String KEY_EAN = "eancode";
    private static final String KEY_PHARMA = "pharmacode";
    private static final String KEY_ATC = "atc";
    private static final String KEY_PACK_INFO = "pack_info_str";
    private static final String KEY_PACKAGES = "packages";

    private static final String PACKAGES_TABLE = String.format("%s,%s,%s,%s,%s,%s",
            KEY_ROWID, KEY_TITLE, KEY_AUTH, KEY_ATC, KEY_PACK_INFO, KEY_PACKAGES);

    private static final String ROSE_DB_TABLE = "rosedb";
    private static final String AIPS_DB_TABLE = "amikodb";

    @Inject @NamedDatabase("rose") Database rose_db;
    @Inject @NamedDatabase("aips") Database aips_db;

    public Result index() {
        return ok("Welcome to AmikoRose");
    }

    public Result test() {
        long starttime = java.lang.System.currentTimeMillis();
        RoseData rd = RoseData.getInstance();
        ArrayList<String> autogenerika = rd.autogenerika_list();
        String basket = "";//"(7680573480114,10)(7680562090041,10)(7680333930248,10)(7680652370046,20)(7680546420598,1)";
        int counter = 0;
        for (String a : autogenerika) {
            basket += "(" + a + ",10)";
            if (counter++>20)
                break;
        }
        String ip_addr = "127.0.0.1:9000/";
        try {
            URL url = new URL("http://" + ip_addr + "/smart/full/compact?pretty=on&authkey=1111&glncode=950757&basket=" + basket);
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }
        Result result = getSmartBasket("on", 0, "1111", "950757", basket);
        String duration = String.format("Duration: %dms\n", (System.currentTimeMillis() - starttime));
        System.out.println(basket);
        System.out.println("Test duration in [ms] = " + duration);
        return result;
    }

    public Result getGtin(String gtin) {
        CompletableFuture<List<GenericArticle>> articles = CompletableFuture.supplyAsync(()->searchEan(gtin));
        CompletableFuture<List<RoseArticle>> rose_articles = articles
                .thenApply(a -> a.stream().map(this::genericArticleToRose).collect(Collectors.toList()));
        // toJson converts the Java object 'f' to JSON
        return rose_articles.thenApply(f -> ok(Json.prettyPrint(toJson(f)))).join();
    }

    public Result getNumRecords() {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> numRecords(rose_db, ROSE_DB_TABLE));
        return future.thenApply(f -> ok(String.format("Num records = %d\n", f))).join();
    }

    public Result getTestBasket(String gtin) {
        long starttime = java.lang.System.currentTimeMillis();
        CompletableFuture<List<GenericArticle>> articles = CompletableFuture.supplyAsync(()->searchEan(gtin));
        // Find substitutes/alternatives
        CompletableFuture<List<RoseArticle>> rose_articles = articles
                .thenApply(a -> a.stream().map(this::getAlternatives).collect(Collectors.toList()));
        // toJson converts the Java object 'f' to JSON
        String res = rose_articles.thenApply(f -> Json.prettyPrint(toJson(f))).join();
        String duration = String.format("Duration: %dms\n\n", (System.currentTimeMillis() - starttime));
        return ok(duration + res);
    }

    /**
     * This is the main entry point to the SmartOrder functionality
     * Accepted are GET requests of the following form:
     *  http://127.0.0.1:9000/smart/full?pretty=on&authkey=1111&glncode=943666&basket=(7680623190048,10)
     *
     */
    @Inject ActorSystem actorSystem;
    public Result getSmartBasket(String pretty, int limit, String auth_key, String gln_code, String basket) {
        ShoppingRose shopping_cart = new ShoppingRose(gln_code);

        if (shopping_cart.checkAuthKey(auth_key)) {
            // Match ean codes (13 digits) and pharma codes (7 digits)
            Pattern p = Pattern.compile("\\((\\d{13}|\\d{7}),(\\d+)\\)");
            Matcher m = p.matcher(basket);
            ArrayList<String> list_of_articles = new ArrayList<>();
            HashMap<String, Integer> map_of_articles = new HashMap<>();
            while (m.find()) {
                String code = m.group(1);
                String qty = m.group(2);
                if (!code.isEmpty() && !qty.isEmpty()) {
                    list_of_articles.add(code);
                    map_of_articles.put(code, Integer.valueOf(qty));
                }
            }
            // Search for ean/pharma codes in rosedb and amikodb (fallback in case nothing is found in first DB)
            List<GenericArticle> articles = list_of_articles.stream()
                    .map(this::searchSingleEan)
                    .collect(Collectors.toList());

            // Set limit to list of articles displayed (either 2 or as many as possible)
            shopping_cart.setResultsLimit(limit>0);

            if (articles.size() > 0) {
                Map<String, GenericArticle> shopping_basket = new HashMap<>();
                Map<String, List<GenericArticle>> map_of_similar_articles = new HashMap<>();
                // Loop through all articles found
                for (GenericArticle article : articles) {
                    String ean = article.getEanCode();
                    String pharma = article.getPharmaCode();

                    // Check for ean/pharma code
                    if (map_of_articles.containsKey(ean) || map_of_articles.containsKey(pharma)) {
                        // Set quantities when found
                        if (map_of_articles.containsKey(ean))
                            article.setQuantity(map_of_articles.get(ean));
                        else
                            article.setQuantity(map_of_articles.get(pharma));
                        // Set shipping status
                        shopping_cart.updateShippingStatus(article);
                        // Add article to shopping basket
                        shopping_basket.put(ean, article);
                        // Find all alternatives using the article's EAN code
                        LinkedList<GenericArticle> la = listSimilarArticles(article);
                        if (la != null) {
                            // Check if ean code is already part of the map, if not add to map
                            if (!map_of_similar_articles.containsKey(ean)) {
                                map_of_similar_articles.put(ean, la);
                            }
                        }
                    }
                }
                // Update shopping basket
                shopping_cart.setShoppingBasket(shopping_basket);
                // Update list of similar articles only for last insert article
                shopping_cart.updateMapSimilarArticles(map_of_similar_articles);
            }

            // List of articles
            List<RoseArticle> list_of_rose_articles = shopping_cart.updateShoppingCart();
            // Calc hash for order
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            String timestamp = dateFormat.format(date);
            String hash = shopping_cart.randomHashCode(timestamp);

            RoseOrder rose_order = new RoseOrder(hash, timestamp, shopping_cart.getCustomerGlnCode());
            rose_order.setListArticles(list_of_rose_articles);

            // Save to file (append)
            ActorRef myActor = actorSystem.actorOf(OrderLogActor.props);
            myActor.tell(rose_order, ActorRef.noSender());

            String order_json;
            if (pretty.equals("on"))
                order_json = Json.prettyPrint(toJson(rose_order));
            else
                order_json = toJson(rose_order).toString();

            return ok(order_json);
        }
        return ok("[]");
    }

    public Result getUserArticles(String pretty, String auth_key, String gln_code) {
        ShoppingRose shopping_cart = new ShoppingRose(gln_code);

        if (shopping_cart.checkAuthKey(auth_key)) {
            // List all articles
            List<GenericArticle> list_of_articles = retrieveAllArticles();
            // Select all articles for which there are discounts for a particular user
            ArrayList<GenericArticle> rebated_articles = new ArrayList<>();
            list_of_articles.forEach( a ->
            {
                if (shopping_cart.getCashRebate(a)>0.0f) {
                    float rbp = a.getRoseBasisPriceAsFloat();
                    a.setCashRebate(rbp * shopping_cart.getCashRebate(a)/100.0f);
                    rebated_articles.add(a);
                }
            });
            // Transform to rose articles
            ArrayList<RoseArticle> rose_articles = rebated_articles.stream()
                    .map(this::genericArticleToRose)
                    .collect(Collectors.toCollection(ArrayList::new));

            // System.out.println("num articles with rebate = " + rose_articles.size() + "/" + list_of_articles.size());

            String order_json;
            if (pretty.equals("on"))
                order_json = Json.prettyPrint(toJson(rose_articles));
            else
                order_json = toJson(rose_articles).toString();

            return ok(order_json);
        }
        return ok("[]");
    }

    /**
     * Maps a generic article to a rose smart order system compatible article
     * @param generic_article
     * @return rose_article
     */
    private RoseArticle genericArticleToRose(GenericArticle generic_article) {
        RoseArticle rose_article = new RoseArticle();

        rose_article.setGtin(generic_article.getEanCode());
        rose_article.setPharma(generic_article.getPharmaCode());
        rose_article.setTitle(generic_article.getPackTitle());
        rose_article.setSize(generic_article.getPackSize());
        rose_article.setGalen(generic_article.getPackGalen());
        rose_article.setUnit(generic_article.getPackUnit());
        rose_article.setSupplier(generic_article.getSupplier());
        rose_article.setRoseBasisPrice(generic_article.getRoseBasisPriceAsFloat());
        rose_article.setPublicPrice(generic_article.getPublicPriceAsFloat());
        rose_article.setExfactoryPrice(generic_article.getExfactoryPriceAsFloat());
        rose_article.setCashRebate(generic_article.getCashRebate());
        rose_article.setQuantity(1);
        rose_article.setSwissmed(generic_article.getFlags());
        rose_article.setPreferences("");
        rose_article.setShippingStatus("");
        rose_article.setAvailability(generic_article.getAvailability());
        rose_article.setNettoPriceList(generic_article.isNplArticle());
        rose_article.alternatives = new LinkedList<>();

        return rose_article;
    }

    /**
     * Used for testing purposes
     * @param article
     * @return rose_article
     */
    private RoseArticle getAlternatives(GenericArticle article) {
        RoseArticle rose_article = genericArticleToRose(article);

        LinkedList<RoseArticle> list_of_similar_articles = listSimilarArticles(article).stream()
                .map(this::genericArticleToRose)
                .collect(Collectors.toCollection(LinkedList::new));
        rose_article.alternatives = list_of_similar_articles;
        return rose_article;
    }

    /**
     * This is a key function that given a generic article finds similar articles
     * Similarity is calculated starting from the ATC code and is then narrowed down
     * by matching dosages and package size.
     * @param article
     * @return list of similar articles
     */
    private LinkedList<GenericArticle> listSimilarArticles(GenericArticle article) {
        LinkedList<GenericArticle> list_a = new LinkedList<>();
        LinkedList<GenericArticle> original_list_a = new LinkedList<>();

        String atc_code = article.getAtcCode();
        String size = article.getPackSize();
        String unit = article.getPackUnit();

        if (atc_code!=null && !atc_code.equals("k.A.")) {
            for (GenericArticle a : searchATC(atc_code)) {
                // Loop through "similar" articles
                if (!a.getAtcCode().equals("k.A.")) {
                    if (!a.getEanCode().equals(article.getEanCode())) {
                        String s = a.getPackSize().toLowerCase();
                        String u = a.getPackUnit().toLowerCase();
                        if (!article.isOffMarket()) {
                            if (!a.isOffMarket()) {
                                // Make sure that articles added to the list are NOT off-the-market
                                // s AND size -> stückzahl, e.g. 12
                                // u AND unit -> dosierung, e.g. 100mg
                                boolean have_same_title = titleComparator(article.getPackTitle(), a.getPackTitle());
                                boolean is_original_but_not_green = article.isOriginal() && article.getShippingStatus() > 1;
                                boolean is_original_alternative_and_green = a.isOriginal() && a.getShippingStatus() == 1;

                                if (is_original_but_not_green && is_original_alternative_and_green && have_same_title) {
                                    // Add it to the list of originals
                                    original_list_a.add(a);
                                } else {
                                    if (!a.isOriginal()) {
                                        if (checkSimilarity2(size, s, unit, u)) {
                                            System.out.println("--> " + a.getPackTitle());
                                            list_a.add(a);
                                        }
                                    }
                                }
                                /*
                                if ((size.contains(s) || s.contains(size)) && (unit.contains(u) || u.contains(unit)) )
                                    list_a.add(a);
                                */
                            }
                        } else {
                            // If the main article is off the market, get some replacements...
                            // Remove all numbers first
                            u = u.replaceAll("[^A-Za-z]", "");
                            unit = unit.replaceAll("[^A-Za-z]", "");
                            // System.out.println(a.getPackTitle() + " -> " + a.getAvailability() + " | " + s + "=" + size + " | " + u + "=" + unit);
                            if (u.equals(unit) && s.equals(size) && !a.isOffMarket())
                                list_a.add(a);
                        }
                    }
                }
            }
        }
        // Add all special originals (checksimilarity3)
        if (original_list_a.size()>0) {
            // Sort according to smart criterion
            Collections.sort(original_list_a, new Comparator<GenericArticle>() {
                private int func(int x) {
                    // Return -1 if value1 == value2, else 1
                    return x==0 ? -1 : 1;
                }
                private int sortUnits(GenericArticle a1, GenericArticle a2) {
                    int u=0, unit1=0, unit2 = 0;
                    if (article.getPackTitle()!=null)
                        u = Integer.valueOf(article.getPackUnit().replaceAll("[^0-9]", ""));
                    if (a1.getPackTitle()!=null)
                        unit1 = Integer.valueOf(a1.getPackUnit().replaceAll("[^0-9]", ""));
                    if (a2.getPackTitle()!=null)
                        unit2 = Integer.valueOf(a2.getPackUnit().replaceAll("[^0-9]", ""));
                    return func(unit1 - u) - func(unit2 - u);
                }
                private int sortSize(GenericArticle a1, GenericArticle a2) {
                    int s=0, size1=0, size2=0;
                    if (article.getPackSize()!=null)
                        s = Integer.valueOf(article.getPackSize());
                    if (a1.getPackSize()!=null)
                        size1 = Integer.valueOf(a1.getPackSize());
                    if (a2.getPackSize()!=null)
                        size2 = Integer.valueOf(a2.getPackSize());
                    return func(size1 - s) - func(size2 - s);
                }
                @Override
                public int compare(GenericArticle a1, GenericArticle a2) {
                    int c = 0;
                    if (c==0)
                        c = sortUnits(a1, a2);
                    if (c==0)
                        c = sortSize(a1, a2);
                    return c;
                }
            });
            // Add to list of alternatives
            list_a.add(original_list_a.get(0));
        }
        // If "Ersatzartikel" exists, add it to list
        String replacement_article = article.getReplacePharma();
        if (replacement_article!=null && !replacement_article.isEmpty()) {
            // Check if article is already in list
            for (GenericArticle a : list_a) {
                if (a.getPharmaCode().equals(replacement_article))
                    return list_a;
            }
            // If not... add it to list
            List<GenericArticle> replace_article = searchEan(replacement_article);
            if (replace_article.size()>0) {
                GenericArticle ra = replace_article.get(0);
                list_a.add(ra);
            }
        }
        return list_a;
    }

    /**
     * Compares two strings (titles)
     * @param title1
     * @param title2
     * @return boolean denoting similarity or not
     */
    private boolean titleComparator(String title1, String title2) {
        // Tokenize titles first
        String[] t1 = title1.split("\\s+");
        String[] t2 = title2.split("\\s+");
        if (t1.length>0 && t2.length>0) {
            if (t1[0].toLowerCase().equals(t2[0].toLowerCase()))
                return true;
        }
        return false;
    }

    private boolean basicSimilarityCheck(String a1, String a2, float s) {
        float s1 = Float.valueOf(a1);
        float s2 = Float.valueOf(a2);
        float f1 = s1*s;
        float f2 = s2*s;
        return s1 > (s2-f2) && s1 < (s2+f2)
                && s2 > (s1-f1) && s2 < (s1+f1);
    }

    private boolean checkSimilarity(String size_1, String size_2, String unit_1, String unit_2) {
        if (size_1.equals(size_2) && unit_1.equals(unit_2))
            return true;
        return false;
    }

    private boolean checkSimilarity2(String size_1, String size_2, String unit_1, String unit_2) {
        boolean check_size = false;
        boolean check_units = false;

        System.out.println(size_1 + " = " + size_2 + " | " + unit_1 + " = " + unit_2);

        if (!size_1.isEmpty() && !size_2.isEmpty())
            check_size = basicSimilarityCheck(size_1, size_2, 0.51f);
        if (!unit_1.isEmpty() && !unit_2.isEmpty())
            check_units = unit_1.equals(unit_2);    // Units/dosage must be the same
        return check_size && check_units;
    }

    private boolean checkSimilarity3(String size_1, String size_2, String unit_1, String unit_2) {
        boolean check_units = false;
        if (checkSimilarity2(size_1, size_2, unit_1, unit_2))
            return true;
        if (!unit_1.isEmpty() && !unit_2.isEmpty()) {
            unit_1 = unit_1.replaceAll("[^0-9]", "");
            unit_2 = unit_2.replaceAll("[^0-9]", "");
            check_units = basicSimilarityCheck(unit_1, unit_2, 1.01f);
        }
        return check_units;
    }

    /**
     * Extracts dosage/unit/prescription strength from package title
     * @param pack_title
     * @return extracted dosage
     */
    private String parseUnitFromTitle(String pack_title) {
        String dosage = "";
        Pattern p = Pattern.compile("(\\d+)(\\.\\d+)?\\s*(ml|mg|mcg|g|mmol|mcg/h|mg/ml)");
        Matcher m = p.matcher(pack_title);
        if (m.find()) {
            dosage = m.group(1);
            String q = m.group(2);
            if (q!=null && !q.isEmpty()) {
                dosage += q;
            }
            dosage += (" " + m.group(3));
        }
        return dosage;
    }

    /**
     * Extracts package size from title
     *
     */
    private int parseSizeFromTitle(String pack_title) {
        String size = "";
        Pattern p = Pattern.compile("(\\d+)\\s*(stk)");
        Matcher m = p.matcher(pack_title);
        if (m.find()) {
            size = m.group(1);
        }
        if (!size.isEmpty())
            return Integer.valueOf(size);
        else
            return 0;
    }

    /** ----------------------------------------------------------------------------
     * ROSE_DB + AMIKO_DB
     ---------------------------------------------------------------------------- */
    private GenericArticle searchSingleEan(String code) {
        GenericArticle article = new GenericArticle();
        boolean article_found = false;

        try {
            Connection conn = rose_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select * from " + ROSE_DB_TABLE + " where "
                    + KEY_EAN + " like '" + code + "%' or "
                    + KEY_EAN + " like '%;" + code + "%' or "
                    + KEY_PHARMA + " like " + "'" + code + "%'";
            ResultSet rs = stat.executeQuery(query);
            if (rs.next()) {
                article = cursorToArticle(rs);
                article_found = true;
            }
            conn.close();
        } catch(SQLException e) {
            System.err.println(">> RoseSqlDb: SQLException in searchEan!");
        }
        // Fallback to aips DB if nothing is found
        if (!article_found) {
            try {

                System.out.println("--> Fallback on aips DB");

                Connection conn = aips_db.getConnection();
                Statement stat = conn.createStatement();
                String query = "select " + PACKAGES_TABLE + " from " + AIPS_DB_TABLE + " where "
                        + KEY_PACKAGES + " like '" + code + "%' or "
                        + KEY_PACKAGES + " like '%|" + code + "%'";
                ResultSet rs = stat.executeQuery(query);
                if (rs.next()) {
                    article = extendedCursorToArticle(rs, code);
                }
                conn.close();
            } catch(SQLException e) {
                System.err.println(">> AipsSqlDb: SQLException in extendedSearchSingleEan!");
            }
        }

        return article;
    }

    /** ----------------------------------------------------------------------------
     * ROSE_DB ONLY
     ---------------------------------------------------------------------------- */
    private int numRecords(Database db, String table) {
        int num_rec = -1;
        try {
            Connection conn = db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select count(*) from " + table;
            ResultSet rs = stat.executeQuery(query);
            num_rec = rs.getInt(1);
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in numRecords");
        }
        return num_rec;
    }

    private List<GenericArticle> retrieveAllArticles() {
        List<GenericArticle> list_of_articles = new ArrayList<>();

        try {
            Connection conn = rose_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select * from " + ROSE_DB_TABLE;
            ResultSet rs = stat.executeQuery(query);
            while (rs.next()) {
                list_of_articles.add(cursorToArticle(rs));
            }
            conn.close();
        } catch(SQLException e) {
            System.err.println(">> RoseSqlDb: SQLException in retrieveAllArticles!");
        }

        return list_of_articles;

    }

    private List<GenericArticle> searchEan(String code) {
        List<GenericArticle> list_of_articles = new ArrayList<>();

        try {
            Connection conn = rose_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select * from " + ROSE_DB_TABLE + " where "
                    + KEY_EAN + " like " + "'" + code + "%' or "
                    + KEY_EAN + " like " + "'%;" + code + "%' or "
                    + KEY_PHARMA + " like " + "'" + code + "%'";
            ResultSet rs = stat.executeQuery(query);
            while (rs.next()) {
                list_of_articles.add(cursorToArticle(rs));
            }
            conn.close();
        } catch(SQLException e) {
            System.err.println(">> RoseSqlDb: SQLException in searchEan!");
        }

        return list_of_articles;
    }

    public List<GenericArticle> searchATC(String atccode) {
        List<GenericArticle> list_of_articles = new ArrayList<>();

        try {
            Connection conn = rose_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select * from " + ROSE_DB_TABLE + " where "
                    + KEY_ATC + " like " + "'" + atccode + "%' or "
                    + KEY_ATC + " like " + "'%;" + atccode + "%'";
            ResultSet rs = stat.executeQuery(query);
            while (rs.next()) {
                list_of_articles.add(cursorToArticle(rs));
            }
            conn.close();
        } catch(SQLException e) {
            System.err.println(">> RoseSqlDb: SQLException in searchATC!");
        }

        return list_of_articles;
    }

    /** ----------------------------------------------------------------------------
     * MAPPINGS FROM DB Format to Article
     ---------------------------------------------------------------------------- */
    private GenericArticle cursorToArticle(ResultSet result) {
        GenericArticle article = new GenericArticle();

        try {
            article.setId(result.getLong(1));			 	// KEY_ROWID
            article.setPackTitle(result.getString(2));	 	// KEY_TITLE
            article.setPackSize(String.valueOf(result.getInt(3)));	// KEY_SIZE
            article.setPackGalen(result.getString(4));	 	// KEY_GALEN
            article.setPackUnit(result.getString(5));	 	// KEY_UNIT
            String ean_str = result.getString(6);			// KEY_EAN
            if (ean_str!=null) {
                String[] m_atc = ean_str.split(";");
                if (m_atc.length>1) {
                    article.setEanCode(m_atc[0]);
                    article.setRegnr(m_atc[1].trim());
                } else if (m_atc.length==1)
                    article.setEanCode(m_atc[0]);
            }
            article.setPharmaCode(result.getString(7));	 	// KEY_PHARMA
            String atc_str = result.getString(8);		    // KEY_ATC
            if (atc_str!=null) {
                String[] m_code = atc_str.split(";");
                if (m_code.length>1) {
                    article.setAtcCode(m_code[0]);
                    article.setAtcClass(m_code[1].trim());
                } else if (m_code.length==1)
                    article.setAtcCode(m_code[0]);
            }
            article.setTherapyCode(result.getString(9)); 	// KEY_THERAPY
            article.setItemsOnStock(result.getInt(10));	 	// KEY_STOCK
            article.setRoseBasisPrice(result.getString(11)); // KEY_PRICE (= Rose Basis Preis)
            article.setAvailability(result.getString(12));	// KEY_AVAIL
            article.setSupplier(result.getString(13));		// KEY_SUPPLIER
            article.setLikes(result.getInt(14));		    // KEY_LIKES
            article.setAuthorGln(result.getString(15));	    // KEY_AUTHOR_GLN (previous: KEY_REPLACE_EAN)
            article.setReplacePharma(result.getString(16));	// KEY_REPLACE_PHARMA
            boolean off_market = result.getBoolean(17);
            if (off_market)
                article.setAvailability("-1");
            article.setFlags(result.getString(18));
            article.setNplArticle(result.getBoolean(19));
            article.setPublicPrice(result.getString(20));    // KEY_PUBLIC_PRICE
            article.setExfactoryPrice(result.getString(21)); // KEY_ROSE_BASIS_PRICE
            article.setCashRebate(0.0f);
        } catch (SQLException e) {
            System.err.println(">> RoseDb: SQLException in cursorToArticle");
        }

        return article;
    }

    private GenericArticle extendedCursorToArticle(ResultSet result, String code) {
        GenericArticle article = new GenericArticle();
        //  KEY_ROWID, KEY_TITLE, KEY_AUTH, KEY_ATC, KEY_PACKAGES
        try {
            article.setId(result.getLong(1));               // KEY_ROWID
            article.setPackTitle(result.getString(2));      // KEY_TITLE
            article.setSupplier(result.getString(3));       // KEY_AUTH
            // Extract ATC Code
            String atc_code_str = result.getString(4);      // KEY_ATCCODE
            String a[] = atc_code_str.split(";");
            if (a.length>0)
                article.setAtcCode(a[0]);
            String pack_info = result.getString(5);         // KEY_PACK_INFO
            String packages = result.getString(6);          // KEY_PACKAGES
            // Extract row which contains 'code'
            String packs[] = packages.split("\n");          // rows of type str|str|str|....
            int row = 0;
            for (String p : packs) {
                if (p.contains(code)) {
                    String s[] = p.split("\\|");
                    if (s.length>10) {
                        String pack_title = s[0];
                        article.setPackTitle(capitalizeFully(pack_title, 1));
                        // Parse units and dosage/size from title
                        String unit = parseUnitFromTitle(pack_title);
                        int size = parseSizeFromTitle(pack_title);
                        article.setPackUnit(unit);
                        article.setPackSize(Integer.toString(size));
                        article.setEanCode(s[9]);
                        article.setPharmaCode(s[10]);
                        article.setPublicPrice(s[4]);
                        break;
                    }
                }
                row++;
            }
            // Extracts row'th row from pack info string
            String info[] = pack_info.split("\n");
            if (info.length>=row) {
                String r = info[row];
                String m = r.substring(r.indexOf("[")+1, r.indexOf("]"));
                if (m.endsWith(", O]"))
                    article.setFlags(m);
                else
                    article.setFlags(m);
            }
            // Set availability to RED (= not on stock!)
            article.setAvailability("xx.xx.2153");

        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in cursorToShortMedi");
        }

        return article;
    }

    private String capitalizeFully(String s, int N) {
        // Split string
        String[] tokens = s.split("\\s");
        // Capitalize only first word!
        tokens[0] = tokens[0].toUpperCase();
        // Reassemble string
        String full_s = "";
        if (tokens.length > 1) {
            for (int i = 0; i < tokens.length - 1; i++) {
                full_s += (tokens[i] + " ");
            }
            full_s += tokens[tokens.length - 1];
        } else {
            full_s = tokens[0];
        }
        return full_s;
    }
}


