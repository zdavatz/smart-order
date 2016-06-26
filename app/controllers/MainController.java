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

    public static final String KEY_ROWID = "_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_AUTH = "auth";
    public static final String KEY_ATCCODE = "atc";
    public static final String KEY_SUBSTANCES = "substances";
    public static final String KEY_REGNRS = "regnrs";
    public static final String KEY_ATCCLASS = "atc_class";
    public static final String KEY_THERAPY = "tindex_str";
    public static final String KEY_APPLICATION = "application_str";
    public static final String KEY_INDICATIONS = "indications_str";
    public static final String KEY_CUSTOMER_ID = "customer_id";
    public static final String KEY_PACK_INFO = "pack_info_str";
    public static final String KEY_ADDINFO = "add_info_str";
    public static final String KEY_PACKAGES = "packages";

    private static final String DATABASE_TABLE = "amikodb";

    /**
     * Table columns used for fast queries
     */
    private static final String SHORT_TABLE = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
            KEY_ROWID, KEY_TITLE, KEY_AUTH, KEY_ATCCODE, KEY_SUBSTANCES, KEY_REGNRS,
            KEY_ATCCLASS, KEY_THERAPY, KEY_APPLICATION, KEY_INDICATIONS,
            KEY_CUSTOMER_ID, KEY_PACK_INFO, KEY_ADDINFO, KEY_PACKAGES);

    @Inject @NamedDatabase("german") Database german_db;
    @Inject @NamedDatabase("french") Database french_db;

    public Result index() {
        return ok();
    }

    private Result retrieveFachinfo(String lang, Medication m) {
        if (m!=null) {
            String content = m.getContent().replaceAll("<html>|</html>|<body>|</body>|<head>|</head>", "");
            String[] titles = getSectionTitles(lang, m);
            String[] section_ids = m.getSectionIds().split(",");
            String name = m.getTitle();
            String titles_html;
            titles_html = "<ul style=\"list-style-type:none;\n\">";
            for (int i = 0; i < titles.length; ++i) {
                if (i < section_ids.length)
                    titles_html += "<li><a onclick=\"move_to_anchor('" + section_ids[i] + "')\">" + titles[i] + "</a></li>";
            }
            titles_html += "</ul>";
            // Text-based HTTP response, default encoding: utf-8
            if (content != null) {
                return ok();
            }
        }
        return ok("Hasta la vista, baby! You just terminated me.");
    }

    private String[] getSectionTitles(String lang, Medication m) {
        // Get section titles from chapters
        String[] section_titles = m.getSectionTitles().split(";");
        // Use abbreviations...
        String[] section_titles_abbr = lang.equals("de") ? models.Constants.SectionTitle_DE : Constants.SectionTitle_FR;
        for (int i = 0; i < section_titles.length; ++i) {
            for (String s : section_titles_abbr) {
                String titleA = section_titles[i].replaceAll(" ", "");
                String titleB = m.getTitle().replaceAll(" ", "");
                // Are we analysing the name of the article?
                if (titleA.toLowerCase().contains(titleB.toLowerCase())) {
                    if (section_titles[i].contains("®"))
                        section_titles[i] = section_titles[i].substring(0, section_titles[i].indexOf("®") + 1);
                    else
                        section_titles[i] = section_titles[i].split(" ")[0].replaceAll("/-", "");
                    break;
                } else if (section_titles[i].toLowerCase().contains(s.toLowerCase())) {
                    section_titles[i] = s;
                    break;
                }
            }
        }
        return section_titles;
    }

    private int numRecords(String lang) {
        int num_rec = -1;
        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select count(*) from amikodb";
            ResultSet rs = stat.executeQuery(query);
            num_rec = rs.getInt(1);
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in numRecords");
        }
        return num_rec;
    }

    private List<Medication> searchName(String lang, String name) {
        List<Medication> med_titles = new ArrayList<>();

        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            ResultSet rs;
            // Allow for search to start inside a word...
            if (name.length()>2) {
                String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
                        + KEY_TITLE + " like " + "'" + name + "%' or "
                        + KEY_TITLE + " like " + "'%" + name + "%'";
                rs = stat.executeQuery(query);
            } else {
                String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
                        + KEY_TITLE + " like " + "'" + name + "%'";
                rs = stat.executeQuery(query);
            }
            if (rs!=null) {
                while (rs.next()) {
                    med_titles.add(cursorToShortMedi(rs));
                }
            }
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in searchName for "+name);
        }

        return med_titles;
    }

    private List<Medication> searchOwner(String lang, String owner) {
        List<Medication> med_auth = new ArrayList<>();

        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE
                    + " where " + KEY_AUTH + " like " + "'" + owner + "%'";
            ResultSet rs = stat.executeQuery(query);
            if (rs!=null) {
                while (rs.next()) {
                    med_auth.add(cursorToShortMedi(rs));
                }
            }
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in searchOwner for " + owner);
        }

        return med_auth;
    }

    private List<Medication> searchATC(String lang, String atccode) {
        List<Medication> med_auth = new ArrayList<>();

        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
                    + KEY_ATCCODE + " like " + "'%;" + atccode + "%' or "
                    + KEY_ATCCODE + " like " + "'" + atccode + "%' or "
                    + KEY_ATCCODE + " like " + "'% " + atccode + "%' or "
                    + KEY_ATCCLASS + " like " + "'" + atccode + "%' or "
                    + KEY_ATCCLASS + " like " + "'%;" + atccode + "%' or "
                    + KEY_ATCCLASS + " like " + "'%#" + atccode + "%' or "
                    + KEY_SUBSTANCES + " like " + "'%, " + atccode + "%' or "
                    + KEY_SUBSTANCES + " like " + "'" + atccode + "%'";
            ResultSet rs = stat.executeQuery(query);
            if (rs!=null) {
                while (rs.next()) {
                    med_auth.add(cursorToShortMedi(rs));
                }
            }
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in searcATC!");
        }

        return med_auth;
    }

    private List<Medication> searchRegnr(String lang, String regnr) {
        List<Medication> med_auth = new ArrayList<>();

        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
                    + KEY_REGNRS + " like " + "'%, " + regnr + "%' or "
                    + KEY_REGNRS + " like " + "'" + regnr + "%'";
            ResultSet rs = stat.executeQuery(query);
            if (rs!=null) {
                while (rs.next()) {
                    med_auth.add(cursorToShortMedi(rs));
                }
            }
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in searchRegnr!");
        }

        return med_auth;
    }

    private List<Medication> searchTherapy(String lang, String application) {
        List<Medication> med_auth = new ArrayList<>();

        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select " + SHORT_TABLE + " from " + DATABASE_TABLE + " where "
                    + KEY_APPLICATION + " like " + "'%," + application + "%' or "
                    + KEY_APPLICATION + " like " + "'" + application + "%' or "
                    + KEY_APPLICATION + " like " + "'%" + application + "%' or "
                    + KEY_APPLICATION + " like " + "'% " + application + "%' or "
                    + KEY_APPLICATION + " like " + "'%;" + application + "%' or "
                    + KEY_THERAPY + " like " + "'" + application + "%' or "
                    + KEY_INDICATIONS + " like " + "'" + application + "%' or "
                    + KEY_INDICATIONS + " like " + "'%;" + application + "%'";
            ResultSet rs = stat.executeQuery(query);
            if (rs!=null) {
                while (rs.next()) {
                    med_auth.add(cursorToShortMedi(rs));
                }
            }
            conn.close();
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in searchTherapy!");
        }

        return med_auth;
    }

    private Medication getMedicationWithId(String lang, long rowId) {
        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            String query = "select * from " + DATABASE_TABLE + " where " + KEY_ROWID + "=" + rowId;
            ResultSet rs = stat.executeQuery(query);
            Medication m = cursorToMedi(rs);
            conn.close();
            if (m!=null)
                return m;
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in getContentWithId!");
        }
        return null;
    }

    private Medication getMedicationWithEan(String lang, String eancode) {
        try {
            Connection conn = lang.equals("de") ? german_db.getConnection() : french_db.getConnection();
            Statement stat = conn.createStatement();
            String search_key = KEY_PACKAGES;
            if (eancode.length()==5)
                search_key = KEY_REGNRS;
            String query = "select * from " + DATABASE_TABLE + " where " + search_key + " like " + "'%" + eancode + "%'";
            ResultSet rs = stat.executeQuery(query);
            Medication m = cursorToMedi(rs);
            conn.close();
            if (m!=null)
                return m;
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in getContentWithId!");
        }
        return null;
    }

    private Medication cursorToShortMedi(ResultSet result) {
        Medication medi = new Medication();
        try {
            medi.setId(result.getLong(1));              // KEY_ROWID
            medi.setTitle(result.getString(2));         // KEY_TITLE
            medi.setAuth(result.getString(3));          // KEY_AUTH
            medi.setAtcCode(result.getString(4));       // KEY_ATCCODE
            medi.setSubstances(result.getString(5));    // KEY_SUBSTANCES
            medi.setRegnrs(result.getString(6));        // KEY_REGNRS
            medi.setAtcClass(result.getString(7));      // KEY_ATCCLASS
            medi.setTherapy(result.getString(8));       // KEY_THERAPY
            medi.setApplication(result.getString(9));   // KEY_APPLICATION
            medi.setIndications(result.getString(10));  // KEY_INDICATIONS
            medi.setCustomerId(result.getInt(11));      // KEY_CUSTOMER_ID
            medi.setPackInfo(result.getString(12));     // KEY_PACK_INFO
            medi.setAddInfo(result.getString(13));      // KEY_ADD_INFO
            medi.setPackages(result.getString(14));     // KEY_PACKAGES
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in cursorToShortMedi");
        }
        return medi;
    }

    private Medication cursorToMedi(ResultSet result) {
        Medication medi = new Medication();
        try {
            medi.setId(result.getLong(1));              // KEY_ROWID
            medi.setTitle(result.getString(2));         // KEY_TITLE
            medi.setAuth(result.getString(3));          // KEY_AUTH
            medi.setAtcCode(result.getString(4));       // KEY_ATCCODE
            medi.setSubstances(result.getString(5));    // KEY_SUBSTANCES
            medi.setRegnrs(result.getString(6));        // KEY_REGNRS
            medi.setAtcClass(result.getString(7));      // KEY_ATCCLASS
            medi.setTherapy(result.getString(8));       // KEY_THERAPY
            medi.setApplication(result.getString(9));   // KEY_APPLICATION
            medi.setIndications(result.getString(10));  // KEY_INDICATIONS
            medi.setCustomerId(result.getInt(11));      // KEY_CUSTOMER_ID
            medi.setPackInfo(result.getString(12));     // KEY_PACK_INFO
            medi.setAddInfo(result.getString(13));      // KEY_ADD_INFO
            medi.setSectionIds(result.getString(14));   // KEY_SECTION_IDS
            medi.setSectionTitles(result.getString(15)); // KEY_SECTION_TITLES
            medi.setContent(result.getString(16));      // KEY_CONTENT
            // KEY_STYLE... (ignore)
            medi.setPackages(result.getString(18)); // KEY_PACKAGES
        } catch (SQLException e) {
            System.err.println(">> SqlDatabase: SQLException in cursorToMedi");
        }
        return medi;
    }
}

