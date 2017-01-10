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

package com.maxl.java.shared;

import java.util.LinkedHashMap;

public class User implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public String gln_code = "";
    public String ideale_id = "";
    public String xpris_id = "";
    public String addr_type = "";	// S: shipping, B: billing, O: Office
    public String category = "";	// arzt, spital, drogerie, ...
    public String title = "";
    public String first_name = "";
    public String last_name = "";
    public String name1 = "";		// company name 1
    public String name2 = "";		// company name 2
    public String name3 = "";		// company name 3
    public String street = "";		// street / pobox
    public String number = "";
    public String zip = "";
    public String city = "";
    public String country = "";
    public String phone = "";
    public String fax = "";
    public String email = "";
    public String owner = "";		// owner[0]=i -> IBSA, owner[1]=d -> Desitin, etc...
    public boolean selbst_disp = false;
    public boolean bet_mittel = false;
    public boolean is_human = true;
    public boolean top_customer = false;    // Topkunde FLAG (zur Rose)

    public String sap_id = "";
    public String specialities = "";
    public String capabilities = "";
    public String bm_type = "";
    public String status = "A";		// Default: Aktiv

    // Spezialrabatt
    public float generics_rebate = 0.0f;
    // Map of GLN to list of company-specific rebate
    public LinkedHashMap<String, Float> rebate_map = null;
    // Map of GLN to list of company-specific expenses
    public LinkedHashMap<String, Float> expenses_map = null;
    // Map of GLN to list of company-specific delivery and logistic costs
    public LinkedHashMap<String, Float> dlk_map = null;

    public User() {
        // Struct-like class... 'nough said.
    }
}
