/*
 * OpenURP, Agile University Resource Planning Solution.
 *
 * Copyright © 2005, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openurp.edu.grade.course.service

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.Operation
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.CourseGradeState

/**
 * 成绩发布监听器堆栈
 *
 */
class CourseGradePublishStack {

  protected var listeners = Collections.newBuffer[CourseGradePublishListener]

  def onPublish(grade: CourseGrade, gradeTypes: Array[GradeType]): Seq[Operation] = {
    val results = Collections.newBuffer[Operation]
    for (listener <- listeners) {
      results ++= listener.onPublish(grade, gradeTypes)
    }
    results
  }

  def onPublish(grades: Iterable[CourseGrade], gradeState: CourseGradeState, gradeTypes: Array[GradeType]): Seq[Operation] = {
    val results = Collections.newBuffer[Operation]
    for (listener <- listeners) {
      results ++= listener.onPublish(grades, gradeState, gradeTypes)
    }
    results
  }

}
