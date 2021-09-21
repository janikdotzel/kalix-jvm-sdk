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

package com.lightbend.akkasls.codegen

import java.nio.file.Path
import java.nio.file.Paths

import scala.annotation.tailrec

import com.lightbend.akkasls.codegen.ModelBuilder.Command
import com.lightbend.akkasls.codegen.ModelBuilder.MessageTypeArgument
import com.lightbend.akkasls.codegen.ModelBuilder.ScalarType
import com.lightbend.akkasls.codegen.ModelBuilder.ScalarTypeArgument
import com.lightbend.akkasls.codegen.ModelBuilder.State
import com.lightbend.akkasls.codegen.ModelBuilder.TypeArgument

object SourceGeneratorUtils {
  val managedComment = """/* This code is managed by Akka Serverless tooling.
                         | * It will be re-generated to reflect any changes to your protobuf definitions.
                         | * DO NOT EDIT
                         | */""".stripMargin

  val unmanagedComment = """|/* This code was generated by Akka Serverless tooling.
                            | * As long as this file exists it will not be re-generated.
                            | * You are free to make changes to this file.
                            | */""".stripMargin

  def mainPackageName(classNames: Iterable[String]): List[String] = {
    val packages = classNames
      .map(
        _.replaceFirst("\\.[^.]*$", "")
          .split("\\.")
          .toList)
      .toSet
    if (packages.isEmpty) throw new IllegalArgumentException("Nothing to generate!")
    longestCommonPrefix(packages.head, packages.tail)
  }

  @tailrec
  def longestCommonPrefix(
      reference: List[String],
      others: Set[List[String]],
      resultSoFar: List[String] = Nil): List[String] = {
    reference match {
      case Nil =>
        resultSoFar
      case head :: tail =>
        if (others.forall(p => p.headOption.contains(head)))
          longestCommonPrefix(tail, others.map(_.tail), resultSoFar :+ head)
        else
          resultSoFar
    }

  }

  def disassembleClassName(fullClassName: String): (String, String) = {
    val className = fullClassName.reverse.takeWhile(_ != '.').reverse
    val packageName = fullClassName.dropRight(className.length + 1)
    packageName -> className
  }

  def qualifiedType(fullyQualifiedName: FullyQualifiedName): String =
    if (fullyQualifiedName.parent.javaMultipleFiles) fullyQualifiedName.name
    else s"${fullyQualifiedName.parent.javaOuterClassname}.${fullyQualifiedName.name}"

  def typeImport(fullyQualifiedName: FullyQualifiedName): String = {
    val name =
      if (fullyQualifiedName.parent.javaMultipleFiles) fullyQualifiedName.name
      else fullyQualifiedName.parent.javaOuterClassname
    s"${fullyQualifiedName.parent.javaPackage}.$name"
  }

  def lowerFirst(text: String): String =
    text.headOption match {
      case Some(c) => c.toLower.toString + text.drop(1)
      case None    => ""
    }

  def packageAsPath(packageName: String): Path =
    Paths.get(packageName.replace(".", "/"))

  def generateImports(
      types: Iterable[FullyQualifiedName],
      packageName: String,
      otherImports: Seq[String],
      semi: Boolean = true): String = {
    val messageTypeImports = types
      .filterNot { typ =>
        typ.parent.javaPackage == packageName
      }
      .map(typeImport)

    val suffix = if (semi) ";" else ""
    (messageTypeImports ++ otherImports).toSeq.distinct.sorted
      .map(pkg => s"import $pkg$suffix")
      .mkString("\n")
  }

  def generateCommandImports(
      commands: Iterable[Command],
      state: State,
      packageName: String,
      otherImports: Seq[String],
      semi: Boolean = true): String = {
    val types = commandTypes(commands) :+ state.fqn
    generateImports(types, packageName, otherImports, semi)
  }

  def generateCommandAndTypeArgumentImports(
      commands: Iterable[Command],
      typeArguments: Iterable[TypeArgument],
      packageName: String,
      otherImports: Seq[String],
      semi: Boolean = true): String = {
    val types = commandTypes(commands) ++ typeArguments.collect { case MessageTypeArgument(fqn) =>
      fqn
    }
    generateImports(types, packageName, otherImports ++ extraTypeImports(typeArguments), semi)
  }

  def extraTypeImports(typeArguments: Iterable[TypeArgument]): Seq[String] =
    typeArguments.collect { case ScalarTypeArgument(ScalarType.Bytes) =>
      "com.google.protobuf.ByteString"
    }.toSeq

  def commandTypes(commands: Iterable[Command]): Seq[FullyQualifiedName] =
    commands.flatMap(command => Seq(command.inputType, command.outputType)).toSeq
}
