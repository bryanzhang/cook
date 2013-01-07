package cook.actor

import cook.config.Config
import cook.config.ConfigRef
import cook.ref.FileRef

import akka.actor.ActorRef
import akka.pattern.ask
import akka.pattern.pipe
import scala.collection.mutable
import scala.concurrent.Future


class ConfigManagerActor extends ActorBase {

  private val cache = mutable.Map[String, Config]()
  private val configWaiters = mutable.Map[String, mutable.ListBuffer[ActorRef]]()

  val configLoaderActor = context.actorFor("./ConfigLoader")
  val configRefManagerActor = context.actorFor("./ConfigRefManager")
  val configManagerActor = self

  def receive = {
    case GetConfig(cookFileRef) =>
      val refName = cookFileRef.refName
      cache.get(refName) match {
        case Some(config) => sender ! config
        case None =>
          val list = configWaiters.getOrElseUpdate(
            refName, mutable.ListBuffer[ActorRef]())
          list += sender
          if (list.size == 1) {
            startLoadConfig(refName, cookFileRef)
          }
      }
    case ConfigLoaded(refName, config) =>
      cache(refName) = config
      configWaiters.remove(refName) match {
        case None =>
        case Some(list) =>
          for (s <- list) {
            s ! config
          }
      }
    case FindConfigRef(configRef) =>
      configLoaderActor ! LoadConfig(configRef)
  }


  private def startLoadConfig(refName: String, cookFileRef: FileRef) {
    ask(configRefManagerActor, LoadConfigRef(refName, cookFileRef)) pipeTo configManagerActor
  }

}
