package com.entitleflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

record CustomerRequest(@NotBlank @Size(max=160) String name,@NotBlank @Email String email,@Size(max=160) String company) {}
record AssetRequest(@NotBlank String assetTag,@NotBlank String serialNumber,@NotBlank String product,@NotBlank String model,LocalDate warrantyExpiry) {}
record OrderItemRequest(@NotBlank String product,@Min(1) @Max(100) int quantity,@NotBlank String licenseType) {}
record OrderRequest(@NotNull Long customerId,@NotEmpty List<@Valid OrderItemRequest> items) {}
record DiagnosticRequest(@NotBlank @Size(max=2000) String question) {}
