/** Ez a Java fájl a backendhez kapcsolódó logikát és viselkedést tartalmazza. */
package com.royalcruise.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.royalcruise.backend.model.Cabin;
import com.royalcruise.backend.model.RouteItem;
import com.royalcruise.backend.repository.CabinRepository;
import com.royalcruise.backend.repository.RouteItemRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.text.Normalizer;
import java.util.stream.Collectors;

@Service
// Ez a service osztaly uzleti szabalyokat futtat, validal es az adat-hozzaferesi reteget hivja.
public class CruiseDataService {

    private final CabinRepository cabinRepository;
    private final RouteItemRepository routeItemRepository;

    public CruiseDataService(
            ObjectMapper objectMapper,
            CabinRepository cabinRepository,
            RouteItemRepository routeItemRepository
    ) {
        // 1) Repository referenciák eltárolása a későbbi szinkron és lekérdezési műveletekhez.
        this.cabinRepository = cabinRepository;
        this.routeItemRepository = routeItemRepository;

        // 2) A seed adatfájlokból induláskor frissítjük a kabin- és útvonal-táblákat.
        syncCabinsFromSeed(objectMapper);
        syncRoutesFromSeed(objectMapper);
    }

        private void syncCabinsFromSeed(ObjectMapper objectMapper) {
        // 1) A JSON seedből betöltjük az összes kabin sorát.
        List<Cabin> seedCabins = loadFromJson(objectMapper, "data/cabins.json", new TypeReference<>() {});

        // 2) Soronként vagy beszúrjuk, vagy a meglévő rekord mezőit frissítjük.
        for (Cabin seedCabin : seedCabins) {
            Optional<Cabin> existing = cabinRepository.findById(seedCabin.getId());
            if (existing.isEmpty()) {
                cabinRepository.save(seedCabin);
                continue;
            }

            Cabin current = existing.get();
            current.setName(seedCabin.getName());
            current.setType(seedCabin.getType());
            current.setCapacity(seedCabin.getCapacity());
            current.setPricePerNight(seedCabin.getPricePerNight());
            current.setDescription(seedCabin.getDescription());
            current.setImage(seedCabin.getImage());
            current.setAmenities(seedCabin.getAmenities());
            cabinRepository.save(current);
        }
    }

        private void syncRoutesFromSeed(ObjectMapper objectMapper) {
        // 1) A JSON seedből betöltjük az összes útvonalat.
        List<RouteItem> seedRoutes = loadFromJson(objectMapper, "data/routes.json", new TypeReference<>() {});

        // 2) Soronként beszúrjuk az új útvonalat, vagy visszafrissítjük a meglévő mezőket.
        for (RouteItem seedRoute : seedRoutes) {
            Optional<RouteItem> existing = routeItemRepository.findById(seedRoute.getId());
            if (existing.isEmpty()) {
                routeItemRepository.save(seedRoute);
                continue;
            }

            RouteItem current = existing.get();
            current.setName(seedRoute.getName());
            current.setImage(seedRoute.getImage());
            current.setDescription(seedRoute.getDescription());
            current.setStops(seedRoute.getStops());
            current.setDate(seedRoute.getDate());
            current.setPrice(seedRoute.getPrice());
            current.setDestination(seedRoute.getDestination());
            current.setRouteName(seedRoute.getRouteName());
            current.setAvailableSeats(seedRoute.getAvailableSeats());
            routeItemRepository.save(current);
        }
    }

