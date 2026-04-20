plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.stardazz.smeeting.core.llm"
    compileSdk = 35

    defaultConfig {
        minSdk = 27

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-fexceptions", "-O2", "-fopenmp")
                arguments.addAll(listOf(
                    "-DANDROID_CPP_FEATURES=exceptions",
                    "-DANDROID_STL=c++_shared",
                    "-DOpenMP_C_FLAGS=-fopenmp",
                    "-DOpenMP_CXX_FLAGS=-fopenmp",
                    "-DCMAKE_SHARED_LINKER_FLAGS=-static-openmp",
                ))
                abiFilters.add("arm64-v8a")
                abiFilters.add("armeabi-v7a")
            }
        }

        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
