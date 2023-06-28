package io.github.mcsim4s.dt.model

import io.github.mcsim4s.dt.model.Process._

sealed trait Process {
  def id: ProcessId

  def tags: Map[String, String] = Map.empty

  def concurrentId: Option[String] = tags.get(ConcurrentReductionTagName)

  def children: Seq[Process] = Seq.empty

  def isGap: Boolean =
    this match {
      case _: Process.Gap => true
      case _              => false
    }
}

object Process {
  private val ConcurrentReductionTagName: String = "concurrent_id"

  case class ProcessId(hash: String)

  case class ParallelProcess(
      service: String,
      operation: String,
      override val children: Seq[SequentialProcess],
      override val tags: Map[String, String]
  ) extends Process {
    lazy val id: ProcessId = {
      val builder = new StringBuilder()
      builder.append(service)
      builder.append(operation)
      children.sortBy(_.id.hash).foreach(child => builder.append(child.id.hash))
      ProcessId(MD5.hash(builder.toString()))
    }
  }

  case class SequentialProcess(override val children: Seq[Process]) extends Process {

    lazy val id: ProcessId = {
      val builder = new StringBuilder()
      builder.append("sequential")
      children.foreach(child => builder.append(child.id))
      ProcessId(MD5.hash(builder.toString()))
    }
  }

  case class ConcurrentProcess(of: ParallelProcess) extends Process {
    override def children: Seq[Process] = Seq(of)

    lazy val id: ProcessId = {
      val builder = new StringBuilder()
      builder.append("concurrent")
      builder.append(of.id.hash)
      ProcessId(MD5.hash(builder.toString()))
    }
  }

  case class Gap(id: ProcessId) extends Process
}
