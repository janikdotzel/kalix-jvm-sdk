/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.javasdk.impl.reflection

import scala.jdk.OptionConverters._
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.Metadata
import kalix.javasdk.impl.AnySupport
import kalix.javasdk.impl.ErrorHandling.BadRequestException

/**
 * Extracts method parameters from an invocation context for the purpose of passing them to a reflective invocation call
 */
trait ParameterExtractor[-C, +T] {
  def extract(context: C): T
}

trait MetadataContext {
  def metadata: Metadata
}

trait DynamicMessageContext {
  def message: DynamicMessage
}

object ParameterExtractors {

  private def decodeParam[T](dm: DynamicMessage, cls: Class[T]): T = {
    val typeUrl = dm.getField(JavaPbAny.getDescriptor.findFieldByName("type_url")).asInstanceOf[String]
    val bytes = dm.getField(JavaPbAny.getDescriptor.findFieldByName("value")).asInstanceOf[ByteString]

    if (cls == classOf[Array[Byte]]) {
      AnySupport.decodePrimitiveBytes(bytes).toByteArray.asInstanceOf[T]
    } else {
      // TODO: avoid creating a new JavaPbAny instance
      // we want to reuse the typeUrl validation and reading logic (skip tag + jackson reader) from JsonSupport
      // we need a new internal version that also handle DynamicMessages
      val any =
        JavaPbAny
          .newBuilder()
          .setTypeUrl(typeUrl)
          .setValue(bytes)
          .build()
      JsonSupport.decodeJson(cls, any)
    }
  }

  case class AnyBodyExtractor[T](cls: Class[_]) extends ParameterExtractor[DynamicMessageContext, T] {
    override def extract(context: DynamicMessageContext): T =
      decodeParam(context.message, cls.asInstanceOf[Class[T]])
  }

  class BodyExtractor[T](field: Descriptors.FieldDescriptor, cls: Class[_])
      extends ParameterExtractor[DynamicMessageContext, T] {

    override def extract(context: DynamicMessageContext): T = {
      context.message.getField(field) match {
        case dm: DynamicMessage => decodeParam(dm, cls.asInstanceOf[Class[T]])
      }
    }
  }

  class FieldExtractor[T](field: Descriptors.FieldDescriptor, required: Boolean, deserialize: AnyRef => T)
      extends ParameterExtractor[DynamicMessageContext, T] {
    override def extract(context: DynamicMessageContext): T = {
      (required, field.isRepeated || context.message.hasField(field)) match {
        case (_, true) => deserialize(context.message.getField(field))
        //we know that currently this applies only to request parameters
        case (true, false)  => throw BadRequestException(s"Required request parameter is missing: ${field.getName}")
        case (false, false) => null.asInstanceOf[T] //could be mapped to optional later on
      }
    }
  }

  class HeaderExtractor[T >: Null](name: String, deserialize: String => T)
      extends ParameterExtractor[MetadataContext, T] {
    override def extract(context: MetadataContext): T = context.metadata.get(name).toScala.map(deserialize).orNull
  }
}
