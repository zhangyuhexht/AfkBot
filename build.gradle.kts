plugins {
    java
    // Paper 官方构建工具：提供 NMS（服务端内部代码）编译支持
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}

group = "com.afkbot"
version = "1.0.0"
description = "挂机假人插件 - 管理员可创建/移除假人，不占用服务器人数上限"

java {
    // Minecraft 26.3 需要 JDK 25
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
}

dependencies {
    // 开发套件自带 paper-api 与完整服务端内部代码（Mojang 映射）
    paperweight.paperDevBundle("26.3.build.+")
}

// Minecraft 26.1+ 服务端不再混淆，产物直接使用 Mojang 映射，无需重混淆
paperweight.reobfArtifactConfiguration =
    io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    processResources {
        // 变量替换（plugin.yml 中的 ${version}）
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
        filteringCharset = "UTF-8"
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    jar {
        archiveBaseName.set("AfkBot")
    }
}
