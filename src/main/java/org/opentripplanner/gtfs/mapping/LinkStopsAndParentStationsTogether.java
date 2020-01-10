package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ParentStationNotFound;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.EntityById;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/**
 * Links child stops with parent stations by adding bidirectional object references.
 */
class LinkStopsAndParentStationsTogether {
    private final EntityById<FeedScopedId, Station> otpStations;
    private final EntityById<FeedScopedId, Stop> otpStops;

    private final DataImportIssueStore issueStore;

    private static Logger LOG = LoggerFactory.getLogger(LinkStopsAndParentStationsTogether.class);

    LinkStopsAndParentStationsTogether(
            EntityById<FeedScopedId, Station> stations,
            EntityById<FeedScopedId, Stop> stops,
            DataImportIssueStore issueStore
    ) {
        this.otpStations = stations;
        this.otpStops = stops;
        this.issueStore = issueStore;
    }

    void link(Collection<org.onebusaway.gtfs.model.Stop> gtfsStops) {
        for (org.onebusaway.gtfs.model.Stop gtfsStop : gtfsStops) {
            if (gtfsStop.getLocationType() == 0 && gtfsStop.getParentStation() != null) {

                Stop otpStop = getOtpStop(gtfsStop);
                Station otpStation = getOtpParentStation(gtfsStop);

                if (otpStation == null) {
                    issueStore.add(
                            new ParentStationNotFound(
                                    otpStop,
                                    gtfsStop.getParentStation()
                            )
                    );
                    continue;
                }

                otpStop.setParentStation(otpStation);
                otpStation.addChildStop(otpStop);
            }
        }
    }

    private Station getOtpParentStation(org.onebusaway.gtfs.model.Stop stop) {
        FeedScopedId otpParentStationId = new FeedScopedId(stop.getId().getAgencyId(), stop.getParentStation());
        return otpStations.get(otpParentStationId);
    }

    private Stop getOtpStop(org.onebusaway.gtfs.model.Stop stop) {
        FeedScopedId otpStopId = mapAgencyAndId(stop.getId());
        return otpStops.get(otpStopId);
    }
}