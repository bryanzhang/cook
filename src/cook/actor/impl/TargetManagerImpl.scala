package cook.actor.impl

import cook.actor.TargetManager
import cook.actor.impl.util.BatchResponser
import cook.config.Config
import cook.config.ConfigRef
import cook.console.ops._
import cook.error._
import cook.ref.NativeTargetRef
import cook.ref.TargetRef
import cook.target.Target
import cook.target.TargetResult

import akka.actor.{ ActorContext, TypedActor, TypedProps }
import scala.concurrent.{ Promise, Future, Await }
import scala.concurrent.duration._
import scala.util.Try


class TargetManagerImpl extends TargetManager with TypedActorBase {

  import ActorRefs._

  private def self = targetManager

  private val nativeResponser = new BatchResponser[String, Target[TargetResult]](processError)

  override def nativeTaskComplete(refName: String)(tryTarget: Try[Target[TargetResult]]) {
    nativeResponser.complete(refName)(tryTarget)
  }

  private def processError(key: String, e: Throwable): Throwable = error(e) {
    "Error when get target: " :: strong(key)
  }

  override def getTarget(targetRef: TargetRef): Future[Target[TargetResult]] = {
    log.debug("getTarget {}", targetRef.refName)
    targetRef match {
      case nativeTargetRef: NativeTargetRef =>
        nativeResponser.onTask(nativeTargetRef.refName) {
          doGetNativeTarget(nativeTargetRef.refName, nativeTargetRef)
        }
    }
  }

  private def doGetNativeTarget(refName: String, nativeTargetRef: NativeTargetRef) {
    import TypedActor.dispatcher

    configManager.getConfig(nativeTargetRef.cookFileRef) flatMap { config =>
      log.debug("doGetNativeTarget {} {}", config, refName)
      config.getTargetOption(refName) match {
        case Some(t) =>
          Future.successful(t)
        case None =>
          Future.failed(targetNotFoundException(refName))
      }
    } onComplete self.nativeTaskComplete(refName)
  }

  private def targetNotFoundException(refName: String) = error {
    "Target " :: strong(refName) :: " not exist" :: newLine ::
    indent :: "Should defined in config " :: strong(ConfigRef.defineConfigRefNameForTarget(refName))
  }
}