        private <T> T loadFromJson(ObjectMapper objectMapper, String resourcePath, TypeReference<T> typeReference) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException exception) {
            throw new IllegalStateException("Nem sikerült betölteni az adatfájlt: " + resourcePath, exception);
        }
    }

        public List<Cabin> getAllCabins() {
        // 1) A teljes kabinlista közvetlen lekérdezése a repositoryból.
        return cabinRepository.findAll();
    }

        public Optional<Cabin> getCabinById(String id) {
        // 1) Egyetlen kabin kiválasztása azonosító alapján.
        return cabinRepository.findById(id);
    }

        public List<Cabin> searchCabins(Integer guests) {
        // 1) A vendégszám alapján kiszűrjük azokat a kabinokat, amelyek elég nagyok.
        int minGuests = guests == null ? 1 : guests;
        return cabinRepository.findAll().stream().filter(c -> c.getCapacity() >= minGuests).toList();
    }

        public List<RouteItem> getAllRoutes() {
        // 1) Az összes útvonal visszaadása tárolt állapotban.
        return routeItemRepository.findAll();
    }

        public Optional<RouteItem> getRouteById(String id) {
        // 1) Az adott útvonalat azonosító alapján visszaadjuk.
        return routeItemRepository.findById(id);
    }

        public List<String> getDestinations() {
        // 1) Az útvonalakból kigyűjtjük az egyedi célállomásokat.
        return routeItemRepository.findAll().stream()
                .map(RouteItem::getDestination)
                .distinct()
                .toList();
    }

        public List<String> getRouteGroupsForDestination(String destination) {
        // 1) Az adott célállomáshoz tartozó útvonalneveket szűrjük ki.
        return routeItemRepository.findAll().stream()
                .filter(r -> r.getDestination().equals(destination))
                .map(RouteItem::getRouteName)
                .distinct()
                .toList();
    }

        public List<RouteItem> searchRoutes(String destination, String routeName, String startDate, String endDate, Integer guests) {
        // 1) Az összes útvonalból indulunk ki, majd egymás után alkalmazzuk a klasszikus szűrőket.
        List<RouteItem> filtered = new ArrayList<>(getAllRoutes());

        if (destination != null && !destination.isBlank()) {
            filtered = filtered.stream().filter(r -> r.getDestination().equals(destination)).collect(Collectors.toList());
        }

        if (routeName != null && !routeName.isBlank()) {
            filtered = filtered.stream().filter(r -> r.getRouteName().equals(routeName)).collect(Collectors.toList());
        }

        if (startDate != null && !startDate.isBlank()) {
            LocalDate start = LocalDate.parse(startDate);
            filtered = filtered.stream().filter(r -> !LocalDate.parse(r.getDate()).isBefore(start)).collect(Collectors.toList());
        }

        if (endDate != null && !endDate.isBlank()) {
            LocalDate end = LocalDate.parse(endDate);
            filtered = filtered.stream().filter(r -> !LocalDate.parse(r.getDate()).isAfter(end)).collect(Collectors.toList());
        }

        if (guests != null && guests > 0) {
            filtered = filtered.stream()
                    .filter(r -> r.getAvailableSeats() >= guests)
                    .collect(Collectors.toList());
        }

        return filtered.stream()
                .sorted(Comparator.comparing(RouteItem::getDate))
                .toList();
    }

    public List<RouteItem> searchRoutes(
            String destination,
            String routeName,
            String startDate,
            String endDate,
            Integer guests,
            Integer minPrice,
            Integer maxPrice,
            String shipType
    ) {
        // 1) Az alap szűrés eredményére ár- és hajótípus-szűrést rétegezünk.
        List<RouteItem> filtered = searchRoutes(destination, routeName, startDate, endDate, guests)
                .stream()
                .filter(route -> minPrice == null || route.getPrice() >= minPrice)
                .filter(route -> maxPrice == null || route.getPrice() <= maxPrice)
                .filter(route -> shipType == null || shipType.isBlank() || shipType.equals(getShipNameForRoute(route)))
                .toList();

        return filtered.stream()
                .sorted(Comparator.comparing(RouteItem::getDate))
                .toList();
    }

        private String getShipNameForRoute(RouteItem route) {
        // 1) Az útvonal szöveges tartalmából kulcsszavak alapján hajónevet választunk.
        String text = collectRouteText(route);

        if (hasKeyword(text, HORIZON_KEYWORDS)) return SHIPS.HORIZON;
        if (hasKeyword(text, SERENITA_KEYWORDS)) return SHIPS.SERENITA;
        if (hasKeyword(text, AURORA_KEYWORDS)) return SHIPS.AURORA;

        return SHIPS.AURORA;
    }

        private String collectRouteText(RouteItem route) {
        // 1) Az útvonal több mezőjét egyetlen normalizált szöveggé fűzzük össze.
        if (route == null) return "";

        List<String> parts = new ArrayList<>();
        if (route.getName() != null) parts.add(route.getName());
        if (route.getDestination() != null) parts.add(route.getDestination());
        if (route.getRouteName() != null) parts.add(route.getRouteName());
        if (route.getStops() != null) parts.addAll(route.getStops());

        return normalizeText(String.join(" ", parts));
    }

        private boolean hasKeyword(String text, List<String> keywords) {
        // 1) Végigmegyünk a kulcsszólistán, és már az első találatnál igazat adunk vissza.
        for (String keyword : keywords) {
            if (text.contains(normalizeText(keyword))) {
                return true;
            }
        }
        return false;
    }

        private String normalizeText(String value) {
        // 1) Az ékezeteket eltávolítjuk, majd kisbetűsítjük a stabil összehasonlításhoz.
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .toLowerCase(java.util.Locale.ROOT);
    }

    // Ez a service osztaly uzleti szabalyokat futtat, validal es az adat-hozzaferesi reteget hivja.
    private static final class SHIPS {
        private static final String AURORA = "RC Aurora";
        private static final String HORIZON = "RC Horizon";
        private static final String SERENITA = "RC Serenita";

                private SHIPS() {
        }
    }

    private static final List<String> HORIZON_KEYWORDS = List.of(
            "koppenhaga",
            "oslo",
            "bergen",
            "stavanger",
            "fjord",
            "balti",
            "eszaki",
            "scandin",
            "stockholm",
            "helsinki"
    );

    private static final List<String> SERENITA_KEYWORDS = List.of(
            "velence",
            "dubrovnik",
            "santorini",
            "mykonos",
            "korfu",
            "athen",
            "adria",
            "görög",
            "gorog",
            "greece"
    );

    private static final List<String> AURORA_KEYWORDS = List.of(
            "barcelona",
            "marseille",
            "roma",
            "napoly",
            "mallorca",
            "mediterran",
            "foldkozi",
            "földközi",
            "sicilia",
            "szicilia",
            "capri"
    );
}

