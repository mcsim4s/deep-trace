package io.github.mcsim4s.dt.dao

import doobie.util.log.{ExecFailure, LogHandler, ProcessingFailure, Success}
import org.slf4j.LoggerFactory

trait LogDoobieQueries {

  private val logger = LoggerFactory.getLogger(this.getClass)

  implicit val slf4jLogHandler: LogHandler = {
    LogHandler {
      case Success(s, a, e1, e2) if (e1 + e2).toMillis > 500 =>
        logger.info(s"""Successful Statement Execution:
                      |
                      |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                      |
                      | arguments = [${a.mkString(", ")}]
                      |   elapsed = ${e1.toMillis.toString} ms exec + ${e2.toMillis.toString} ms processing (${(e1 + e2).toMillis.toString} ms total)
          """.stripMargin)

      case Success(_, _, _, _) =>
      case ProcessingFailure(s, a, e1, e2, t) =>
        logger.error(s"""Failed Resultset Processing:
                      |
                      |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                      |
                      | arguments = [${a.mkString(", ")}]
                      |   elapsed = ${e1.toMillis.toString} ms exec + ${e2.toMillis.toString} ms processing (failed) (${(e1 + e2).toMillis.toString} ms total)
                      |   failure = ${t.getMessage}
          """.stripMargin)

      case ExecFailure(s, a, e1, t) =>
        logger.error(s"""Failed Statement Execution:
                        |
                        |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
                        |
                        | arguments = [${a.mkString(", ")}]
                        |   elapsed = ${e1.toMillis.toString} ms exec (failed)
                        |   failure = ${t.getMessage}
          """.stripMargin)

    }
  }

}
