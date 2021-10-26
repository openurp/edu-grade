/*
 * Copyright (C) 2005, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openurp.edu.grade.course.service.impl

import org.openurp.edu.grade.model.Grade.Status.New
import org.openurp.edu.grade.model.Grade.Status.Published
import org.beangle.commons.collection.Collections
import org.beangle.data.dao.OqlBuilder
import org.beangle.commons.lang.Objects
import org.beangle.commons.lang.Strings
import org.beangle.commons.lang.functor.Predicate
import org.openurp.code.edu.model.CourseTakeType
import org.openurp.code.edu.model.GradeType
import org.openurp.base.edu.model.Project
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.CourseGradeState
import org.openurp.edu.grade.course.model.ExamGrade
import org.openurp.edu.grade.course.model.ExamGradeState
import org.openurp.edu.grade.course.model.GaGrade
import org.openurp.edu.grade.course.model.GaGradeState
import org.openurp.edu.grade.course.service.CourseGradeCalculator
import org.openurp.edu.grade.course.service.CourseGradePublishStack
import org.openurp.edu.grade.course.service.CourseGradeService
import org.openurp.edu.grade.course.service.CourseGradeSettings
import org.openurp.edu.grade.course.service.GradeCourseTypeProvider
import org.openurp.edu.grade.course.service.ScoreConverter
import org.openurp.edu.clazz.model.Clazz
import org.openurp.edu.grade.model.Grade
import org.openurp.edu.grade.model.GradeState
import org.beangle.data.dao.Operation
import org.beangle.commons.bean.orderings.PropertyOrdering
import org.beangle.data.model.Entity
import org.openurp.edu.grade.BaseServiceImpl

class CourseGradeServiceImpl extends BaseServiceImpl with CourseGradeService {

  var calculator: CourseGradeCalculator = _

  var gradeCourseTypeProvider: GradeCourseTypeProvider = _

  var publishStack: CourseGradePublishStack = _

  var settings: CourseGradeSettings = _

  private def getGrades(clazz: Clazz): Seq[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "courseGrade")
    query.where("courseGrade.clazz = :clazz", clazz)
    entityDao.search(query)
  }

  def getState(clazz: Clazz): CourseGradeState = {
    val list = entityDao.findBy(classOf[CourseGradeState].getName, "clazz", List(clazz))
    if (list.isEmpty) {
      return null
    }
    list.head
  }

  def getPublishableGradeTypes(project: Project): Seq[GradeType] = {
    // 查找除去最终成绩之外的所有可发布成绩
    var gradeTypes = entityDao.getAll(classOf[GradeType])
    gradeTypes = gradeTypes.filter { input =>
      input.isGa || input.id == GradeType.Final
    }
    gradeTypes.sorted(new PropertyOrdering("code"))
  }

  /**
   * 发布学生成绩
   */
  def publish(clazzIdSeq: String, gradeTypes: Array[GradeType], published: Boolean): Unit = {
    val ids2 = Strings.splitToLong(clazzIdSeq)
    val ids: Array[Long] = Array.ofDim(ids2.length)
    for (i <- 0 until ids.length) ids(i) = ids2(i).longValue
    val clazzes = entityDao.find(classOf[Clazz], ids.toList)
    if (Collections.isNotEmpty(clazzes)) {
      for (clazz <- clazzes) {
        updateState(clazz, gradeTypes, if (published) Published else New)
      }
    }
  }

  /** 依据状态调整成绩 */
  def recalculate(gradeState: CourseGradeState): Unit = {
    if (null == gradeState) {
      return
    }
    val published = Collections.newBuffer[GradeType]
    for (egs <- gradeState.examStates if egs.status == Published) published += egs.gradeType
    for (egs <- gradeState.gaStates if egs.status == Published) published += egs.gradeType
    val grades = getGrades(gradeState.clazz)
    for (grade <- grades) {
      updateGradeState(grade, gradeState, grade.project)
      for (state <- gradeState.examStates) {
        val gradeType = state.gradeType
        updateGradeState(grade.getExamGrade(gradeType).get, state, grade.project)
      }
      calculator.calcAll(grade, gradeState)
    }
    entityDao.saveOrUpdate(grades)
    if (!published.isEmpty) {
      publish(gradeState.clazz.id.toString, published.toArray, true)
    }
  }

  def remove(clazz: Clazz, gradeType: GradeType): Unit = {
    val state = getState(clazz)
    val courseGrades = entityDao.findBy(classOf[CourseGrade], "clazz", List(clazz))
    val gradeSetting = settings.getSetting(clazz.project)
    val save = Collections.newBuffer[Entity[_]]
    val remove = Collections.newBuffer[Entity[_]]
    val gts = Collections.newSet[GradeType]
    gts += gradeType

    if (GradeType.EndGa == gradeType.id) {
      gts ++= gradeSetting.gaElementTypes
    } else if (GradeType.MakeupGa == gradeType.id) {
      gts += new GradeType(GradeType.Makeup)
    } else if (GradeType.DelayGa == gradeType.id) {
      gts += new GradeType(GradeType.Delay)
    }
    for (courseGrade <- courseGrades; if (courseGrade.courseTakeType.id != CourseTakeType.Exemption)) {
      if (GradeType.Final == gradeType.id) {
        if (New == courseGrade.status) remove += courseGrade
      } else {
        if (removeGrade(courseGrade, gts, state)) {
          remove += courseGrade
        } else {
          save += courseGrade
        }
      }
    }
    if (null != state) {
      if (GradeType.Final == gradeType.id) {
        state.status = New
        state.examStates.clear()
        state.gaStates.clear()
      } else {
        for (gt <- gts) {
          if (gt.isGa) {
            val ggs = state.getState(gt).asInstanceOf[GaGradeState]
            state.gaStates.remove(ggs)
          } else {
            val egs = state.getState(gt).asInstanceOf[ExamGradeState]
            state.examStates.remove(egs)
          }
        }
      }
    }
    if (state.examStates.isEmpty && state.gaStates.isEmpty) {
      remove += state
    } else {
      save += state
    }
    // FIXME 日志
    entityDao.execute(Operation.saveOrUpdate(save).remove(remove))
  }

  private def removeGrade(courseGrade: CourseGrade, gradeTypes: Iterable[GradeType], state: CourseGradeState): Boolean = {
    for (gradeType <- gradeTypes) {
      if (gradeType.isGa) {
        val ga = courseGrade.getGaGrade(gradeType).orNull
        if (null != ga && New == ga.status) courseGrade.gaGrades -= ga
      } else {
        val exam = courseGrade.getExamGrade(gradeType).orNull
        if (null != exam && New == exam.status) courseGrade.examGrades -= exam
      }
    }
    if (Collections.isNotEmpty(courseGrade.gaGrades) || Collections.isNotEmpty(courseGrade.examGrades)) {
      calculator.calcAll(courseGrade, state)
      false
    } else {
      true
    }
  }

  /**
   * 依据状态信息更新成绩的状态和记录方式
   *
   * @param grade
   * @param state
   */
  private def updateGradeState(grade: Grade, state: GradeState, project: Project): Unit = {
    if (null != grade && null != state) {
      if (Objects.!=(grade.gradingMode, state.gradingMode)) {
        grade.gradingMode = state.gradingMode
        val converter = calculator.gradeRateService.getConverter(project, state.gradingMode)
        grade.scoreText = converter.convert(grade.score)
      }
      grade.status = state.status
    }
  }

  private def updateState(clazz: Clazz, gradeTypes: Array[GradeType], status: Int): Unit = {
    val courseGradeStates = entityDao.findBy(classOf[CourseGradeState].getName, "clazz", List(clazz))
    var gradeState: CourseGradeState = null
    for (gradeType <- gradeTypes) {
      gradeState = if (courseGradeStates.isEmpty) new CourseGradeState else courseGradeStates.head
      if (gradeType.id == GradeType.Final) {
        gradeState.status = status
      } else {
        gradeState.updateStatus(gradeType, status)
      }
    }
    val grades = entityDao.findBy(classOf[CourseGrade], "clazz", List(clazz))
    val toBeSaved = Collections.newBuffer[Operation]
    val published = Collections.newSet[CourseGrade]
    for (grade <- grades; if (grade.courseTakeType.id != CourseTakeType.Exemption)) {
      for (gradeType <- gradeTypes) {
        var updated = false
        if (gradeType.id == GradeType.Final) {
          grade.status = status
          updated = true
        } else {
          grade.getGrade(gradeType) foreach { examGrade =>
            examGrade.status = status
            updated = true
          }
        }
        if (updated) published += grade
      }
    }
    if (status == Published) toBeSaved ++= publishStack.onPublish(published, gradeState, gradeTypes)
    toBeSaved ++= Operation.saveOrUpdate(clazz, gradeState).saveOrUpdate(published)
      .build()
    entityDao.execute(toBeSaved.toArray.toIndexedSeq: _*)
  }

}
