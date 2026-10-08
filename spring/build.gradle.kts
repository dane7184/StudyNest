plugins {
    java
    // Spring Boot / dependency-management plugins come from buildSrc's classpath;
    // declaring them here with a version causes "already on the classpath with an unknown version".
}

allprojects {
    group = "com.client.lms"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
