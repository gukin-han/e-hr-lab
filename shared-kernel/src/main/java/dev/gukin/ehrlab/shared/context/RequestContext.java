package dev.gukin.ehrlab.shared.context;

import dev.gukin.ehrlab.shared.id.Ids;
import java.util.Objects;
import java.util.UUID;

public record RequestContext(UUID tenantId, UUID traceId) {
  public RequestContext {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(traceId, "traceId is required");
  }

  public static RequestContext of(UUID tenantId, UUID traceId) {
    return new RequestContext(tenantId, traceId);
  }

  public static RequestContext of(UUID tenantId) {
    return new RequestContext(tenantId, Ids.generate());
  }
}
