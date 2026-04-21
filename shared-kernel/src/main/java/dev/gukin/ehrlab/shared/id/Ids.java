package dev.gukin.ehrlab.shared.id;

import com.github.f4b6a3.uuid.UuidCreator;
import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Ids {

  public static UUID generate() {
    return UuidCreator.getTimeOrderedEpoch();
  }

  public static byte[] toBytes(UUID uuid) {
    if (uuid == null) return null;
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  public static UUID fromBytes(byte[] bytes) {
    if (bytes == null) return null;
    ByteBuffer bb = ByteBuffer.wrap(bytes); // byte[]를 읽기 모드 buffer로
    long mostSig = bb.getLong();
    long leastSig = bb.getLong();
    return new UUID(mostSig, leastSig);
  }
}
