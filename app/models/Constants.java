/*
Copyright (c) 2016 ML <cybrmx@gmail.com>

This file is part of AmikoRose.

AmiKoRose is free software: you can redistribute it and/or modify
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

import java.util.LinkedHashMap;
import java.util.Map;

public class Constants {
    static public final String ROSE_DIR = "/rose/";
    static public final String LOG_DIR = "/orders/";

    static public final Map<String, Integer> doctorPreferences;
    static {
        // LinkedHashMap preserves insertion order
        doctorPreferences = new LinkedHashMap<>();
        doctorPreferences.put("helvepharm", 1);	// helvepharm ag, 7601001003736
        doctorPreferences.put("mepha", 2);		// mepha schweiz ag, 7601001396685
        doctorPreferences.put("sandoz", 3);		// sandoz pharmaceuticals ag, 7601001029439
        doctorPreferences.put("actavis", 4);	// actavis switzerland ag, 7601001376618
        doctorPreferences.put("spirig", 5);		// spirig healthcare ag, 7601001394834
    }

    static public final Map<String, Integer> rosePreferences;
    static {
        // LinkedHashMap preserves insertion order
        rosePreferences = new LinkedHashMap<>();
        rosePreferences.put("helvepharm", 1);	// helvepharm
        rosePreferences.put("mepha", 2);		// mepha schweiz ag
        //rosePreferences.put("teva", 3);			// teva pharma ag
        //rosePreferences.put("sanofi", 4);		// zentiva (Sanofi)
        //rosePreferences.put("sandoz", 5);		// sandoz pharmaceuticals ag
    }
}
