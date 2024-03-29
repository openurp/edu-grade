package org.openurp.edu.grade.course.service.impl

class GradeStatExtractor(textResource: TextResource) extends DefaultPropertyExtractor(textResource) {

  def getPropertyValue(target: AnyRef, property: String): AnyRef = {
    if ("teachers" == property) {
      var teacherName = ""
      var teachers = Collections.newArrayList()
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
        teacherName += teachers.get(i).name
      }
      teacherName
    } else {
      super.getPropertyValue(target, property)
    }
  }
}
