package cook.actor.impl

import cook.actor.ConfigLoader
import cook.actor.ConfigManager
import cook.actor.ConfigRefLoader
import cook.actor.ConfigRefManager
import cook.actor.ConsoleOutputter
import cook.actor.StatusManager
import cook.actor.TargetBuilder
import cook.actor.TargetManager
import cook.app.Global

import akka.actor.{ TypedActor, TypedProps, SupervisorStrategy }
import akka.event.Logging

trait TypedActorBase extends TypedActor.Supervisor {
  val log = Logging(TypedActor.context.system, TypedActor.context.self)

  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy
}

object ActorRefs {

  import Global.system

  lazy val configRefLoader =
    TypedActor(system).typedActorOf(
      TypedProps[ConfigRefLoader],
      system.actorFor("/user/ConfigRefLoader"))

  lazy val configRefManager =
    TypedActor(system).typedActorOf(
      TypedProps[ConfigRefManager],
      system.actorFor("/user/ConfigRefManager"))

  lazy val configLoader =
    TypedActor(system).typedActorOf(
      TypedProps[ConfigLoader],
      system.actorFor("/user/ConfigLoader"))

  lazy val configManager =
    TypedActor(system).typedActorOf(
      TypedProps[ConfigManager],
      system.actorFor("/user/ConfigManager"))

  lazy val targetManager =
    TypedActor(system).typedActorOf(
      TypedProps[TargetManager],
      system.actorFor("/user/TargetManager"))

  lazy val targetBuilder =
    TypedActor(system).typedActorOf(
      TypedProps[TargetBuilder],
      system.actorFor("/user/TargetBuilder"))

  lazy val consoleOutputter =
    TypedActor(system).typedActorOf(
      TypedProps[ConsoleOutputter],
      system.actorFor("/user/ConsoleOutputter"))

  implicit lazy val statusManager =
    TypedActor(system).typedActorOf(
      TypedProps[StatusManager],
      system.actorFor("/user/StatusManager"))

}
