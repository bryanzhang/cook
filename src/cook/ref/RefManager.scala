package cook.ref

import cook.error.ErrorTracking._

import scala.annotation.tailrec
import scala.collection.mutable

object RefManager {

  val factorys = mutable.Set[RefFactory[Ref]]()

  def apply(baseSegments: List[String], refName: String): Ref = {

    @tailrec
    def tryFactorys(it: Iterator[RefFactory[Ref]]): Option[Ref] = {
      if (it.hasNext) {
        val factory = it.next
        factory(baseSegments, refName) match {
          case Some(ref) => Some(ref)
          case None => tryFactorys(it)
        }
      } else {
        None
      }
    }

    tryFactorys(factorys.iterator) getOrElse reportError("Unknown ref name: " + refName)
  }
}