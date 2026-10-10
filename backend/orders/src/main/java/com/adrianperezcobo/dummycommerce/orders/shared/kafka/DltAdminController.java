package com.adrianperezcobo.dummycommerce.orders.shared.kafka;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/kafka/dlt")
public class DltAdminController {
    public record ReprocessRequest(@NotBlank String topic, @NotNull @PositiveOrZero Integer partition, @NotNull @PositiveOrZero Long offset) {}
    private final DltReprocessor reprocessor;

    public DltAdminController(DltReprocessor reprocessor) {
        this.reprocessor = reprocessor;
    }

    @PostMapping("/reprocess")
    public DltReprocessor.Result reprocess(@Valid @RequestBody ReprocessRequest request) {
        return reprocessor.reprocess(request.topic(), request.partition(), request.offset());
    }
}
