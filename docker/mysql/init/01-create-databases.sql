-- 모듈러 모놀리스의 모듈 경계 = MySQL DATABASE 단위
-- ehrlab_shared는 MYSQL_DATABASE 환경변수로 자동 생성됨, 나머지 3개를 여기서 생성

CREATE DATABASE IF NOT EXISTS ehrlab_hr
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS ehrlab_attendance
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS ehrlab_leave
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- dev 사용자가 4개 DB 모두 접근 가능하도록 권한 부여
-- (MYSQL_USER는 기본적으로 MYSQL_DATABASE만 권한 가짐)
GRANT ALL PRIVILEGES ON ehrlab_shared.*     TO 'dev'@'%';
GRANT ALL PRIVILEGES ON ehrlab_hr.*         TO 'dev'@'%';
GRANT ALL PRIVILEGES ON ehrlab_attendance.* TO 'dev'@'%';
GRANT ALL PRIVILEGES ON ehrlab_leave.*      TO 'dev'@'%';

FLUSH PRIVILEGES;
