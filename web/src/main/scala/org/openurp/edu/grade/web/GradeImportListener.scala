package org.openurp.edu.grade.course.service





import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import org.beangle.commons.collection.Collections
import org.beangle.data.dao.EntityDao
import org.beangle.data.dao.OqlBuilder
import org.beangle.commons.entity.Entity
import org.beangle.commons.entity.metadata.Model
import org.beangle.commons.lang.Strings
import org.beangle.commons.transfer.TransferResult
import org.beangle.commons.transfer.importer.listener.ItemImporterListener
import org.openurp.edu.base.model.Semester
import org.openurp.code.edu.model.CourseTakeType
import org.openurp.code.edu.model.ExamStatus
import org.openurp.code.edu.model.GradeType
import org.openurp.code.edu.model.GradingMode
import org.openurp.edu.base.model.Course
import org.openurp.edu.base.model.Project
import org.openurp.edu.base.model.Student
import org.openurp.edu.grade.Grade
import org.openurp.edu.grade.course.domain.GradeType
import org.openurp.edu.grade.course.model.CourseGrade
import org.openurp.edu.grade.course.model.ExamGrade
import org.openurp.edu.clazz.model.Clazz



/**
 * 成绩导入监听器,实现全部数据导入的完整性。<br>
 * 依照学生、学期和考试类型作为唯一标识
 */
