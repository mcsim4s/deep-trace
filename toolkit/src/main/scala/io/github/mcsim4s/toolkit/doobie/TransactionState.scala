package io.github.mcsim4s.toolkit.doobie

import io.opentelemetry.api.trace.Span
import zio._

sealed trait TransactionState

object TransactionState {
  final case class Ongoing(spanFinalizer: UIO[Any]) extends TransactionState
  final case object Closed extends TransactionState

}
