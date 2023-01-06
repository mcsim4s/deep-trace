package io.github.mcsim4s.toolkit.doobie

import java.io.{InputStream, Reader}
import java.net.URL
import java.sql
import java.sql._
import java.util.Calendar
import scala.annotation.nowarn

class WrappedPreparedStatement(ps: PreparedStatement, val query: String) extends PreparedStatement {
  override def executeQuery(): ResultSet = ps.executeQuery()

  override def executeUpdate(): Int = ps.executeUpdate()

  override def setNull(parameterIndex: Int, sqlType: Int): Unit = ps.setNull(parameterIndex, sqlType)

  override def setBoolean(parameterIndex: Int, x: Boolean): Unit = ps.setBoolean(parameterIndex, x)

  override def setByte(parameterIndex: Int, x: Byte): Unit = ps.setByte(parameterIndex, x)

  override def setShort(parameterIndex: Int, x: Short): Unit = ps.setShort(parameterIndex, x)

  override def setInt(parameterIndex: Int, x: Int): Unit = ps.setInt(parameterIndex, x)

  override def setLong(parameterIndex: Int, x: Long): Unit = ps.setLong(parameterIndex, x)

  override def setFloat(parameterIndex: Int, x: Float): Unit = ps.setFloat(parameterIndex, x)

  override def setDouble(parameterIndex: Int, x: Double): Unit = ps.setDouble(parameterIndex, x)

  override def setBigDecimal(parameterIndex: Int, x: java.math.BigDecimal): Unit = ps.setBigDecimal(parameterIndex, x)

  override def setString(parameterIndex: Int, x: String): Unit = ps.setString(parameterIndex, x)

  override def setBytes(parameterIndex: Int, x: scala.Array[Byte]): Unit = ps.setBytes(parameterIndex, x)

  override def setDate(parameterIndex: Int, x: Date): Unit = ps.setDate(parameterIndex, x)

  override def setTime(parameterIndex: Int, x: Time): Unit = ps.setTime(parameterIndex, x)

