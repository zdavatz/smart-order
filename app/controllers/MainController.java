/*
Copyright (c) 2016 ML <cybrmx@gmail.com>

This file is part of AmikoRose.

AmiKoWeb is free software: you can redistribute it and/or modify
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

import models.Article;
import models.Constants;
import models.Medication;
import play.db.NamedDatabase;
import play.db.Database;
import play.mvc.*;
import views.html.*;
import play.data.FormFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

    public Result getEan(String ean) {
        CompletableFuture<List<Article>> articles = CompletableFuture.supplyAsync(()->searchEan(ean));
        return articles.thenApply(f -> ok(toJson(f))).join();
    }

    public Result getNumRecords() {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> numRecords());
        return future.thenApply(f -> ok(String.format("Num records = %d\n", f))).join();
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

    private List<Article> searchEan(String code) {
        List<Article> list_of_articles = new ArrayList<>();

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

    public List<Article> searchATC(String atccode) {
        List<Article> list_of_articles = new ArrayList<>();

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

    private Article cursorToArticle(ResultSet result) {
        Article article = new Article();

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
            String atc_str = result.getString(8);			// KEY_ATC
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
            article.setExfactoryPrice(result.getString(11));// KEY_PRICE (= Rose Basis Preis)
            article.setAvailability(result.getString(12));	// KEY_AVAIL
            article.setSupplier(result.getString(13));		// KEY_SUPPLIER
            article.setLikes(result.getInt(14));			// KEY_LIKES
            article.setReplaceEan(result.getString(15));	// KEY_REPLACE_EAN
            article.setReplacePharma(result.getString(16));	// KEY_REPLACE_PHARMA
            boolean off_market = result.getBoolean(17);
            if (off_market)
                article.setAvailability("-1");				// -1 -> not on the market anymore!
            article.setFlags(result.getString(18));
        } catch (SQLException e) {
            System.err.println(">> RoseDb: SQLException in cursorToArticle");
        }

        return article;
    }
}


