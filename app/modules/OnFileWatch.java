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

package modules;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import com.google.inject.Singleton;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Created by maxl on 26.06.2016.
 */
@Singleton
public class OnFileWatch {

    @Inject
    public OnFileWatch(ActorSystem system) {

        // Get reference to FileWatchActor
        ActorRef fileWatchActor = system.actorOf(FileWatchActor.props);

        // 'cancellable' can be used to cancel the execution of the scheduled operation
        Cancellable cancellable = system.scheduler().schedule(
                Duration.create(10, TimeUnit.SECONDS),   // Initial delay 0 milliseconds
                Duration.create(1, TimeUnit.MINUTES),   // Frequency 1 minute
                fileWatchActor,
                "tick",
                system.dispatcher(),
                null
        );
    }
}
