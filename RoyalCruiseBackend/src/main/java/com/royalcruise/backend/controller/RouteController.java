/** Ez a Java fájl a backendhez kapcsolódó logikát és viselkedést tartalmazza. */
package com.royalcruise.backend.controller;

import com.royalcruise.backend.model.RouteItem;
import com.royalcruise.backend.service.CruiseDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
// Ez a controller HTTP vegpontokat ad, bemeneti kerest fogad es a megfelelo service/repository logikara delegal.
public class RouteController {

    private final CruiseDataService cruiseDataService;

    public RouteController(CruiseDataService cruiseDataService) {
        this.cruiseDataService = cruiseDataService;
    }

    @GetMapping
    public List<RouteItem> getAllRoutes() {
        // 1) Az összes útvonal visszaadása a fennmaradó helyekkel együtt.
        return cruiseDataService.getAllRoutes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteItem> getRouteById(@PathVariable String id) {
        // 1) Egyetlen útvonal visszaadása, vagy 404 ha nincs ilyen rekord.
        return cruiseDataService.getRouteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<RouteItem> searchRoutes(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String routeName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String shipType
    ) {
        // 1) A keresési paramétereket egy szolgáltatásrétegen keresztül szűrt útvonalakká alakítjuk.
        return cruiseDataService.searchRoutes(destination, routeName, startDate, endDate, guests, minPrice, maxPrice, shipType);
    }

    @GetMapping("/destinations")
    public List<String> getDestinations() {
        // 1) Az elérhető célállomások lista visszaadása szűrőopciókhoz.
        return cruiseDataService.getDestinations();
    }

    @GetMapping("/route-groups")
    public List<String> getRouteGroupsByDestination(@RequestParam String destination) {
        // 1) Az adott célállomáshoz tartozó útvonalnevek visszaadása.
        return cruiseDataService.getRouteGroupsForDestination(destination);
    }
}

