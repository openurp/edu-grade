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
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.GaGrade
import org.openurp.edu.grade.course.service.GradeRateService
import org.openurp.edu.grade.course.service.ScoreConverter
import org.openurp.edu.grade.course.domain.GradeFilter

/**
 * 想把补考成绩和期末总评平行打印出来的过滤器
 */
class MakeupGradeFilter extends GradeFilter {

  var gradeRateService: GradeRateService = _

  override def filter(grades: collection.Seq[CourseGrade]): collection.Seq[CourseGrade] = {
    val gradeList = Collections.newBuffer[CourseGrade]
    for (courseGrade <- grades) {
      var finded = false
      for (gaGrade <- courseGrade.gaGrades) {
        val newGrade = new CourseGrade
        newGrade.std = courseGrade.std
        newGrade.semester = courseGrade.semester
        newGrade.clazz = courseGrade.clazz
        newGrade.course = courseGrade.course
        newGrade.courseType = courseGrade.courseType
        newGrade.crn = courseGrade.crn
        newGrade.courseType = courseGrade.courseType
        newGrade.courseTakeType = courseGrade.courseTakeType
        newGrade.freeListening = courseGrade.freeListening
        newGrade.score = gaGrade.score
        newGrade.passed = gaGrade.passed
        newGrade.gradingMode = gaGrade.gradingMode
        val converter = gradeRateService.getConverter(courseGrade.project, gaGrade.gradingMode)
        newGrade.gp = converter.calcGp(newGrade.score)
        finded = true
        gradeList += newGrade
      }
      if (!finded) {
        gradeList += courseGrade
      }
    }
    gradeList
  }

}
