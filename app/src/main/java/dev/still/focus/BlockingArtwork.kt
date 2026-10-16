package dev.still.focus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local, normalized still images: no remote URL, storage permission, or expiring picker grant. */
object BlockingArtwork {
    private val fileName = Regex("[a-f0-9]{64}\\.png")
    private fun directory(ctx: Context) = File(ctx.noBackupFilesDir, "blocking-artwork").apply { mkdirs() }
    fun validName(name: String) = name.isEmpty() || fileName.matches(name)
    fun bitmap(ctx: Context, name: String): Bitmap? = if (!fileName.matches(name)) null else
        runCatching { BitmapFactory.decodeFile(File(directory(ctx), name).path) }.getOrNull()

    fun importImage(ctx: Context, uri: Uri, draft: String = ""): String {
        require(uri.scheme in setOf("content", "android.resource")) { "Choose an image from your device." }
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= 20 * 1024 * 1024) { "Choose an image under 20 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("This image could not be opened. Choose another one.")
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            require(info.size.width in 1..32768 && info.size.height in 1..32768) { "Choose a smaller image." }
            val scale = (1024f / maxOf(info.size.width, info.size.height)).coerceAtMost(1f)
            decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val png = try { ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.toByteArray()
        } } finally { bitmap.recycle() }
        val name = MessageDigest.getInstance("SHA-256").digest(png).joinToString("") { "%02x".format(it) } + ".png"
        val destination = File(directory(ctx), name)
        if (!destination.exists()) {
            val temporary = File.createTempFile("import-", ".tmp", directory(ctx))
            try {
                temporary.writeBytes(png)
                check(temporary.renameTo(destination)) { "Couldn't save this image. Try again." }
            } finally { temporary.delete() }
        }
        prune(ctx, setOf(name, draft, Repository.state.blockMessage.image))
        return name
    }

    fun prune(ctx: Context, keep: Set<String>) {
        directory(ctx).listFiles()?.filter { fileName.matches(it.name) && it.name !in keep }?.forEach { it.delete() }
    }
    fun art(message: BlockMessage, mode: FocusMode) = CatArt.entries.firstOrNull { it.name == message.art } ?: mode.catArt()
}

@Composable fun BlockingIllustration(message: BlockMessage, mode: FocusMode, modifier: Modifier = Modifier) {
    if (message.art == "NONE" && message.image.isEmpty()) return
    val ctx = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, message.image) {
        value = withContext(Dispatchers.IO) { BlockingArtwork.bitmap(ctx, message.image) }
    }
    val image = bitmap
    if (image != null) Image(image.asImageBitmap(), null, modifier, contentScale = ContentScale.Fit)
    else CatDoodle(BlockingArtwork.art(message, mode), modifier)
}

val blockingCats = listOf(CatArt.REMINDER, CatArt.PATIENCE, CatArt.BOUNDARY,
    CatArt.BREATHE, CatArt.MOON, CatArt.TEA, CatArt.BOOK, CatArt.HEADPHONES,
    CatArt.CLOUD, CatArt.FLOWER, CatArt.KITE, CatArt.PILLOW, CatArt.LANTERN)

@Composable fun BlockingArtChoices(art: String, image: String, enabled: Boolean, choose: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(blockingCats, key = { it.name }) { cat ->
            val selected = image.isEmpty() && art == cat.name
            val label = cat.name.lowercase().replaceFirstChar { it.uppercase() }
            Surface(onClick = { choose(cat.name) }, enabled = enabled, selected = selected,
                modifier = Modifier.width(100.dp), shape = RoundedCornerShape(20.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = .07f),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CatDoodle(cat, Modifier.size(84.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
