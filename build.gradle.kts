import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.TextReportRenderer
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.CHECKCAST
import org.objectweb.asm.Opcodes.INVOKESTATIC

buildscript {
    dependencies {
        classpath(libs.android.gradlePlugin)
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dependencyLicenseReport)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val licenseResDir = "$projectDir/build/dependency-license-res"

// ---------------------------------------------------------------------------
// Release signing (see RELEASING.md)
//
// Values are read from environment variables first, then from the (gitignored)
// local.properties file. If any value is missing, or the keystore file does not
// exist, the release build is left unsigned instead of failing, so debug builds
// and CI jobs without secrets keep working.
// ---------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun releaseSigningValue(envName: String, propertyName: String): String? =
    providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseKeystorePath = releaseSigningValue("POCKETLINK_KEYSTORE_PATH", "pocketlink.keystore.path")
val releaseKeystorePassword = releaseSigningValue("POCKETLINK_KEYSTORE_PASSWORD", "pocketlink.keystore.password")
val releaseKeyAlias = releaseSigningValue("POCKETLINK_KEY_ALIAS", "pocketlink.key.alias")
val releaseKeyPassword = releaseSigningValue("POCKETLINK_KEY_PASSWORD", "pocketlink.key.password")
val releaseKeystoreFile = releaseKeystorePath?.let { file(it) }
val hasReleaseSigning = releaseKeystoreFile?.isFile == true &&
    releaseKeystorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

if (!hasReleaseSigning && gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
    logger.warn(
        "PocketLink: release signing is not configured (POCKETLINK_KEYSTORE_PATH, POCKETLINK_KEYSTORE_PASSWORD, " +
            "POCKETLINK_KEY_ALIAS, POCKETLINK_KEY_PASSWORD). The release APK will be UNSIGNED. See RELEASING.md."
    )
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

base {
    archivesName.set("com.pocketlink")
}

android {
    namespace = "org.kde.kdeconnect_tp"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.pocketlink"
        minSdk = 23
        targetSdk = 37
        versionCode = 100
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    sourceSets.getByName("main") {
        res.directories += licenseResDir
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources {
            merges += listOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.md", "META-INF/NOTICE")
        }
    }
    signingConfigs {
        getByName("debug") {
            val localKeystore = file("debug.keystore")
            if (localKeystore.exists()) {
                storeFile = localKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true // minSdk 23 < 24, so keep the JAR signature for Android 6
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            // Left null when no release key is configured -> AGP produces an unsigned APK.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}


tasks.withType<Test>().configureEach {
    // Objenesis 3.4 (transitive from mockk) contains an obsolete META-INF/INDEX.LIST
    // that triggers an OpenJDK 17 URLClassPath bug (resolved in JDK 18+ via JDK-8273401).
    // Under JDK 18+, JAR indexing in URLClassPath is disabled by default.
    // Under JDK 17, strip INDEX.LIST from objenesis to prevent InvalidJarIndexError.
    doFirst {
        val testJvmVersion = javaLauncher.orNull?.metadata?.languageVersion?.asInt()?.let {
            JavaVersion.toVersion(it)
        } ?: JavaVersion.current()

        if (testJvmVersion < JavaVersion.VERSION_18) {
            val cleanJarsDir = layout.buildDirectory.dir("clean-test-jars").get().asFile
            val sanitized = classpath.files.map { file ->
                if (file.isFile && file.name.startsWith("objenesis-") && file.name.endsWith(".jar")) {
                    val hasIndex = try {
                        ZipFile(file).use { zip -> zip.getEntry("META-INF/INDEX.LIST") != null }
                    } catch (e: Exception) {
                        false
                    }
                    if (hasIndex) {
                        val normalizedPath = file.toPath().toAbsolutePath().normalize().toString()
                        val digest = MessageDigest.getInstance("SHA-256")
                        digest.update(normalizedPath.toByteArray(Charsets.UTF_8))
                        file.inputStream().use { stream ->
                            val buf = ByteArray(8192)
                            var r: Int
                            while (stream.read(buf).also { r = it } != -1) {
                                digest.update(buf, 0, r)
                            }
                        }
                        val cacheKey = digest.digest().joinToString("") { "%02x".format(it) }.take(24)
                        val dest = File(cleanJarsDir, "$cacheKey-${file.name}")
                        val isUpToDate = dest.exists() && dest.length() > 0L
                        if (!isUpToDate) {
                            dest.parentFile.mkdirs()
                            val destTmp = File(cleanJarsDir, "$cacheKey-${file.name}.tmp")
                            ZipFile(file).use { zipIn ->
                                ZipOutputStream(destTmp.outputStream()).use { zipOut ->
                                    for (entry in zipIn.entries().asSequence()) {
                                        if (entry.name != "META-INF/INDEX.LIST") {
                                            zipOut.putNextEntry(ZipEntry(entry.name))
                                            zipIn.getInputStream(entry).copyTo(zipOut)
                                            zipOut.closeEntry()
                                        }
                                    }
                                }
                            }
                            if (dest.exists()) {
                                dest.delete()
                            }
                            destTmp.renameTo(dest)
                        }
                        dest
                    } else {
                        file
                    }
                } else {
                    file
                }
            }
            classpath = files(sanitized)
        }
    }
}


/**
 * Fix PosixFilePermission class type check issue.
 *
 * It fixed the class cast exception when lib desugar enabled and minSdk < 26.
 */
abstract class FixPosixFilePermissionClassVisitorFactory :
    AsmClassVisitorFactory<FixPosixFilePermissionClassVisitorFactory.Params> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return object : ClassVisitor(instrumentationContext.apiVersion.get(), nextClassVisitor) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                if (name == "attributesToPermissions") { // org.apache.sshd.sftp.common.SftpHelper.attributesToPermissions
                    return object : MethodVisitor(
                        instrumentationContext.apiVersion.get(),
                        super.visitMethod(access, name, descriptor, signature, exceptions)
                    ) {
                        override fun visitTypeInsn(opcode: Int, type: String?) {
                            // We need to prevent Android Desugar modifying the `PosixFilePermission` classname.
                            //
                            // Android Desugar will replace `CHECKCAST java/nio/file/attribute/PosixFilePermission`
                            // to `CHECKCAST j$/nio/file/attribute/PosixFilePermission`.
                            // We need to replace it with `CHECKCAST java/lang/Enum` to prevent Android Desugar from modifying it.
                            if (opcode == CHECKCAST && type == "java/nio/file/attribute/PosixFilePermission") {
                                println("Bypass PosixFilePermission type check success.")
                                // `Enum` is the superclass of `PosixFilePermission`.
                                // Due to `Object` is not the superclass of `Enum`, we need to use `Enum` instead of `Object`.
                                super.visitTypeInsn(opcode, "java/lang/Enum")
                            } else {
                                super.visitTypeInsn(opcode, type)
                            }
                        }
                    }
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return (classData.className == "org.apache.sshd.sftp.common.SftpHelper").also {
            if (it) println("SftpHelper Found! Instrumenting...")
        }
    }

    interface Params : InstrumentationParameters
}

/**
 * Collections.unmodifiableXXX is not exist when Android API level is lower than 26.
 * So we replace the call to Collections.unmodifiableXXX with the original collection by removing the call.
 */
abstract class FixCollectionsClassVisitorFactory :
    AsmClassVisitorFactory<FixCollectionsClassVisitorFactory.Params> {
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return object : ClassVisitor(instrumentationContext.apiVersion.get(), nextClassVisitor) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                return object : MethodVisitor(
                    instrumentationContext.apiVersion.get(),
                    super.visitMethod(access, name, descriptor, signature, exceptions)
                ) {
                    override fun visitMethodInsn(
                        opcode: Int,
                        type: String?,
                        name: String?,
                        descriptor: String?,
                        isInterface: Boolean
                    ) {
                        val backportClass = "org/kde/kdeconnect/helpers/CollectionsBackport"

                        if (opcode == INVOKESTATIC && type == "java/util/Collections") {
                            val replaceRules = mapOf(
                                "unmodifiableNavigableSet" to "(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;",
                                "unmodifiableSet" to "(Ljava/util/Set;)Ljava/util/Set;",
                                "unmodifiableNavigableMap" to "(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;",
                                "emptyNavigableMap" to "()Ljava/util/NavigableMap;")
                            if (name in replaceRules && descriptor == replaceRules[name]) {
                                super.visitMethodInsn(opcode, backportClass, name, descriptor, isInterface)
                                val calleeClass = classContext.currentClassData.className
                                println("Replace Collections.$name call with CollectionsBackport.$name from $calleeClass success.")
                                return
                            }
                        }
                        super.visitMethodInsn(opcode, type, name, descriptor, isInterface)
                    }
                }
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className.startsWith("org.apache.sshd") // We only need to fix the Apache SSHD library
    }

    interface Params : InstrumentationParameters
}

ksp {
    arg("com.albertvaka.classindexksp.annotations", "org.kde.kdeconnect.plugins.PluginFactory.LoadablePlugin")
}

androidComponents {
    onVariants { variant ->
        variant.instrumentation.transformClassesWith(
            FixPosixFilePermissionClassVisitorFactory::class.java,
            InstrumentationScope.ALL
        ) { }
        variant.instrumentation.transformClassesWith(
            FixCollectionsClassVisitorFactory::class.java,
            InstrumentationScope.ALL
        ) { }

        // When the "Generate Signed APK/Bundle" wizard is used, copy the source map to the output directory
        val variantName = variant.name
        val capitalized = variantName.replaceFirstChar { it.uppercase() }
        // The 'android.injected.apk.location' property is only set when using the wizard
        val apkLocation = providers.gradleProperty("android.injected.apk.location")
        // Plain task with doLast (no declared outputs) so we don't clash with AGP tasks that also write into the
        // destination folder (e.g. createReleaseApkListingFileRedirect writing output-metadata.json).
        val mappingFile = layout.buildDirectory.file("outputs/mapping/$variantName/mapping.txt")
        val nativeSymbolsFile = layout.buildDirectory.file("outputs/native-debug-symbols/$variantName/native-debug-symbols.zip")
        val destDir = apkLocation.map { File(it, variantName) }
        val copyExtras = tasks.register("copySigningExtraOutputs$capitalized") {
            description = "Copies R8 mapping.txt and native-debug-symbols.zip next to the signed $variantName APK/bundle."
            onlyIf { apkLocation.isPresent }
            doLast {
                val dest = destDir.get().apply { mkdirs() }
                val mapping = mappingFile.get().asFile
                if (mapping.exists()) mapping.copyTo(File(dest, "mapping.txt"), overwrite = true)
                val symbols = nativeSymbolsFile.get().asFile
                if (symbols.exists()) symbols.copyTo(File(dest, "native-debug-symbols.zip"), overwrite = true)
            }
        }
        tasks.matching { it.name == "assemble$capitalized" || it.name == "bundle$capitalized" }
            .configureEach { finalizedBy(copyExtras) }
    }
}

dependencies {
    // It has a bug that causes a crash when using PosixFilePermission and minSdk < 26.
    // It has been used in SSHD Core.
    // We have taken a workaround to fix it.
    // See `FixPosixFilePermissionClassVisitorFactory` for more details.
    coreLibraryDesugaring(libs.android.desugarJdkLibsNio)

    // Compose
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.media)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // For ViewModel
    implementation(libs.androidx.lifecycle.livedata.ktx) // For LiveData
    implementation(libs.androidx.lifecycle.process) // For ProcessLifecycleOwner
    implementation(libs.androidx.gridlayout)
    implementation(libs.google.android.material)
    implementation(libs.coil.core) //For loading and caching album art
    implementation(libs.coil.network.okhttp)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.handroid)

    implementation(libs.apache.sshd.core)
    implementation(libs.apache.sshd.sftp)
    implementation(libs.apache.sshd.scp)
    implementation(libs.apache.sshd.mina)
    implementation(libs.apache.mina.core)

    implementation(libs.bcpkix.jdk18on) //For SSL certificate generation

    ksp(libs.classindexksp)

    // The android-smsmms library is the only way I know to handle MMS in Android
    // (Shouldn't a phone OS make phone things easy?)
    // This library was originally authored as com.klinkerapps at https://github.com/klinker41/android-smsmms.
    // However, that version is under-loved. I have therefore made "some fixes" and published it.
    // Please see https://invent.kde.org/sredman/android-smsmms/-/tree/master
    implementation(libs.android.smsmms)
    implementation(libs.logger)

    implementation(libs.univocity.parsers)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.slf4j.simple) // do not try to use the Android logger backend in tests
    testImplementation(libs.jsonassert)
    testImplementation(libs.robolectric) {
        exclude(group = "org.robolectric", module = "nativeruntime-dist-compat")
    }
    testImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // For device controls
    implementation(libs.reactive.streams)
    implementation(libs.rxjava)
}

licenseReport {
    configurations = arrayOf("releaseRuntimeClasspath", "coreLibraryDesugaring")
    renderers = arrayOf<ReportRenderer>(TextReportRenderer())
}

tasks.named("generateLicenseReport") {
    val outputFile = file("$licenseResDir/raw/license")
    val inputFiles = files(
        layout.projectDirectory.file("COPYING"),
        layout.buildDirectory.file("reports/dependency-license/THIRD-PARTY-NOTICES.txt")
    )
    outputs.file(outputFile)
    doLast {
        outputFile.apply {
            parentFile.mkdirs()
            writeText(inputFiles.joinToString(separator = "\n") { it.readText() })
        }
    }
}

tasks.named("preBuild") {
    dependsOn("generateLicenseReport")
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    doLast {
        val debugApk = layout.buildDirectory.file("outputs/apk/debug/com.pocketlink-debug.apk").get().asFile
        if (debugApk.exists()) {
            val rootApk = layout.buildDirectory.file("outputs/apk/com.pocketlink.apk").get().asFile
            val debugDirApk = layout.buildDirectory.file("outputs/apk/debug/com.pocketlink.apk").get().asFile
            debugApk.copyTo(rootApk, overwrite = true)
            debugApk.copyTo(debugDirApk, overwrite = true)
        }
    }
}

// Release artifacts for GitHub Releases (see RELEASING.md):
//   build/outputs/apk/release/PocketLink-<versionName>.apk          (signed)
//   build/outputs/apk/release/PocketLink-<versionName>-unsigned.apk (no release key configured)
// plus a matching <apk>.sha256 file in `sha256sum -c` format.
tasks.matching { it.name == "assembleRelease" }.configureEach {
    val releaseVersionName = android.defaultConfig.versionName
    val signed = hasReleaseSigning
    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        // AGP's own output name, chosen explicitly so stale copies in the folder are never picked up.
        val agpApk = File(releaseDir, if (signed) "com.pocketlink-release.apk" else "com.pocketlink-release-unsigned.apk")
        check(agpApk.isFile) { "Expected release APK not found: $agpApk" }

        // Remove PocketLink-*.apk / .sha256 from earlier builds so only the current artifact remains.
        releaseDir.listFiles()
            ?.filter { it.name.startsWith("PocketLink-") && (it.extension == "apk" || it.extension == "sha256") }
            ?.forEach { it.delete() }

        val artifactName = "PocketLink-$releaseVersionName" + (if (signed) "" else "-unsigned") + ".apk"
        val artifact = agpApk.copyTo(File(releaseDir, artifactName), overwrite = true)

        val digest = MessageDigest.getInstance("SHA-256")
        artifact.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        File(releaseDir, "$artifactName.sha256").writeText("$sha256  $artifactName\n")

        // Keep the legacy com.pocketlink.apk copies used by local install scripts.
        agpApk.copyTo(layout.buildDirectory.file("outputs/apk/com.pocketlink.apk").get().asFile, overwrite = true)
        agpApk.copyTo(File(releaseDir, "com.pocketlink.apk"), overwrite = true)

        logger.lifecycle("PocketLink release artifact: $artifact")
        logger.lifecycle("SHA-256: $sha256")
        if (!signed) logger.warn("PocketLink: release APK is UNSIGNED and cannot be installed. See RELEASING.md.")
    }
}

tasks.register("assemblePocketLinkApk") {
    description = "Compiles PocketLink and ensures com.pocketlink.apk is generated"
    group = "build"
    dependsOn("assembleDebug")
}

