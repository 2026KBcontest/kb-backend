package com.moveout.kb_backend.mydata.controller;

import com.moveout.kb_backend.mydata.dto.MyDataSnapshotResponse;
import com.moveout.kb_backend.mydata.service.MyDataService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mydata")
@RequiredArgsConstructor
public class MyDataController {

    private final MyDataService myDataService;

    @GetMapping
    public MyDataSnapshotResponse getSnapshot(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return myDataService.getSnapshot(userId);
    }

    @PostMapping("/sync")
    public MyDataSnapshotResponse sync(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return myDataService.sync(userId);
    }
}
