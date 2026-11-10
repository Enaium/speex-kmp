import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        mainRun {
            mainClass = "cn.enaium.speex.examples.visual.Main_jvmKt"
        }
    }

    // The same UI source builds into a standalone executable on every desktop
    // target imgui-kmp and sdl-kmp publish klibs for.
    macosArm64 {
        binaries.executable()
    }

    macosX64 {
        binaries.executable()
    }

    linuxX64 {
        binaries.executable()
    }

    mingwX64 {
        binaries.executable()
    }

    compilerOptions {
        // DeviceThread uses an expect/actual class, like the library module.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // This repository disables the automatic hierarchy template in
    // gradle.properties, so materialize the shared native source set here.
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":speex-kmp"))
                // Dear ImGui/ImPlot and the SDL3 window they are driven from.
                // The example synthesises its audio, so no audio device binding
                // is needed and it runs on headless machines too.
                implementation(libs.imgui.kmp)
                implementation(libs.sdl.kmp)
                // Real microphone capture, speaker playback and window replay.
                implementation(libs.audio.io.kmp)
            }
        }
    }
}

// SDL has to own the first thread on macOS. `jvmRun` passes the flag; an IDE
// run configuration has to add it itself (the JVM entry point explains that).
tasks.withType<JavaExec>().configureEach {
    if (OperatingSystem.current().isMacOsX) {
        jvmArgs("--enable-native-access=ALL-UNNAMED", "-XstartOnFirstThread")
    }
}
