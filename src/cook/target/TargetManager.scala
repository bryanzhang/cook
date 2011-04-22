package cook.target

import scala.collection.mutable.HashMap

import cook.config.runner.ConfigType
import cook.config.runner.CookRunner
import cook.util.TargetLabel

object TargetManager {

  val targets = new HashMap[String, Target]

  def push(t: Target) {
    targets.put(t.name, t)
  }

  def getTarget(targetLabel: TargetLabel): Target = {
    if (!hasTarget(targetLabel.targetFullname)) {
      CookRunner.run(targetLabel.config, ConfigType.COOK)
    }

    targets.get(targetLabel.targetFullname) match {
      case Some(target) => target
      case None => {
        throw new TargetException(
            "Target \"%s\" is not defined".format(targetLabel.targetFullname))
      }
    }
  }

  private[target]
  def hasTarget(name: String) = targets.contains(name)

}