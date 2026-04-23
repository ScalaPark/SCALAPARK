package app.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.reflect.ClassTag

trait JsonStringDeSerializer[T] {
  implicit protected val ct: ClassTag[T]

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JavaTimeModule())
  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  protected def fromJsonString(json: String): Either[String, T] = {
    try Right(mapper.readValue(json, ct.runtimeClass.asInstanceOf[Class[T]]))
    catch {
      case ex: Exception => Left(s"Unable to deserialize message: ${ex.getMessage}")
    }
  }
}