class GradeImportListener(private var entityDao: EntityDao, private var project: Project, private var calculator: CourseGradeCalculator)
    extends ItemImporterListener {

  override def onFinish(tr: TransferResult) {
  }

  override def onItemStart(tr: TransferResult) {
  }

  override def onItemFinish(tr: TransferResult) {
    val courseGrade = populateCourseGrade(tr)
    if (!tr.hasErrors()) {
      try {
        courseGrade.updatedAt=Instant.now
        entityDao.saveOrUpdate(courseGrade)
      } catch {
        case e: ConstraintViolationException => for (constraintViolation <- e.getConstraintViolations) {
          tr.addFailure(constraintViolation.getPropertyPath + constraintViolation.getMessage, constraintViolation.getInvalidValue)
        }
      }
    }
  }

  private def getPropEntity[T <: Entity[_]](clazz: Class[T], 
      tr: TransferResult, 
      key: String, 
      notNull: Boolean): T = {
    val description = importer.getDescriptions.get(key)
    val value = importer.getCurData.get(key).asInstanceOf[String]
    if (Strings.isBlank(value)) {
      if (notNull) {
        tr.addFailure(description + "不能为空", value)
      } else {
        return null
      }
    }
    val nameList = entityDao.get(clazz, "name", value)
    if (nameList.size != 1) {
      val codeList = entityDao.get(clazz, "code", value)
      if (codeList.size == 1) {
        return codeList.head
      } else if ((nameList.size + codeList.size) == 0) {
        tr.addFailure(importer.getDescriptions.get(key) + "不存在", value)
      } else {
        tr.addFailure(importer.getDescriptions.get(key) + "存在多条记录", value)
      }
      return null
    }
    nameList.head
  }

  private def checkCourseGradeExists(project: Project, 
      std: Student, 
      course: Course, 
      semester: Semester, 
      tr: TransferResult): CourseGrade = {
    val builder = OqlBuilder.from(classOf[CourseGrade], "courseGrade")
    builder.where("courseGrade.std = :student", std)
    builder.where("courseGrade.course = :course", course)
    builder.where("courseGrade.semester = :semester", semester)
    builder.where("courseGrade.project = :project", project)
    var courseGrades: List[CourseGrade] = null
    val courseGrade = Model.newInstance(classOf[CourseGrade])
    try {
      courseGrades = entityDao.search(builder)
      if (courseGrades.size == 1) {
        return courseGrades.head
      } else if (courseGrades.size > 1) {
        tr.addFailure("存在多条记录(", std.name + "," + project.name + "," + course.name + 
          "," + 
          semester.getCode + 
          ")")
        return null
      }
      courseGrade.setStd(std)
      courseGrade.setProject(project)
      courseGrade.setSemester(semester)
      courseGrade.course=(course)
    } catch {
      case e: Exception => 
    }
    courseGrade
  }

    /**
   * 成绩验证(期中成绩,期末成绩等)
   *
   * @param courseGrade
   * @param tr
   * @return
   */
  private def setExamGrades(courseGrade: CourseGrade, tr: TransferResult) {
    var examGrades = courseGrade.examGrades
    val gradeTypes = entityDao.getAll(classOf[GradeType])
    if (Collections.isEmpty(examGrades)) {
      examGrades = Collections.newHashSet()
    }
    var examStatus = getPropEntity(classOf[ExamStatus], tr, "examStatus", false)
    if (examStatus == null) {
      examStatus = entityDao.get(classOf[ExamStatus], ExamStatus.NORMAL)
    }
    var gradingMode = getPropEntity(classOf[GradingMode], tr, "gradingMode", false)
    if (gradingMode == null) {
      gradingMode = entityDao.get(classOf[GradingMode], GradingMode.Percent)
    }
    courseGrade.setGradingMode(gradingMode)
    for (gradeType <- gradeTypes) {
      val value = importer.getCurData.get(gradeType.getCode).asInstanceOf[String]
      val examGrade = checkExamGradeExists(examGrades)
      checkGrade(value, examGrade, tr)
      examGrade.setGradeType(gradeType)
      examGrade.setExamStatus(examStatus)
      examGrade.setCourseGrade(courseGrade)
      examGrade.setGradingMode(gradingMode)
      if (gradeType.id == GradeType.EndGa) {
        examGrade.setStatus(Grade.Status.Published)
      }
    }
  }

  private def checkGrade(value: String, examGrade: ExamGrade, tr: TransferResult) {
    if (Strings.isNotEmpty(value)) {
      if (value.matches("^\\d*\\.?\\d*$") && java.lang.Float.parseFloat(value) <= 100) {
        examGrade.setScore(java.lang.Float.parseFloat(value))
      } else if (value.matches("^\\d*\\.?\\d*$") && java.lang.Float.parseFloat(value) > 100) {
        tr.addFailure("分数不能大于100", value)
      } else {
        examGrade.setScoreText(value)
      }
    }
  }

  private def checkExamGradeExists(examGrades: Set[ExamGrade]): ExamGrade = {
    var itor = examGrades.iterator()
    while (itor.hasNext) {
      return itor.next()
    }
    Model.newInstance(classOf[ExamGrade])
  }

  private def populateCourseGrade(tr: TransferResult): CourseGrade = {
    val std = getPropEntity(classOf[Student], tr, "student", true)
    val course = getPropEntity(classOf[Course], tr, "course", true)
    val semester = getPropEntity(classOf[Semester], tr, "semester", true)
    val courseGrade = checkCourseGradeExists(project, std, course, semester, tr)
    setExamGrades(courseGrade, tr)
    val clazz = getPropEntity(classOf[Clazz], tr, "clazz", false)
    if (null != clazz) {
      courseGrade.setClazz(clazz)
    }
    courseGrade.setCrn(importer.getCurData.get("clazz").asInstanceOf[String])
    val courseTakeType = getPropEntity(classOf[CourseTakeType], tr, "courseTakeType", false)
    if (null != courseTakeType) {
      courseGrade.setCourseTakeType(courseTakeType)
    }
    courseGrade.setStatus(Grade.Status.Published)
    courseGrade
  }

  def setEntityDao(entityDao: EntityDao) {
    this.entityDao = entityDao
  }

  def setCalculator(calculator: CourseGradeCalculator) {
    this.calculator = calculator
  }

  def setProject(project: Project) {
    this.project = project
  }
}
