package redef.io

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

import redef.util.Using.Releasable

opaque type TempFile = File

given tempFileReleasable: Releasable[TempFile] = new Releasable[TempFile] {
  def release(resource: TempFile)(using CanThrow[IOException]): Unit =
    Files.deleteIfExists(resource)
}
