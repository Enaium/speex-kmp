/*
 * Per-OS/arch JNI artifact: linux-x86_64.
 * Ships libspeex_jni.so as a classpath resource at
 * /cn/enaium/speex/native/linux-x86_64/, which NativeLoader
 * (in :speex-kmp's jvmMain) extracts and System.load()s at runtime.
 */
import org.gradle.internal.os.OperatingSystem

plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
}

group = rootProject.group
version = rootProject.version

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val jniOs = "linux"
val jniArch = "x86_64"
val classifier = "$jniOs-$jniArch"
val libFile = "libspeex_jni.so"
val resourceDir = "cn/enaium/speex/native/$classifier"

val canBuildHere = OperatingSystem.current().isLinux

val cmakeExecutable: String = rootProject.extra["cmakeExecutable"] as String

val nativeOutputDir = layout.buildDirectory.dir("jni-native/$classifier")
val cmakeBuildDir = layout.buildDirectory.dir("cmake-jni/$classifier")

val configureJniLibrary = tasks.register<Exec>("configureJniLibrary") {
    group = "build"
    description = "cmake-configures speex_jni for $classifier."
    onlyIf { canBuildHere }
    val outDir = nativeOutputDir.get().asFile
    val buildDir = cmakeBuildDir.get().asFile
    doFirst {
        outDir.mkdirs()
        buildDir.mkdirs()
    }
    workingDir = buildDir
    val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
    val jniInclude = if (javaHome.isNotEmpty()) "$javaHome/include" else ""
    // A runner that is not itself x86_64 needs the cross toolchain CI installs;
    // without the flags CMake would write a host-arch binary into an artifact
    // that promises another architecture.
    val crossFlags = if (System.getProperty("os.arch").lowercase() in listOf("x86_64", "amd64")) {
        emptyList()
    } else {
        listOf(
            "-DCMAKE_SYSTEM_NAME=Linux",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_C_COMPILER=x86_64-linux-gnu-gcc",
        )
    }
    commandLine(
        listOf(
            cmakeExecutable,
            rootProject.file("jni").absolutePath,
            "-DCMAKE_BUILD_TYPE=Release",
            "-DBUILD_JNI=ON",
            "-DJNI_INCLUDE_DIR=$jniInclude",
            "-DJNI_INCLUDE_DIR_PLATFORM=$jniInclude/linux",
            "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${outDir.absolutePath}",
            "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${outDir.absolutePath}",
        ) + crossFlags,
    )
}

val buildJniLibrary = tasks.register<Exec>("buildJniLibrary") {
    group = "build"
    description = "Builds $libFile for $classifier."
    onlyIf { canBuildHere }
    dependsOn(configureJniLibrary)
    workingDir = cmakeBuildDir.get().asFile
    commandLine(cmakeExecutable, "--build", ".", "--target", "speex_jni", "--config", "Release")
    inputs.files(
        rootProject.file("jni/CMakeLists.txt"),
        rootProject.file("jni/config.h.in"),
        rootProject.file("jni/speex_kmp.c"),
        rootProject.file("jni/speexdsp_kmp.c"),
        rootProject.file("jni/include/speex_kmp.h"),
        rootProject.file("jni/include/speexdsp_kmp.h"),
        rootProject.file("jni/include/speex_config_types.h"),
        rootProject.file("jni/include/speexdsp_config_types.h"),
        rootProject.file("jni/speex_jni.c"),
        rootProject.file("jni/speexdsp_jni.c"),
    )
    inputs.dir(rootProject.file("speex/libspeex"))
    inputs.dir(rootProject.file("speex/include"))
    inputs.dir(rootProject.file("speexdsp/libspeexdsp"))
    inputs.dir(rootProject.file("speexdsp/include"))
    outputs.file(nativeOutputDir.map { it.file(libFile) })
}

tasks.named<Copy>("processResources") {
    dependsOn(buildJniLibrary)
    from(nativeOutputDir.map { it.file(libFile) }) {
        into(resourceDir)
    }
}

val hasSigningKey = rootProject.extra["hasSigningKey"] as Boolean

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (hasSigningKey) signAllPublications()
    coordinates(
        groupId = rootProject.group.toString(),
        artifactId = "speex-kmp-jni-jvm-$classifier",
        version = rootProject.version.toString(),
    )
    pom {
        name.set("speex-kmp-jni-jvm-$classifier")
        description.set(
            "Prebuilt JNI shared library for speex-kmp on $jniOs/$jniArch. " +
                "Loaded automatically by NativeLoader; not intended to be depended on directly.",
        )
        url.set("https://github.com/Enaium/speex-kmp")
        inceptionYear.set("2026")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Enaium")
            }
        }
        scm {
            url.set("https://github.com/Enaium/speex-kmp")
            connection.set("scm:git:git@github.com:Enaium/speex-kmp.git")
            developerConnection.set("scm:git:git@github.com:Enaium/speex-kmp.git")
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/Enaium/speex-kmp/issues")
        }
    }
}
