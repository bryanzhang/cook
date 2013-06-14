package cook.target

import cook.error.ErrorTracking._
import cook.ref.NativeTargetRef
import cook.ref.TargetRef

class NativeTarget[R <: TargetResult](
    ref: NativeTargetRef,
    buildCmd: TargetBuildCmd[R],
    resultFn: TargetResultFn[R],
    metaFn: TargetMetaFn[R],
    runCmd: Option[TargetRunCmd[R]],
    deps: Seq[TargetRef])
  extends Target[R](ref, buildCmd, resultFn, metaFn, runCmd, deps) {

}
