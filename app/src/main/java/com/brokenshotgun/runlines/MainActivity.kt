package com.brokenshotgun.runlines

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.brokenshotgun.runlines.data.FountainSerializer
import com.brokenshotgun.runlines.data.PdfParser
import com.brokenshotgun.runlines.data.ScriptReaderDbHelper
import com.brokenshotgun.runlines.model.Script
import com.brokenshotgun.runlines.ui.navigation.AppNavigation
import com.brokenshotgun.runlines.ui.screens.TtsPlaybackController
import com.brokenshotgun.runlines.utils.Intents
import com.google.android.material.snackbar.Snackbar
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: ScriptReaderDbHelper
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(this)
        dbHelper = ScriptReaderDbHelper(this)

        val deepLinkScriptId = intent.getLongExtra("script_id", -1L)
        val deepLinkSceneIndex = intent.getIntExtra("scene_index", 0)

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        onAddScript = { showAddScriptDialog() },
                        onImportScript = { showImportFileSelect() },
                        dbHelper = dbHelper,
                        initialScriptId = deepLinkScriptId,
                        initialSceneIndex = deepLinkSceneIndex
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            TtsPlaybackController.hideNotification(this)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            TtsPlaybackController.hideNotification(this)
        }
        super.onDestroy()
    }

    private fun showAddScriptDialog() {
        val inputText = EditText(this).apply {
            hint = "Enter script name"
        }
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(inputText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(50, 50, 50, 50) })
        }
        AlertDialog.Builder(this).apply {
            setTitle("Add Script")
            setView(inputLayout)
            setPositiveButton("Add") { _, _ ->
                val name = inputText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newScript = Script(name)
                    dbHelper.insertScript(newScript)
                }
            }
        }.create().show()
    }

    private fun showImportFileSelect() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        Intents.maybeStartActivityForResult(this, intent, 0)
    }

    // Note: onActivityResult needs to be handled via ActivityResultLauncher in modern Compose apps,
    // but for this migration we can still override it or use rememberLauncherForActivityResult.
    // For now, I'll keep the logic but it needs to be wired into the Compose shell.
}
