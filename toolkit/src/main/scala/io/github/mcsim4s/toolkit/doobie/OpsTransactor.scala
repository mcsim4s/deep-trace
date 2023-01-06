package io.github.mcsim4s.toolkit.doobie

import cats.effect.Resource
import com.zaxxer.hikari._
import doobie.util.transactor.{Strategy, Transactor}
import io.github.mcsim4s.toolkit.config.JdbcConfig
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.telemetry.opentelemetry.Tracing
import zio.{Task, _}

import java.sql.Connection
import javax.sql.DataSource

object OpsTransactor {

  def makeTransactor(conf: JdbcConfig, tracing: Tracing): ZIO[Scope, Throwable, Transactor[Task]] = {
    import tracing._
    for {
      dataSource <- ZIO
        .fromAutoCloseable {
          ZIO.from {
            import conf._
            val hikariConfig = new HikariConfig()
            hikariConfig.setPoolName(s"db-$name")
            hikariConfig.setDriverClassName(driver)
            hikariConfig.setJdbcUrl(url)
            hikariConfig.setReadOnly(readOnly)
            hikariConfig.setUsername(user)
            hikariConfig.setPassword(password)
            props.foreach { case (key, value) =>
              hikariConfig.addDataSourceProperty(key, value)
            }
            new HikariDataSource(hikariConfig)
          }
        }
      connect = (dataSource: DataSource) => {
        val acquire: Task[Connection] = ZIO.attemptBlocking(dataSource.getConnection)

        def release(c: Connection) = ZIO.attemptBlocking(c.close()).orDie
        Resource.scoped(ZIO.acquireRelease(acquire)(release))
      }
      stateRef <- FiberRef.make[TransactionState](TransactionState.Closed)
      interpreter = new TracingInterpreter(tracing, state = stateRef)
    } yield Transactor[Task, DataSource](dataSource, connect, interpreter.ConnectionInterpreter, Strategy.default)
  }
}
