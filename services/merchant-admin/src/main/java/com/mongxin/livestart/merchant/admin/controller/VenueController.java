package com.mongxin.livestart.merchant.admin.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.VenueDO;
import com.mongxin.livestart.merchant.admin.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-admin/venue")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping("/create")
    public Result<Void> createVenue(@RequestBody VenueDO requestParam) {
        venueService.createVenue(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<VenueDO>> listVenues() {
        return Results.success(venueService.listAllVenues());
    }

    @PutMapping("/update")
    public Result<Void> updateVenue(@RequestBody VenueDO requestParam) {
        venueService.updateVenue(requestParam);
        return Results.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteVenue(@PathVariable("id") Long id) {
        venueService.deleteVenue(id);
        return Results.success();
    }
}
