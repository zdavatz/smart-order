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

package models;

import com.maxl.java.shared.User;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by maxl on 26.06.2016.
 */
public class RoseData {

    private static final String ROSE_DIR = "./rose/";

    private HashMap<String, User> rose_user_map;
    private HashMap<String, Float> rose_sales_figs_map;
    private ArrayList<String> rose_autogenerika_list;

    public HashMap<String, User> user_map() {
        return this.rose_user_map;
    }

    public HashMap<String, Float> sales_figs_map() {
        return this.rose_sales_figs_map;
    }

    public ArrayList<String> autogenerika_list() {
        return this.rose_autogenerika_list;
    }

    public void loadAllFiles() {
        rose_user_map = loadRoseUserMap(ROSE_DIR + "rose_conditions.ser.clear");
        rose_sales_figs_map = loadRoseSalesFigures(ROSE_DIR + "rose_sales_fig.ser.clear");
        rose_autogenerika_list = loadRoseAutoGenerika(ROSE_DIR + "rose_autogenerika.ser.clear");

        // Test
        /*
        for (Map.Entry<String, User> entry : rose_user_map.entrySet()) {
            String id = entry.getKey();
            User user = entry.getValue();
            System.out.println(user.gln_code);
        }
        rose_autogenerika_list.forEach((String a) -> System.out.println(a));
        */
    }

    /**
     * Loads Rose users
     * Format: gln code -> user
     * @param ser_file_name
     * @return user map
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, User> loadRoseUserMap(String ser_file_name) {
        HashMap<String, User> user_map = new HashMap<String, User>();
        byte[] serialized_object = FileOps.readBytesFromFile(ser_file_name);
        if (serialized_object!=null) {
            user_map = (HashMap<String, User>)FileOps.deserialize(serialized_object);
        }
        return user_map;
    }

    /**
     * Loads Rose sales figures
     * Format: pharma code -> sales figure
     * @param ser_file_name
     * @return sales figures map
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, Float> loadRoseSalesFigures(String ser_file_name) {
        HashMap<String, Float> sales_figures_map = new HashMap<String, Float>();
        byte[] serialized_object = FileOps.readBytesFromFile(ser_file_name);
        if (serialized_object!=null) {
            sales_figures_map = (HashMap<String, Float>)FileOps.deserialize(serialized_object);
        }
        return sales_figures_map;
    }

    /**
     * Loads Rose list of autogenerika
     * @param ser_file_name
     * @return
     */
    @SuppressWarnings("unchecked")
    private ArrayList<String> loadRoseAutoGenerika(String ser_file_name) {
        ArrayList<String> auto_generika_list = new ArrayList<String>();
        byte[] serialized_object = FileOps.readBytesFromFile(ser_file_name);
        if (serialized_object!=null) {
            auto_generika_list = (ArrayList<String>)FileOps.deserialize(serialized_object);
        }
        return auto_generika_list;
    }
}
