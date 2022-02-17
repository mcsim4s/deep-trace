package io.github.mcsim4s.dt.engine.live

import io.github.mcsim4s.dt.model.DeepTraceError
import io.github.mcsim4s.dt.model.DeepTraceError.CasConflict
import zio.Schedule

package object store {
  val CasRetryPolicy: Schedule[Any, DeepTraceError, (DeepTraceError, Long)] =
    Schedule.recurWhile[DeepTraceError] {
      case _: CasConflict => true
      case _              => false
    } && Schedule.recurs(3)
}