  override def setTimestamp(parameterIndex: Int, x: Timestamp): Unit = ps.setTimestamp(parameterIndex, x)

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Int): Unit =
    ps.setAsciiStream(parameterIndex, x, length)

  @nowarn("cat=deprecation")
  override def setUnicodeStream(parameterIndex: Int, x: InputStream, length: Int): Unit =
    ps.setUnicodeStream(parameterIndex, x, length)

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Int): Unit =
    ps.setBinaryStream(parameterIndex, x, length)

  override def clearParameters(): Unit = ps.clearParameters()

  override def setObject(parameterIndex: Int, x: Any, targetSqlType: Int): Unit =
    ps.setObject(parameterIndex, x, targetSqlType)

  override def setObject(parameterIndex: Int, x: Any): Unit = ps.setObject(parameterIndex, x)

  override def execute(): Boolean = ps.execute()

  override def addBatch(): Unit = ps.addBatch()

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Int): Unit =
    ps.setCharacterStream(parameterIndex, reader, length)

  override def setRef(parameterIndex: Int, x: Ref): Unit = ps.setRef(parameterIndex, x)

  override def setBlob(parameterIndex: Int, x: Blob): Unit = ps.setBlob(parameterIndex, x)

  override def setClob(parameterIndex: Int, x: Clob): Unit = ps.setClob(parameterIndex, x)

  override def setArray(parameterIndex: Int, x: sql.Array): Unit = ps.setArray(parameterIndex, x)

  override def getMetaData: ResultSetMetaData = ps.getMetaData

  override def setDate(parameterIndex: Int, x: Date, cal: Calendar): Unit = ps.setDate(parameterIndex, x, cal)

  override def setTime(parameterIndex: Int, x: Time, cal: Calendar): Unit = ps.setTime(parameterIndex, x, cal)

  override def setTimestamp(parameterIndex: Int, x: Timestamp, cal: Calendar): Unit =
    ps.setTimestamp(parameterIndex, x, cal)

  override def setNull(parameterIndex: Int, sqlType: Int, typeName: String): Unit =
    ps.setNull(parameterIndex, sqlType, typeName)

  override def setURL(parameterIndex: Int, x: URL): Unit = ps.setURL(parameterIndex, x)

  override def getParameterMetaData: ParameterMetaData = ps.getParameterMetaData

  override def setRowId(parameterIndex: Int, x: RowId): Unit = ps.setRowId(parameterIndex, x)

  override def setNString(parameterIndex: Int, value: String): Unit = ps.setNString(parameterIndex, value)

  override def setNCharacterStream(parameterIndex: Int, value: Reader, length: Long): Unit =
    ps.setNCharacterStream(parameterIndex, value, length)

  override def setNClob(parameterIndex: Int, value: NClob): Unit = ps.setNClob(parameterIndex, value)

  override def setClob(parameterIndex: Int, reader: Reader, length: Long): Unit =
    ps.setClob(parameterIndex, reader, length)

  override def setBlob(parameterIndex: Int, inputStream: InputStream, length: Long): Unit =
    ps.setBlob(parameterIndex, inputStream, length)

  override def setNClob(parameterIndex: Int, reader: Reader, length: Long): Unit =
    ps.setNClob(parameterIndex, reader, length)

  override def setSQLXML(parameterIndex: Int, xmlObject: SQLXML): Unit =
    ps.setSQLXML(parameterIndex, xmlObject)

  override def setObject(parameterIndex: Int, x: Any, targetSqlType: Int, scaleOrLength: Int): Unit =
    ps.setObject(parameterIndex, x, targetSqlType, scaleOrLength)

  override def setAsciiStream(parameterIndex: Int, x: InputStream, length: Long): Unit =
    ps.setAsciiStream(parameterIndex, x, length)

  override def setBinaryStream(parameterIndex: Int, x: InputStream, length: Long): Unit =
    ps.setBinaryStream(parameterIndex, x, length)

  override def setCharacterStream(parameterIndex: Int, reader: Reader, length: Long): Unit =
    ps.setCharacterStream(parameterIndex, reader, length)

  override def setAsciiStream(parameterIndex: Int, x: InputStream): Unit = ps.setAsciiStream(parameterIndex, x)

  override def setBinaryStream(parameterIndex: Int, x: InputStream): Unit = ps.setBinaryStream(parameterIndex, x)

  override def setCharacterStream(parameterIndex: Int, reader: Reader): Unit =
    ps.setCharacterStream(parameterIndex, reader)

  override def setNCharacterStream(parameterIndex: Int, value: Reader): Unit =
    ps.setNCharacterStream(parameterIndex, value)

  override def setClob(parameterIndex: Int, reader: Reader): Unit = ps.setClob(parameterIndex, reader)

  override def setBlob(parameterIndex: Int, inputStream: InputStream): Unit = ps.setBlob(parameterIndex, inputStream)

  override def setNClob(parameterIndex: Int, reader: Reader): Unit = ps.setNClob(parameterIndex, reader)

  override def executeQuery(sql: String): ResultSet = ps.executeQuery(sql)

  override def executeUpdate(sql: String): Int = ps.executeUpdate(sql)

  override def close(): Unit = ps.close()

  override def getMaxFieldSize: Int = ps.getMaxFieldSize

  override def setMaxFieldSize(max: Int): Unit = ps.setMaxFieldSize(max)

  override def getMaxRows: Int = ps.getMaxRows

  override def setMaxRows(max: Int): Unit = ps.setMaxRows(max)

  override def setEscapeProcessing(enable: Boolean): Unit = ps.setEscapeProcessing(enable)

  override def getQueryTimeout: Int = ps.getQueryTimeout

  override def setQueryTimeout(seconds: Int): Unit = ps.setQueryTimeout(seconds)

  override def cancel(): Unit = ps.cancel()

  override def getWarnings: SQLWarning = ps.getWarnings

  override def clearWarnings(): Unit = ps.clearWarnings()

  override def setCursorName(name: String): Unit = ps.setCursorName(name)

  override def execute(sql: String): Boolean = ps.execute(sql)

  override def getResultSet: ResultSet = ps.getResultSet

  override def getUpdateCount: Int = ps.getUpdateCount

  override def getMoreResults: Boolean = ps.getMoreResults()

  override def setFetchDirection(direction: Int): Unit = ps.setFetchDirection(direction)

  override def getFetchDirection: Int = ps.getFetchDirection

  override def setFetchSize(rows: Int): Unit = ps.setFetchSize(rows)

  override def getFetchSize: Int = ps.getFetchSize

  override def getResultSetConcurrency: Int = ps.getResultSetConcurrency

  override def getResultSetType: Int = ps.getResultSetType

  override def addBatch(sql: String): Unit = ps.addBatch()

  override def clearBatch(): Unit = ps.clearBatch()

  override def executeBatch(): scala.Array[Int] = ps.executeBatch()

  override def getConnection: Connection = ps.getConnection

  override def getMoreResults(current: Int): Boolean = ps.getMoreResults(current)

  override def getGeneratedKeys: ResultSet = ps.getGeneratedKeys

  override def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = ps.executeUpdate(sql, autoGeneratedKeys)

  override def executeUpdate(sql: String, columnIndexes: scala.Array[Int]): Int = ps.executeUpdate(sql, columnIndexes)

  override def executeUpdate(sql: String, columnNames: scala.Array[String]): Int = ps.executeUpdate(sql, columnNames)

  override def execute(sql: String, autoGeneratedKeys: Int): Boolean = ps.execute(sql, autoGeneratedKeys)

  override def execute(sql: String, columnIndexes: scala.Array[Int]): Boolean = ps.execute(sql, columnIndexes)

  override def execute(sql: String, columnNames: scala.Array[String]): Boolean = ps.execute(sql, columnNames)

  override def getResultSetHoldability: Int = ps.getResultSetHoldability

  override def isClosed: Boolean = ps.isClosed

  override def setPoolable(poolable: Boolean): Unit = ps.setPoolable(poolable)

  override def isPoolable: Boolean = ps.isPoolable

  override def closeOnCompletion(): Unit = ps.closeOnCompletion()

  override def isCloseOnCompletion: Boolean = ps.isCloseOnCompletion

  override def unwrap[T](iface: Class[T]): T = ps.unwrap(iface)

  override def isWrapperFor(iface: Class[_]): Boolean = ps.isWrapperFor(iface)
}
