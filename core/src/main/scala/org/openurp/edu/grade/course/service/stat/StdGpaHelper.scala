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

package org.openurp.edu.grade.course.service.stat

import org.openurp.edu.grade.course.model.StdGpa
import org.openurp.edu.grade.course.service.GpaService

object StdGpaHelper {

  def statGpa(multiStdGrade: MultiStdGrade, gpaService: GpaService): Unit = {
    var stdGradeList = multiStdGrade.stdGrades
    if (null != stdGradeList) {
      stdGradeList = List.empty
    }
    for (stdGrade <- stdGradeList) {
      val stdGpa = new StdGpa(stdGrade.std)
      stdGpa.gpa = gpaService.getGpa(stdGrade.std, stdGrade.grades)
      stdGrade.stdGpa = stdGpa
    }
  }
}
