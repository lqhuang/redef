package redef.io

import java.io.{IOException}
import java.io.File
import java.nio.file.{DirectoryNotEmptyException}
import java.nio.file.Files
import java.nio.file.Path

// import scala.language.experimental.saferExceptions

import scala.util.Either

import scala.util.Using.Releasable

object TempFileUtils {
  opaque type TempFile = File
  type Excs = DirectoryNotEmptyException | IOException | SecurityException

  given tempFileReleasable: Releasable[TempFile] =
    new Releasable[TempFile] {
      def release(resource: TempFile): Unit =
        try {
          Files.deleteIfExists(resource.toPath())
        } catch {
          case e: Excs => {
            // Ignore the exception, as we are deleting a temporary file.
            // Warn if necessary.
          }
        }
    }
}
