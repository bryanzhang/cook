package cook.actor

import cook.error.CookException

trait ConsoleOutputter {

  def printError(e: CookException)
  def printUnknownError(e: Throwable)
  def stopStatusUpdate
  // NOTE(timgreen): return value Int is used to mark this request blocking.
  def blockToFinish: Int
}