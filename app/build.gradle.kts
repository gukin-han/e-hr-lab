plugins {
    id("ehrlab.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":modules:hr"))
    implementation(project(":modules:attendance"))
    implementation(project(":modules:leave"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.testcontainers:mysql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
}
