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
import models.RoseData;

import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by maxl on 27.06.2016.
 */
public class FileWatchActor extends UntypedActor {

    private static final String ROSE_DIR = "./rose/";

    private WatchService _watcher;

    // The Props object describes how to create an actor
    public static Props props = Props.create(FileWatchActor.class);

    public FileWatchActor() {
        Path dir = Paths.get(ROSE_DIR);
        try {
            _watcher = FileSystems.getDefault().newWatchService();
            dir.register(_watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (java.io.IOException e) {
            return;
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        WatchKey key = _watcher.poll();
        if (key != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                // Get event type
                WatchEvent.Kind<?> kind = event.kind();
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path file_name = ev.context();
                @SuppressWarnings("unchecked")
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                // Filter...
                if (kind == OVERFLOW) {
                    // Do nothing...
                } else if (kind == ENTRY_DELETE) {
                    // process delete event
                } else if (kind == ENTRY_MODIFY || kind==ENTRY_CREATE) {
                    // process modify event
                    Thread.sleep(1000);
                    /*
                    System.out.println("Re-loading all rose files... " + dateFormat.format(date));
                    RoseData.getInstance().loadAllFiles();
                    */
                    RoseData.getInstance().loadFile(file_name.toString());
                    key.reset();
                }
            }
        }
    }
}
