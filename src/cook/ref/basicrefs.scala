package cook.ref

import cook.console.ops._
import cook.error._
import cook.path.Path

import scala.annotation.tailrec
import scala.reflect.io.{ Path => SPath, Directory }


/**
 * Directory reference.
 *
 * segments is start from cook root.
 *
 * @author iamtimgreen@gmail.com (Tim Green)
 */
class DirRef(val segments: List[String]) extends Ref {

  def toDir: Directory = segments.foldLeft(Path().rootDir: SPath)(_ / _).toDirectory
  override def refName: String = if (segments.isEmpty) "/" else segments.mkString("/", "/", "/")
  override def isDir: Boolean = true
}

object DirRefFactory extends RefFactory[DirRef] {

  /**
   * DirRef must endsWith '/' and not contains ":", "//".
   *
   * StartsWith '/' means abs path, e.g
   *   "/src/a/b/c/"
   *   "/"
   * Otherwise means relative path, e.g
   *   "a/b/c/"
   *   "d"
   * "." and ".." is valid, e.g
   *   "../../../a/b/c"
   *   "./x/y/z"
   * But the ref should never jump out of the cook root.
   *   "/../../a/" is invalid
   */
  override def apply(baseSegments: List[String], refName: String): Option[DirRef] = {
    if (refName.nonEmpty &&
      (refName.lastOption != Some('/') || refName.indexOf("//") > 0 || refName.indexOf(":") > 0)) {
      None
    } else {
      val segments =
        if (refName.headOption == Some('/')) {
          relativeDir(Nil, refName.drop(1))
        } else {
          relativeDir(baseSegments, refName)
        }
      Some(new DirRef(segments))
    }
  }

  def relativeDir(originBaseSegments: List[String], refName: String): List[String] = {
    @tailrec
    def doRelativeDir(baseSegments: List[String], segments: List[String]): List[String] = {
      segments match {
        case Nil =>
          baseSegments
        case ".." :: tail =>
          reportErrorIf(baseSegments.isEmpty) {
            "Bad ref, " :: strong("DirRef") :: "can not jump out of cook root: " ::
            "base(" :: originBaseSegments.toString :: ") " ::
            "ref(" :: refName :: ")"
          }
          doRelativeDir(baseSegments.dropRight(1), tail)
        case "." :: tail =>
          doRelativeDir(baseSegments, tail)
        case head :: tail =>
          doRelativeDir(baseSegments :+ head, tail)
      }
    }

    if (refName.isEmpty) {
      originBaseSegments
    } else {
      doRelativeDir(originBaseSegments, refName.split('/').toList)
    }
  }

}

abstract class PathRef(val dir: DirRef, lastPart: String) extends Ref

class FileRef(dir: DirRef, val filename: String) extends PathRef(dir, filename) {

  def toPath: SPath = dir.toDir / filename
  override def refName: String = dir.refName + filename
  override def isFile: Boolean = true
}

object FileRefFactory extends RefFactory[FileRef] {

  val P = "((.*/)?)([^:/]+)".r
  override def apply(baseSegments: List[String], refName: String): Option[FileRef] = {
    refName match {
      case P(refDir, _, filename) =>
        DirRefFactory(baseSegments, refDir) map { dir =>
          new FileRef(dir, filename)
        }
      case _ =>
        None
    }
  }
}

trait TargetRef extends Ref {

  def targetName: String
  def targetPath: List[String]

  def targetBuildParentDir: Directory = {
    targetPath.foldLeft(Path().targetBuildDir: SPath)(_ / _) toDirectory
  }

  def targetBuildDir: Directory = targetBuildParentDir / (targetName + ".cook_target") toDirectory

  def metaKey: String
}

class NativeTargetRef(dir: DirRef, val targetName: String)
  extends PathRef(dir, targetName) with TargetRef {

  override def targetPath: List[String] = "native" :: dir.segments

  override def refName: String = {
    val dirRefName = dir.refName.dropRight(1)
    if (dirRefName.nonEmpty) {
      dirRefName + ":" + targetName
    } else {
      "/:" + targetName
    }
  }
  override def isTarget: Boolean = true
  override def isNativeTarget: Boolean = true
  def cookFileRef: FileRef = new FileRef(dir, "COOK")

  override def metaKey: String = "nativeTarget:" + refName
}

object NativeTargetRefFactory extends RefFactory[NativeTargetRef] {

  val P = "(.*):([^:/]+)".r
  override def apply(baseSegments: List[String], refName: String): Option[NativeTargetRef] = {
    refName match {
      case P(refDir, targetName) =>
        val refDirFix = if (refDir.nonEmpty) refDir + "/" else ""
        DirRefFactory(baseSegments, refDirFix) map { dir =>
          new NativeTargetRef(dir, targetName)
        }
      case _ => None
    }
  }
}

// TODO(timgreen): Plugin Target ref
// Start with //<plugin_name>/
// e.g. "//mvn/com.github.scopt%%scopt%2.1.0"
// can be generated by some helper function like:
// mvn / "com.github.scopt" %% "scopt" % "2.1.0"
trait PluginTargetRef extends TargetRef {

  def pluginName: String
  final override def targetPath: List[String] = "plugin" :: pluginName :: pluginTargetPath
  def pluginTargetPath: List[String]

  override def isTarget: Boolean = true
  override def isPluginTarget: Boolean = true
}

trait PluginTargetRefFactory[P <: PluginTargetRef] extends RefFactory[P] {

  val pluginName: String

  final override def apply(baseSegments: List[String], refName: String): Option[P] = {
    val prefix = "//" + pluginName + "/"
    if (refName.startsWith(prefix)) {
      parseRefName(refName.drop(prefix.size))
    } else {
      None
    }
  }

  def parseRefName(refName: String): Option[P] = None
}
