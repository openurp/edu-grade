package org.openurp.edu.grade.course.service.impl

import org.beangle.commons.collection.Collections
import org.beangle.data.transfer.exporter.DefaultPropertyExtractor
import org.openurp.edu.base.model.Teacher
import org.openurp.edu.grade.course.model.CourseGradeState
import org.openurp.edu.course.model.Clazz

class GradeStatExtractor  extends DefaultPropertyExtractor {

  override def getPropertyValue(target: Object, property: String): Any = {
    if ("teachers" == property) {
      var teacherName = ""
      var teachers = Collections.newBuffer[Teacher]
      if (target.isInstanceOf[Clazz]) {
        val clazz = target.asInstanceOf[Clazz]
        teachers = clazz.teachers
      } else {
        val gradeState = target.asInstanceOf[CourseGradeState]
        teachers = gradeState.clazz.teachers
      }
      if (teachers.size == 0) {
        return "未安排教师"
      }
      for (i <- 0 until teachers.size) {
        if (i > 0) {
          teacherName += ","
        }
        teacherName += teachers(i).user.name
      }
      teacherName
    } else {
      super.getPropertyValue(target, property)
    }
  }
}
