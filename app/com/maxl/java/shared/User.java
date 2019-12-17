/*
Copyright (c) 2019, ywesee GmbH, created by b123400 <i@b123400.net>

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

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import models.GenericArticle;

public class User {

    public String city;
    public String gln_code;
    public String name1;
    public HashMap<String, Integer> neu_map;
    public String street;
    public String zip;

    public User() {
    }

    public boolean isPreferenceEmpty() {
        return this.getPreferences().isEmpty();
    }

    public Integer getPreferenceCount() {
        return this.getPreferences().size();
    }

    public Set<String> getPreferences() {
         return neu_map
            .keySet()
            .stream()
            .filter(k -> neu_map.getOrDefault(k, 0) == 1)
            .collect(Collectors.toSet());
    }

    public boolean isEanPreferred(String ean_code) {
        String name = GenericArticle.eanNameMap.getOrDefault(ean_code, null);
        if (name == null) {
            return false;
        }
        return this.getPreferences().contains(name);
    }
}
