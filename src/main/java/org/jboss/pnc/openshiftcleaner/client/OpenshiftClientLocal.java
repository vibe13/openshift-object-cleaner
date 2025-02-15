package org.jboss.pnc.openshiftcleaner.client;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

@ApplicationScoped
@Slf4j
public class OpenshiftClientLocal {

    @Inject
    OpenShiftClient client;

    public List<String> cleanServices(long intervalDays, String query) {

        List<String> deletedResources = new LinkedList<>();

        List<Service> resources = client.services()
                .list()
                .getItems()
                .stream()
                .filter(a -> a.getMetadata().getName().contains(query))
                .collect(Collectors.toList());

        for (Service service : resources) {
            log.info("Processing service: {}", service.getMetadata().getName());
            LocalDate dateCreated = parseTimestamp(service.getMetadata().getCreationTimestamp());
            long days = dayDuration(dateCreated);

            if (days > intervalDays) {
                log.info("Deleting service: {}, {} days old", service.getMetadata().getName(), days);
                client.services().delete(service);
                deletedResources.add("service:" + service.getMetadata().getName());
            }
        }

        return deletedResources;
    }

    public List<String> cleanRoutes(long intervalDays, String query) {

        List<String> deletedResources = new LinkedList<>();

        List<Route> resources = client.routes()
                .list()
                .getItems()
                .stream()
                .filter(a -> a.getMetadata().getName().contains(query))
                .collect(Collectors.toList());

        for (Route route : resources) {
            log.info("Processing route: {}", route.getMetadata().getName());
            LocalDate dateCreated = parseTimestamp(route.getMetadata().getCreationTimestamp());
            long days = dayDuration(dateCreated);

            if (days > intervalDays) {
                log.info("Deleting route: {}, {} days old", route.getMetadata().getName(), days);
                client.routes().delete(route);
                deletedResources.add("route:" + route.getMetadata().getName());
            }
        }

        return deletedResources;
    }

    public List<String> cleanPods(long intervalDays, String query) {

        List<String> deletedResources = new LinkedList<>();

        List<Pod> resources = client.pods()
                .list()
                .getItems()
                .stream()
                .filter(a -> a.getMetadata().getName().contains(query))
                .collect(Collectors.toList());

        for (Pod pod : resources) {
            log.info("Processing pod: {}", pod.getMetadata().getName());
            LocalDate dateCreated = parseTimestamp(pod.getMetadata().getCreationTimestamp());
            long days = dayDuration(dateCreated);

            if (days > intervalDays) {
                log.info("Deleting pod: {}, {} days old", pod.getMetadata().getName(), days);
                client.pods().delete(pod);
                deletedResources.add("pod:" + pod.getMetadata().getName());
            }
        }

        return deletedResources;
    }

    /**
     * Given a timestamp, return a LocaleDate object
     * <p>
     * timestamp in the format: 2016-08-16T15:23:01Z
     *
     * @param timestamp: timestamp
     * @return LocaleDate object
     */
    private LocalDate parseTimestamp(String timestamp) {
        Instant instant = Instant.parse(timestamp);
        LocalDateTime result = LocalDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()));
        return result.toLocalDate();
    }

    private long dayDuration(LocalDate start) {
        LocalDate today = LocalDate.now();
        return DAYS.between(start, today);
    }

}
