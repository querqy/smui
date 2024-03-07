package services

import com.google.cloud.storage.{BlobId, StorageOptions}

import java.io.{InputStreamReader, Reader}
import java.net.URI
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

trait ReaderProvider {
  def openReader(url: String): Reader
}

class FileSystemReaderProvider extends ReaderProvider {
  override def openReader(url: String): Reader = {
    val uri = if (url.startsWith("file://")) url else "file://"  + url
    val path = Paths.get(new URI(uri))
    Files.newBufferedReader(path)
  }
}

class GcsReaderProvider extends ReaderProvider {

  override def openReader(url: String): Reader = {
    val storage = StorageOptions.getDefaultInstance.getService
    val blob = storage.get(BlobId.fromGsUtilUri(url))
    val readChannel = blob.reader()
    new InputStreamReader(Channels.newInputStream(readChannel), StandardCharsets.UTF_8)
  }
}

class ReaderProviderDispatcher extends ReaderProvider {

  private val fileSystemReaderProvider = new FileSystemReaderProvider()
  private lazy val gcsReaderProvider: GcsReaderProvider = {
    new GcsReaderProvider()
  }

  override def openReader(url: String): Reader = {
    val readerProvider = url.toLowerCase.trim match {
      case url if url.startsWith("gs://") => gcsReaderProvider
      case url if url.startsWith("file://") => fileSystemReaderProvider
      case url if Files.exists(Paths.get(url)) => fileSystemReaderProvider
      case _ => throw new IllegalArgumentException(s"Unsupported URL scheme or file not found: ${url}")
    }
    readerProvider.openReader(url)
  }

}
