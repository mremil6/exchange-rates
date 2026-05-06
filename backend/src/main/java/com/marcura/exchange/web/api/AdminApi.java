package com.marcura.exchange.web.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Tag(name = "Admin", description = "Operational endpoints")
@RequestMapping("/admin")
public interface AdminApi {

    @Operation(summary = "Manually trigger a rate fetch (idempotent)")
    @PostMapping("/refresh")
    Map<String, Object> refresh();
}
