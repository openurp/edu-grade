/*
 * OpenURP, Agile University Resource Planning Solution.
 *
 * Copyright © 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openurp.edu.grade.course.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.impl.BaseServiceImpl
import org.beangle.data.dao.OqlBuilder
import org.beangle.commons.lang.Strings
import org.openurp.code.edu.model.ExamStatus
import org.openurp.code.edu.model.ExamType
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.CourseGradeState
import org.openurp.edu.grade.course.model.ExamGrade
import org.openurp.edu.grade.course.service.CourseGradePublishListener
import org.openurp.edu.grade.course.service.CourseGradeSettings
import org.openurp.edu.exam.model.ExamTaker
import org.openurp.edu.course.model.Clazz
import ExamTakerGeneratePublishListener._
import org.openurp.edu.grade.course.service.CourseGradeSetting
import org.beangle.data.dao.Operation
import org.beangle.data.orm.Collection

object ExamTakerGeneratePublishListener {

  private val Makeup = new ExamType(ExamType.Makeup)

  private val Delay = new ExamType(ExamType.Delay)
}

class ExamTakerGeneratePublishListener extends BaseServiceImpl with CourseGradePublishListener {

  private var settings: CourseGradeSettings = _

  private var forbiddenCourseNames: Array[String] = new Array[String](0)

  private var forbiddenCourseTypeNames: Array[String] = new Array[String](0)

  private var forbiddenCourseTakeTypeNames: Array[String] = new Array[String](0)

  def onPublish(grades: Iterable[CourseGrade], gradeState: CourseGradeState, gradeTypes: Array[GradeType]): Seq[Operation] = {
    val operations = Collections.newBuffer[Operation]
    val hasEndGa = gradeTypes.exists(_.id.equals(GradeType.EndGa));
    if (!hasEndGa) return operations
    if (isClazzForbidden(gradeState.clazz)) return operations
    val setting = settings.getSetting(gradeState.clazz.project)
    val examTakers = getExamTakers(gradeState.clazz)
    for (grade <- grades) operations ++= publishOneGrade(grade, setting, gradeTypes, examTakers)
    operations
  }

  def onPublish(grade: CourseGrade, gradeTypes: Array[GradeType]): Seq[Operation] = {
    val operations = Collections.newBuffer[Operation]
    val hasGa = gradeTypes.exists(_.id.equals(GradeType.EndGa));
    if (!hasGa) return operations
    val clazz = grade.clazz.get
    if (isClazzForbidden(clazz)) return operations
    val setting = settings.getSetting(clazz.project)
    val examTakers = getExamTakers(clazz, grade.std)
    operations ++= publishOneGrade(grade, setting, gradeTypes, examTakers)
    operations
  }

  protected def isClazzForbidden(clazz: Clazz): Boolean = {
    if (null != clazz) {
      for (courseName <- forbiddenCourseNames if clazz.course.name.contains(courseName)) return true
      for (courseTypeName <- forbiddenCourseTypeNames if clazz.courseType.name.contains(courseTypeName)) return true
    }
    false
  }

  protected def isCourseTakeTypeForbidden(grade: CourseGrade): Boolean = {
    forbiddenCourseTakeTypeNames.find(grade.courseTakeType.name.contains(_))
      .map(_ => true)
      .getOrElse(false)
  }

  protected def getMakeupOrDelayExamTypeId(setting: CourseGradeSetting, examGrade: ExamGrade): Int = {
    if (isCourseTakeTypeForbidden(examGrade.courseGrade)) return 0
    val examStatus = examGrade.examStatus
    if (examStatus.deferred) {
      ExamType.Delay
    } else {
      if (setting.allowExamStatuses.contains(examStatus)) {
        ExamType.Makeup
      } else {
        0
      }
    }
  }

  private def getExamTakers(clazz: Clazz): Map[Student, ExamTaker] = {
    val builder = OqlBuilder.from(classOf[ExamTaker], "examTaker")
    builder.where("examTaker.clazz=:clazz and examTaker.examType in (:examTypes)", clazz, Array(Makeup, Delay))
    val examTakers = entityDao.search(builder)
    examTakers.map(t => (t.std, t)).toMap
  }

  private def getExamTakers(clazz: Clazz, std: Student): Map[Student, ExamTaker] = {
    val builder = OqlBuilder.from(classOf[ExamTaker], "examTaker")
    builder.where(
      "examTaker.std=:std and examTaker.clazz=:clazz and examTaker.examType in (:examTypes) and examTaker.activity is null",
      std, clazz, Array(Makeup, Delay))
    val examTakers = entityDao.search(builder)
    examTakers.map(t => (t.std, t)).toMap
  }

  def publishOneGrade(
    grade:      CourseGrade,
    setting:    CourseGradeSetting,
    gradeTypes: Array[GradeType],
    examTakers: collection.Map[Student, ExamTaker]): Seq[Operation] = {
    val operations = Collections.newBuffer[Operation]
    val examGrade = grade.getExamGrade(GradeType.End).orNull
    if (null == examGrade) return operations
    val clazz = grade.clazz.get
    val std = grade.std
    var taker: ExamTaker = null
    if (!grade.passed) {
      val examTypeId = getMakeupOrDelayExamTypeId(setting, examGrade)
      if (examTypeId > 0) taker = getOrCreateExamTaker(std, clazz, new ExamType(examTypeId), examTakers)
      if (null == taker) {
        addRemoveExamTakers(operations, std, examTakers, Makeup, Delay)
      } else {
        operations ++= Operation.saveOrUpdate(taker).build()
        if (taker.examType == Makeup) addRemoveExamTakers(operations, std, examTakers, Delay)
        if (taker.examType == Delay) addRemoveExamTakers(operations, std, examTakers, Makeup)
      }
    } else {
      if (null != grade.getExamGrade(GradeType.Delay))
        addRemoveExamTakers(
          operations,
          std, examTakers, Makeup)
      if (null !=
        grade.getGrade(new GradeType(GradeType.MakeupGa))) addRemoveExamTakers(
        operations,
        std, examTakers, Delay)
    }
    operations
  }

  private def addRemoveExamTakers(
    operations: collection.mutable.Buffer[Operation],
    std:        Student,
    examTakers: collection.Map[Student, ExamTaker],
    examTypes:  ExamType*) {
    examTakers.get(std) foreach { taker =>
      for (examType <- examTypes if taker.examType == examType) operations ++= Operation.remove(taker).build()
    }
  }

  private def getOrCreateExamTaker(
    std:        Student,
    clazz:      Clazz,
    examType:   ExamType,
    examTakers: collection.Map[Student, ExamTaker]): ExamTaker = {
    examTakers.get(std) match {
      case None =>
        val taker = new ExamTaker()
        taker.std = std
        taker.clazz = clazz
        taker.semester = clazz.semester
        taker.examType = examType
        taker.examStatus = new ExamStatus(ExamStatus.Normal)
        taker
      case Some(taker) => taker
    }
  }

  def setForbiddenCourseNames(names: String) {
    forbiddenCourseNames = Strings.split(names, ",")
    if (null == forbiddenCourseNames) forbiddenCourseNames = Array.ofDim[String](0)
  }

  def setForbiddenCourseTypeNames(names: String) {
    forbiddenCourseTypeNames = Strings.split(names, ",")
    if (null == forbiddenCourseTypeNames) forbiddenCourseTypeNames = Array.ofDim[String](0)
  }

  def setForbiddenCourseTakeTypeNames(names: String) {
    forbiddenCourseTakeTypeNames = Strings.split(names, ",")
    if (null == forbiddenCourseTakeTypeNames) forbiddenCourseTakeTypeNames = Array.ofDim[String](0)
  }
}
