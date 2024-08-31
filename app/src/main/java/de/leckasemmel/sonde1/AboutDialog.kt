package de.leckasemmel.sonde1

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.Locale


class AboutDialog (val context: Context, private val info: RaComm.TargetInfo) {
    fun show() {
        var appVersion = "-"

        try {
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }

        // Split BL652 firmware version into four fields
        val bl652FirmwareVersion: String = String.format(
            Locale.US, "%d.%d.%d.%d",
                (info.bl652FirmwareVersion / (16777216)) % 256,
                ((info.bl652FirmwareVersion % 16777216) / 65536) % 256,
                ((info.bl652FirmwareVersion % 65536) / 256) % 256,
                info.bl652FirmwareVersion % 256
        )

        var powerScript: String = context.getString(R.string.dialog_about_unknown)
        if (info.bl652HavePowerScript != null) {
            powerScript = if (info.bl652HavePowerScript != 0) {
                context.getString(R.string.dialog_about_yes)
            } else {
                context.getString(R.string.dialog_about_no)
            }
        }

        var loaderVersion: String = context.getString(R.string.dialog_about_unknown)
        if (info.loaderVersion != null) {
            if (info.loaderVersion != 0) {
                loaderVersion = String.format(Locale.US, "%d", info.loaderVersion)
            }
        }

        val message: String = String.format(
            Locale.US, "%s %s\n%s %s.%s\n%s %s\n%s %s\n%s %s",
            context.getString(R.string.dialog_about_app_version),
            appVersion,
            context.getString(R.string.dialog_about_firmware_version),
            info.firmwareMajor,
            info.firmwareMinor,
            "BL652 Firmware:",
            bl652FirmwareVersion,
            context.getString(R.string.dialog_about_power_script),
            powerScript,
            context.getString(R.string.dialog_about_loader_version),
            loaderVersion
        )

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_about_title))
            .setCancelable(true)
            .setMessage(message)
            .setPositiveButton("OK") {_, _ ->  }
            .create().show()
    }
}
