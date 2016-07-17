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

package myactors;

import akka.actor.Props;
import akka.actor.UntypedActor;
import models.Constants;
import models.FileOps;
import models.RoseOrder;

/**
 * Created by maxl on 15.07.2016.
 */
public class OrderLogActor extends UntypedActor {

    // The Props object describes how to create an actor
    public static Props props = Props.create(OrderLogActor.class);

    String m_log_dir = "";

    public OrderLogActor() {
        m_log_dir = System.getProperty("user.dir") + Constants.LOG_DIR;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        RoseOrder ro = (RoseOrder)message;
        String glncode = ro.getGlncode();
        FileOps.appendToFile(ro.getOrderCSV(), m_log_dir + glncode, "order_log_" + glncode + ".csv");
    }
}
