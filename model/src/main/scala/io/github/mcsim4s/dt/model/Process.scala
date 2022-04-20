package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process.ProcessId

sealed trait Process {
  def id: ProcessId

  def isGap: Boolean =
    this match {
      case _: Process.Gap => true
      case _              => false
    }
}

object Process {
  case class ProcessId(hash: String)

  case class ParallelProcess(
      service: String,
      operation: String,
      children: Seq[SequentialProcess]
  ) extends Process {
    lazy val id: ProcessId = {
      val builder = new StringBuilder()
      builder.append(service)
      builder.append(operation)
      children.sortBy(_.id.hash).foreach(child => builder.append(child.id.hash))
      ProcessId(MD5.hash(builder.toString()))
    }
  }

  case class SequentialProcess(children: Seq[Process]) extends Process {
    lazy val id: ProcessId = {
      val builder = new StringBuilder()
      children.filterNot(_.isGap).foreach(child => builder.append(child.id))
      ProcessId(MD5.hash(builder.toString()))
    }
  }

  case class ConcurrentProcess(of: ParallelProcess) extends Process {
    lazy val id: ProcessId = {
      val builder = new StringBuilder()
      builder.append("concurrent")
      builder.append(of.id.hash)
      ProcessId(MD5.hash(builder.toString()))
    }
  }

  case class Gap(parent: SequentialProcess, previous: Option[Process]) extends Process {
    lazy val id: ProcessId = {
      val builder = new StringBuilder()
      builder.append("gap")
      builder.append(parent.id.hash)
      val path = parent.children.takeWhile(p => previous.forall(_.id == p.id))
      path.foreach(p => builder.append(p.id.hash))
      ProcessId(MD5.hash(builder.toString()))
    }
  }
}
