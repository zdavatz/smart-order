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

import models.GenericArticle;
import models.RoseArticle;
import models.ShoppingRose;
import play.db.NamedDatabase;
import play.db.Database;
import play.libs.Json;
import play.mvc.*;

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

    private static final String KEY_EAN = "eancode";
    private static final String KEY_PHARMA = "pharmacode";
    private static final String KEY_ATC = "atc";

    private static final String ROSE_DB_TABLE = "rosedb";

    @Inject @NamedDatabase("rose") Database rose_db;

    public Result index() {
        return ok("Hello AmikoRose");
    }

    public Result getGtin(String gtin) {
        CompletableFuture<List<GenericArticle>> articles = CompletableFuture.supplyAsync(()->searchEan(gtin));
        CompletableFuture<List<RoseArticle>> rose_articles = articles
                .thenApply(a -> a.stream().map(this::genericArticleToRose).collect(Collectors.toList()));
        // toJson converts the Java object 'f' to JSON
        return rose_articles.thenApply(f -> ok(Json.prettyPrint(toJson(f)))).join();
    }

    public Result getNumRecords() {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> numRecords());
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

    public Result getSmartBasket(String pretty, int limit, String gln_code, String basket) {
        // gln_code = "7601000234285";
        ShoppingRose shopping_cart = new ShoppingRose(gln_code);

        Pattern p = Pattern.compile("\\((\\d{13}),(\\d+)\\)");
        Matcher m = p.matcher(basket);
        ArrayList<String> list_of_articles = new ArrayList<>();
        HashMap<String, Integer> map_of_articles = new HashMap<>();
        while (m.find()) {
            if (!m.group(1).isEmpty() && !m.group(2).isEmpty()) {
                list_of_articles.add(m.group(1));
                map_of_articles.put(m.group(1), Integer.valueOf(m.group(2)));
            }
        }

        List<GenericArticle> articles = list_of_articles.stream()
                .map(this::searchSingleEan)
                .collect(Collectors.toList());
        if (limit <= 0)
            shopping_cart.setResultsLimit(false);
        if (articles.size() > 0) {
            Map<String, GenericArticle> shopping_basket = new HashMap<>();
            Map<String, List<GenericArticle>> map_of_similar_articles = new HashMap<>();
            // Loop through all articles found
            for (GenericArticle article : articles) {
                String ean = article.getEanCode();
                article.setQuantity(map_of_articles.get(ean));
                shopping_basket.put(ean, article);
                LinkedList<GenericArticle> la = listSimilarArticles(article);
                if (la != null) {
                    // Check if ean code is already part of the map...
                    if (!map_of_similar_articles.containsKey(ean)) {
                        map_of_similar_articles.put(ean, la);
                    }
                }
            }
            // Update shopping basket
            shopping_cart.setShoppingBasket(shopping_basket);
            // Update list of similar articles only for last insert article
            shopping_cart.updateMapSimilarArticles(map_of_similar_articles);
        }

        List<RoseArticle> list_of_rose_articles = shopping_cart.updateShoppingCart();

        if (pretty.equals("on"))
            return ok(Json.prettyPrint(toJson(list_of_rose_articles)));
        else
            return ok(toJson(list_of_rose_articles));
    }

    private RoseArticle genericArticleToRose(GenericArticle generic_article) {
        RoseArticle rose_article = new RoseArticle();

        rose_article.setGtin(generic_article.getEanCode());
        rose_article.setPharma(generic_article.getPharmaCode());
        rose_article.setTitle(generic_article.getPackTitle());
        rose_article.setSupplier(generic_article.getSupplier());
        rose_article.setQuantity(1);
        rose_article.setSwissmed(generic_article.getFlags());
        rose_article.setRosePrice(generic_article.getExfactoryPriceAsFloat());
        rose_article.setPublicPrice(generic_article.getPublicPriceAsFloat());
        rose_article.setCashRebate(0.0f);
        rose_article.setPreferences("");
        rose_article.setShippingStatus("");
        rose_article.alternatives = new LinkedList<>();

        return rose_article;
    }

    private RoseArticle getAlternatives(GenericArticle generic_article) {
        RoseArticle rose_article = genericArticleToRose(generic_article);

        LinkedList<RoseArticle> list_of_similar_articles = listSimilarArticles(generic_article).stream()
                .map(this::genericArticleToRose)
                .collect(Collectors.toCollection(LinkedList::new));
        rose_article.alternatives = list_of_similar_articles;

        return rose_article;
    }

    private LinkedList<GenericArticle> listSimilarArticles(GenericArticle article) {
        LinkedList<GenericArticle> list_a = new LinkedList<>();

        String atc_code = article.getAtcCode();
        String size = article.getPackSize();
        String unit = article.getPackUnit();
        if (atc_code!=null && !atc_code.equals("k.A.")) {
            for (GenericArticle a : searchATC(atc_code)) {
                if (!a.getAtcCode().equals("k.A.")) {
                    String s = a.getPackSize().toLowerCase();
                    String u = a.getPackUnit().toLowerCase();
                    if (!article.isOffMarket()) {
                        if (!a.isOffMarket()) {
                            // Make sure that articles added to the list are NOT off-the-market
                            if ((size.contains(s) || s.contains(size)) && (unit.contains(u) || u.contains(unit)) )
                                list_a.add(a);
                        }
                    } else {
                        // If the main article is off the market, get some replacements...
                        u = u.replaceAll("[^A-Za-z]","");
                        unit = unit.replaceAll("[^A-Za-z]","");
                        // System.out.println(a.getPackTitle() + " -> " + a.getAvailability() + " | " + s + "=" + size + " | " + u + "=" + unit);
                        if (u.equals(unit) && s.equals(size) && !a.isOffMarket())
                            list_a.add(a);
                    }
                }
            }
        }
        // If "Ersatzartikel" exists, add it to list
        String replace_pharma_code = article.getReplacePharma();
        if (replace_pharma_code!=null && !replace_pharma_code.isEmpty()) {
            // Check if article is already in list
            for (GenericArticle a : list_a) {
                if (a.getPharmaCode().equals(replace_pharma_code))
                    return list_a;
            }
            List<GenericArticle> replace_article = searchEan(replace_pharma_code);
            if (replace_article.size()>0)
                list_a.add(replace_article.get(0));
        }
        return list_a;
    }

    private int numRecords() {
        int num_rec = -1;
        try {
            Connection conn = rose_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select count(*) from " + ROSE_DB_TABLE;
            ResultSet rs = stat.executeQuery(query);
            num_rec = rs.getInt(1);
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in numRecords");
        }
        return num_rec;
    }

    private List<GenericArticle> searchEan(String code) {
        List<GenericArticle> list_of_articles = new ArrayList<>();

        System.out.println("search ean = " + code);

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

    private GenericArticle searchSingleEan(String code) {
        GenericArticle article = new GenericArticle();

        try {
            Connection conn = rose_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select * from " + ROSE_DB_TABLE + " where "
                    + KEY_EAN + " like " + "'" + code + "%' or "
                    + KEY_EAN + " like " + "'%;" + code + "%' or "
                    + KEY_PHARMA + " like " + "'" + code + "%'";
            ResultSet rs = stat.executeQuery(query);
            if (rs.next()) {
                article = cursorToArticle(rs);
            }
            conn.close();
        } catch(SQLException e) {
            System.err.println(">> RoseSqlDb: SQLException in searchEan!");
        }

        return article;
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

    private GenericArticle cursorToArticle(ResultSet result) {
        GenericArticle genericArticle = new GenericArticle();

        try {
            genericArticle.setId(result.getLong(1));			 	// KEY_ROWID
            genericArticle.setPackTitle(result.getString(2));	 	// KEY_TITLE
            genericArticle.setPackSize(String.valueOf(result.getInt(3)));	// KEY_SIZE
            genericArticle.setPackGalen(result.getString(4));	 	// KEY_GALEN
            genericArticle.setPackUnit(result.getString(5));	 	// KEY_UNIT
            String ean_str = result.getString(6);			        // KEY_EAN
            if (ean_str!=null) {
                String[] m_atc = ean_str.split(";");
                if (m_atc.length>1) {
                    genericArticle.setEanCode(m_atc[0]);
                    genericArticle.setRegnr(m_atc[1].trim());
                } else if (m_atc.length==1)
                    genericArticle.setEanCode(m_atc[0]);
            }
            genericArticle.setPharmaCode(result.getString(7));	 	// KEY_PHARMA
            String atc_str = result.getString(8);			        // KEY_ATC
            if (atc_str!=null) {
                String[] m_code = atc_str.split(";");
                if (m_code.length>1) {
                    genericArticle.setAtcCode(m_code[0]);
                    genericArticle.setAtcClass(m_code[1].trim());
                } else if (m_code.length==1)
                    genericArticle.setAtcCode(m_code[0]);
            }
            genericArticle.setTherapyCode(result.getString(9)); 	// KEY_THERAPY
            genericArticle.setItemsOnStock(result.getInt(10));	 	// KEY_STOCK
            genericArticle.setExfactoryPrice(result.getString(11)); // KEY_PRICE (= Rose Basis Preis)
            genericArticle.setAvailability(result.getString(12));	// KEY_AVAIL
            genericArticle.setSupplier(result.getString(13));		// KEY_SUPPLIER
            genericArticle.setLikes(result.getInt(14));			    // KEY_LIKES
            genericArticle.setReplaceEan(result.getString(15));	    // KEY_REPLACE_EAN
            genericArticle.setReplacePharma(result.getString(16));	// KEY_REPLACE_PHARMA
            boolean off_market = result.getBoolean(17);
            if (off_market)
                genericArticle.setAvailability("-1");				// -1 -> not on the market anymore!
            genericArticle.setFlags(result.getString(18));
            genericArticle.setPublicPrice(result.getString(20));
        } catch (SQLException e) {
            System.err.println(">> RoseDb: SQLException in cursorToArticle");
        }

        return genericArticle;
    }
}


