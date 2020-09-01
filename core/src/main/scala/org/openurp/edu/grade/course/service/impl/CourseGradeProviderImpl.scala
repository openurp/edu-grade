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
import org.beangle.data.dao.OqlBuilder
import org.beangle.commons.lang.time.Stopwatch
import org.openurp.edu.base.model.Semester
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.BaseServiceImpl
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.domain.CourseGradeProvider
import org.openurp.edu.grade.model.Grade

import scala.collection.mutable.Buffer

class CourseGradeProviderImpl extends BaseServiceImpl with CourseGradeProvider {

  def getPassedStatus(std: Student): collection.Map[Long, Boolean] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "cg")
    query.where("cg.std = :std", std)
    query.select("cg.course.id,cg.passed")
    val rs = entityDao.search(query).asInstanceOf[List[Array[Any]]]
    val courseMap = Collections.newMap[Long, Boolean]
    for (obj <- rs) {
      val courseId = obj(0).asInstanceOf[Number].longValue
      if (null != obj(1)) {
        if (!courseMap.contains(courseId) || !courseMap(courseId)) {
          courseMap.put(courseId, obj(1).asInstanceOf[java.lang.Boolean].booleanValue)
        }
      } else {
        courseMap.put(courseId, false)
      }
    }
    courseMap
  }

  def getPublished(std: Student, semesters: Semester*): Seq[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std = :std", std)
    query.where("grade.status =:status", Grade.Status.Published)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    query.orderBy("grade.semester.beginOn")
    entityDao.search(query)
  }

  def getAll(std: Student, semesters: Semester*): Seq[CourseGrade] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std = :std", std)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    query.orderBy("grade.semester.beginOn")
    entityDao.search(query)
  }

  def getPublished(stds: Iterable[Student], semesters: Semester*): collection.Map[Student, collection.Seq[CourseGrade]] = {
    val sw = new Stopwatch()
    sw.start()
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std in (:stds)", stds)
    query.where("grade.status =:status", Grade.Status.Published)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    val allGrades = entityDao.search(query)
    val gradeMap = Collections.newMap[Student, Buffer[CourseGrade]]
    for (std <- stds) {
      gradeMap.put(std, Collections.newBuffer[CourseGrade])
    }
    for (g <- allGrades) gradeMap(g.std) += g
    gradeMap
  }

  def getAll(stds: Iterable[Student], semesters: Semester*): collection.Map[Student, collection.Seq[CourseGrade]] = {
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std in (:stds)", stds)
    if (null != semesters && semesters.length > 0) {
      query.where("grade.semester in(:semesters)", semesters)
    }
    val allGrades = entityDao.search(query)
    val gradeMap = Collections.newMap[Student, Buffer[CourseGrade]]
    for (std <- stds) {
      gradeMap.put(std, Collections.newBuffer[CourseGrade])
    }
    for (g <- allGrades) gradeMap(g.std) += g
    gradeMap
  }
}
