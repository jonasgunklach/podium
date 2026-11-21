package app.podiumpodcasts.podium

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import app.podiumpodcasts.podium.manager.DatabaseManager

// ponytail: debug-only migration helper, reuses the app's own restore; release builds don't include it
class ImportBackupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

        if(uri == null || !DatabaseManager.isRestoreFileValid(this, uri)) {
            Toast.makeText(this, "Not a podium backup", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        DatabaseManager.restoreFromBackup(this, uri)
        Toast.makeText(this, "Backup imported", Toast.LENGTH_LONG).show()

        // restart so Room reopens (and migrates) the restored database
        startActivity(packageManager.getLaunchIntentForPackage(packageName))
        finish()
        Process.killProcess(Process.myPid())
    }
}
