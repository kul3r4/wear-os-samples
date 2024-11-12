/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

const val SET_WATCH_FACE_CMD =
    "shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE --es operation set-watchface --es watchFaceId %s"

/**
 * Installs and sets watch face on an attached device via ADB.
 */
open class AdbInstallTask : DefaultTask() {
    @set:Option(option = "device", description = "The ADB device to install on")
    @get:Input
    var device: String = ""

    @get:Input
    lateinit var apkLocation: Provider<Directory>

    @get:Input
    lateinit var artifactLoader: BuiltArtifactsLoader

    // As this task has no outputs defined, it will always be executed, which is desirable as the
    // APK should be installed even if the APK itself hasn't changed. (It may have been removed from
    // the device).

    @TaskAction
    fun install() {
        val artifacts =
            artifactLoader.load(apkLocation.get()) ?: throw GradleException("Cannot load APKs")
        if (artifacts.elements.size != 1)
            throw GradleException("Expected only one APK!")
        val apkPath = File(artifacts.elements.single().outputFile).toPath()

        val deviceArg = if (device.isEmpty()) "" else "-s $device "
        val installWatchFaceCmd = deviceArg + "install $apkPath"
        val setWatchFaceCmd = deviceArg + SET_WATCH_FACE_CMD.format(artifacts.applicationId)

        project.exec {
            val argsList = installWatchFaceCmd.split(" ").toList()
            it.commandLine("adb")
            it.args(argsList)
        }

        project.exec {
            val argsList = setWatchFaceCmd.split(" ").toList()
            it.commandLine("adb")
            it.args(argsList)
        }
    }
}