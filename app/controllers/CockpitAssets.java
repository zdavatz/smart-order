/*
Copyright (c) 2020, ywesee GmbH, created by b123400 <i@b123400.net>

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

import java.io.*;
import play.mvc.*;

public class CockpitAssets extends Controller {

    public Result at(String filePath) {
        try {
            String baseDir = System.getProperty("user.dir");
            File directory = new File(baseDir, "cockpit");
            File targetFile = new File(directory.getAbsolutePath(), filePath);

            if (!targetFile.getCanonicalPath().startsWith(directory.getCanonicalPath())) {
                return Results.forbidden();
            }
            if (!targetFile.isFile()) {
                return Results.notFound();
            }
            return ok(targetFile, true);
        } catch (IOException e) {
            return Results.notFound();
        }
    }
}