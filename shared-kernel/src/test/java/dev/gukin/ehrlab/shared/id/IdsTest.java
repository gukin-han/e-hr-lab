package dev.gukin.ehrlab.shared.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("식별자 스모크 테스트")
class IdsTest {

  @Test
  @DisplayName("바이트 변환 결과가 동일하다")
  void toBytes_fromBytes_equals() {
    UUID original = Ids.generate();

    UUID actual = Ids.fromBytes(Ids.toBytes(original));

    assertThat(actual).isEqualTo(original);
  }

  @Test
  @DisplayName("null이 주어졌을때 null을 반환한다")
  void shouldReturnNull_whenNullGiven() {
    UUID uuid = Ids.fromBytes(null);
    byte[] bytes = Ids.toBytes(null);

    assertAll(
        () -> assertThat(uuid).isNull(),
        () -> assertThat(bytes).isNull()
    );
  }

  @Test
  @DisplayName("식별자는 시간순서대로 생성된다")
  void generate_shouldGenerateInTimeOrder() throws InterruptedException {
    UUID uuid1 = Ids.generate();
    Thread.sleep(2);
    UUID uuid2 = Ids.generate();

    assertThat(uuid1).isLessThan(uuid2);
  }
}