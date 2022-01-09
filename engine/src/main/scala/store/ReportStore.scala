package io.github.mcsim4s.dt
package store

import zio._
import zio.macros.accessible

@accessible
object ReportStore {
  type ReportStore = Has[Service]

  trait Service {
    def create(request: AnalysisRequest): UIO[AnalysisReport]
  }
}
