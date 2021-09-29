plugins {
    java
    `maven-publish`
}

group = "cn.lnkdoc.expand"
version = "2021.0"

dependencies {
    // https://mvnrepository.com/artifact/com.wgzhao.datax/datax-common
    implementation("com.alibaba.datax:datax-common:0.0.1-SNAPSHOT")
    // https://mvnrepository.com/artifact/net.sourceforge.javacsv/javacsv
    implementation("net.sourceforge.javacsv:javacsv:2.0")
    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:23.0")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-compress
    implementation("org.apache.commons:commons-compress:1.21")


    // https://mvnrepository.com/artifact/javacsv/javacsv
    //implementation("javacsv:javacsv:1.0")


    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:1.7.32")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    testImplementation("ch.qos.logback:logback-classic:1.2.6")

    // https://mvnrepository.com/artifact/commons-beanutils/commons-beanutils
    implementation("commons-beanutils:commons-beanutils:1.9.4")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.0")
}

//仓库配置
repositories {
    mavenLocal { setUrl("file://${project.rootDir}/lib") }
    //首先去本地仓库找
    mavenLocal()
    //然后去阿里仓库找
    // build.gradle:
    // maven { url "http://maven.aliyun.com/nexus/content/groups/public/" }

    // build.gradle.kts:
    maven { url = uri("https://repo.spring.io/release") }
    maven {
        isAllowInsecureProtocol = true
        setUrl("https://maven.aliyun.com/nexus/content/groups/public/")
    }
    maven {
        isAllowInsecureProtocol = true
        url = uri("https://maven.aliyun.com/repository/public") }
    maven {
        isAllowInsecureProtocol = true
        url = uri("https://maven.aliyun.com/repository/google") }
    maven {
        isAllowInsecureProtocol = true
        url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    maven {
        isAllowInsecureProtocol = true
        url = uri("https://maven.aliyun.com/repository/spring-plugin") }
    maven {
        isAllowInsecureProtocol = true
        url = uri("https://maven.aliyun.com/repository/apache-snapshots") }
    maven {
        isAllowInsecureProtocol = true
        url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/") }
    maven {
        isAllowInsecureProtocol = true
        url = uri("https://www.ebi.ac.uk/intact/maven/nexus/content/repositories/public/") }
    google()
    //jcenter()
    //最后从maven中央仓库找
    mavenCentral()
}

// 指定上传的路径
val localMavenRepo = "file://" + File(System.getProperty("user.home", ".m2/repository")).absolutePath

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}