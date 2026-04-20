package dev.gukin.ehrlab.shared.id;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Ids {
  public static UUID generate() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
