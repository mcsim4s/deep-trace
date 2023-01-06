package io.github.mcsim4s.toolkit.doobie

import cats.data.Kleisli
import cats.~>
import doobie.WeakAsync
import doobie.free.KleisliInterpreter
import doobie.free.connection.ConnectionOp
import doobie.free.preparedstatement.PreparedStatementOp
import zio.{Exit, FiberRef, Task, UIO, ZIO}
import zio.telemetry.opentelemetry.Tracing
import io.github.mcsim4s.toolkit.tracing._

import java.sql.{Connection, PreparedStatement, ResultSet}

class TracingInterpreter(tracing: Tracing, state: FiberRef[TransactionState]) extends KleisliInterpreter[Task] {
  implicit override val asyncM: WeakAsync[Task] = WeakAsync.doobieWeakAsyncForAsync(zio.interop.catz.asyncInstance)
  type ConnectionKleisli[T] = Kleisli[Task, Connection, T]
  type PreparedStatementKleisli[T] = Kleisli[Task, PreparedStatement, T]

  private def startTransactionIfNeeded: UIO[Unit] = {
    for {
      oldSpan <- state.get
      _ <- ZIO.when(oldSpan == TransactionState.Closed) {
        for {
          newTransactionSpan <- tracing.spanUnsafe("jdbc_transaction")
          (_, finalizer) = newTransactionSpan
          _ <- state.set(TransactionState.Ongoing(finalizer))
        } yield ()
      }
    } yield ()
  }

  private def endTransaction(exit: Exit[Throwable, _]): UIO[Unit] = {
    for {
      parent <- state.get
      _ <- parent match {
        case TransactionState.Ongoing(finalizer) =>
          for {
            _ <- exit.foldExitZIO(_ => tracing.markFailed, _ => ZIO.unit)
            _ <- finalizer
            _ <- state.set(TransactionState.Closed)

          } yield ()
        case TransactionState.Closed => ZIO.unit
      }
    } yield ()
  }

  override lazy val ConnectionInterpreter: ConnectionOp ~> ConnectionKleisli =
    new ConnectionInterpreter {

      override def prepareStatement(sql: String): Kleisli[Task, Connection, PreparedStatement] = {
        super.prepareStatement(sql).map(ps => new WrappedPreparedStatement(ps, sql))
      }

      override def commit: Kleisli[Task, Connection, Unit] = {
        val commit: ConnectionKleisli[Unit] = super.commit
        commit.lower.flatMapF(task => task.onExit(endTransaction))
      }

      override def rollback: Kleisli[Task, Connection, Unit] = {
        val rollback: ConnectionKleisli[Unit] = super.rollback
        rollback.lower.flatMapF(task => task.onExit(endTransaction))
      }

      override def close: Kleisli[Task, Connection, Unit] = {
        val close: ConnectionKleisli[Unit] = super.close
        close.lower.flatMapF(task => task.onExit(endTransaction))
      }
    }

  override lazy val PreparedStatementInterpreter: PreparedStatementOp ~> PreparedStatementKleisli =
    new PreparedStatementInterpreter {

      private def executeTraced[T](operation: PreparedStatementKleisli[T]) = {
        raw(identity).flatMap { ps =>
          operation.lower.flatMapF { task =>
            val tracedOperation =
              tracing.span("jdbc_query") {
                val query = ps match {
                  case w: WrappedPreparedStatement => w.query
                  case _ => ""
                }
                for {
                  _ <- tracing.setAttribute("db.statement", query)
                  res <- task
                } yield res
              }

            startTransactionIfNeeded *> tracedOperation
          }
        }
      }

      override def executeQuery: Kleisli[Task, PreparedStatement, ResultSet] = {
        executeTraced(super.executeQuery)
      }

      override def executeUpdate: Kleisli[Task, PreparedStatement, Int] = {
        executeTraced(super.executeUpdate)
      }

      override def executeBatch: Kleisli[Task, PreparedStatement, Array[Int]] = {
        executeTraced(super.executeBatch)
      }
    }
}
