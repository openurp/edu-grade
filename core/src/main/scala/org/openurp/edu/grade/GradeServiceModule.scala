package org.openurp.edu.grade

import org.beangle.commons.inject.bind.AbstractBindModule
import org.openurp.edu.grade.app.service.impl.GradeInputSwithServiceImpl
import org.openurp.edu.grade.app.service.impl.ReportTemplateServiceImpl
import org.openurp.edu.grade.audit.service.internal.PlanAuditServiceImpl
import org.openurp.edu.grade.audit.service.listeners.PlanAuditCommonElectiveListener
import org.openurp.edu.grade.audit.service.listeners.PlanAuditCourseSubstitutionListener
import org.openurp.edu.grade.audit.service.listeners.PlanAuditCourseTakerListener
import org.openurp.edu.grade.audit.service.listeners.PlanAuditCourseTypeMatchListener
import org.openurp.edu.grade.audit.service.listeners.PlanAuditSkipListener
import org.openurp.edu.grade.audit.service.observers.PlanAuditGpaObserver
import org.openurp.edu.grade.audit.service.observers.PlanAuditPersistObserver
import org.openurp.edu.grade.course.service.CourseGradePublishStack
import org.openurp.edu.grade.course.service.GradingModeHelper
import org.openurp.edu.grade.course.service.impl.BestGpaStatService
import org.openurp.edu.grade.course.service.impl.BestGradeFilter
import org.openurp.edu.grade.course.service.impl.BestOriginGradeFilter
import org.openurp.edu.grade.course.service.impl.DefaultCourseGradeCalculator
import org.openurp.edu.grade.course.service.impl.DefaultGpaPolicy
import org.openurp.edu.grade.course.service.impl.DefaultGpaService
import org.openurp.edu.grade.course.service.impl.DefaultGradeTypePolicy
import org.openurp.edu.grade.course.service.impl.DefaultGradingModeStrategy
import org.openurp.edu.grade.course.service.impl.ExamTakerGeneratePublishListener
import org.openurp.edu.grade.course.service.impl.MakeupByExamStrategy
import org.openurp.edu.grade.course.service.impl.MakeupGradeFilter
import org.openurp.edu.grade.course.service.impl.More01ReserveMethod
import org.openurp.edu.grade.course.service.impl.MoreHalfReserveMethod
import org.openurp.edu.grade.course.service.impl.PassedGradeFilter
import org.openurp.edu.grade.course.service.impl.RecalcGpPublishListener
import org.openurp.edu.grade.course.service.impl.ScriptGradeFilter
import org.openurp.edu.grade.course.service.impl.SpringGradeFilterRegistry
import org.openurp.edu.grade.course.service.impl.StdGradeServiceImpl
import org.openurp.edu.grade.course.service.internal.BestGradeCourseGradeProviderImpl
import org.openurp.edu.grade.course.service.internal.CourseGradeProviderImpl
import org.openurp.edu.grade.course.service.internal.CourseGradeServiceImpl
import org.openurp.edu.grade.course.service.internal.GradeCourseTypeProviderImpl
import org.openurp.edu.grade.course.service.internal.GradeRateServiceImpl
import org.openurp.edu.grade.setting.service.impl.CourseGradeSettingsImpl
import org.openurp.edu.grade.transcript.service.impl.SpringTranscriptDataProviderRegistry
import org.openurp.edu.grade.transcript.service.impl.TranscriptGpaProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptPlanCourseProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptPublishedExternExamGradeProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptPublishedGradeProvider
import org.openurp.edu.grade.transcript.service.impl.TranscriptStdGraduationProvider
//remove if not needed
import scala.collection.JavaConversions._

class GradeServiceModule extends AbstractBindModule {

  protected override def doBinding() {
    bind("bestGradeCourseGradeProvider", classOf[BestGradeCourseGradeProviderImpl])
    bind("planAuditSkipListener", classOf[PlanAuditSkipListener])
    bind("planAuditCourseSubstitutionListener", classOf[PlanAuditCourseSubstitutionListener])
    bind("planAuditCourseTakerListener", classOf[PlanAuditCourseTakerListener])
    bind("planAuditCourseTypeMatchListener", classOf[PlanAuditCourseTypeMatchListener])
    bind("planAuditCommonElectiveListener", classOf[PlanAuditCommonElectiveListener])
    bind("planAuditGpaObserver", classOf[PlanAuditGpaObserver])
    bind("planAuditPersistObserver", classOf[PlanAuditPersistObserver])
    bind(classOf[CourseGradeSettingsImpl])
    bind("gradeRateService", classOf[GradeRateServiceImpl])
    bind("bestGradeFilter", classOf[BestGradeFilter])
    bind("gpaPolicy", classOf[DefaultGpaPolicy])
    bind("bestOriginGradeFilter", classOf[BestOriginGradeFilter])
    bind("passedGradeFilter", classOf[PassedGradeFilter])
    bind("gradeFilterRegistry", classOf[SpringGradeFilterRegistry])
    bind("courseGradeService", classOf[CourseGradeServiceImpl])
    bind("gradeInputSwithService", classOf[GradeInputSwithServiceImpl])
    bind(classOf[ReportTemplateServiceImpl])
    bind("scriptGradeFilter", classOf[ScriptGradeFilter])
    bind("courseGradeProvider", classOf[CourseGradeProviderImpl])
    bind("courseGradeCalculator", classOf[DefaultCourseGradeCalculator])
    bind("gpaService", classOf[DefaultGpaService])
    bind("bestGpaStatService", classOf[BestGpaStatService])
    bind("gradeCourseTypeProvider", classOf[GradeCourseTypeProviderImpl])
    bind("planAuditService", classOf[PlanAuditServiceImpl])
      .property("listeners", list(ref("planAuditSkipListener"), ref("planAuditCourseSubstitutionListener"), 
      ref("planAuditCourseTakerListener"), ref("planAuditCourseTypeMatchListener"), ref("planAuditCommonElectiveListener")))
    bind("makeupStdStrategy", classOf[MakeupByExamStrategy])
    bind("gradingModeHelper", classOf[GradingModeHelper])
    bind("gradingModeStrategy", classOf[DefaultGradingModeStrategy])
    bind("stdGradeService", classOf[StdGradeServiceImpl])
    bind("makeupGradeFilter", classOf[MakeupGradeFilter])
    bind("recalcGpPublishListener", classOf[RecalcGpPublishListener])
    bind("examTakerGeneratePublishListener", classOf[ExamTakerGeneratePublishListener])
    bind("courseGradePublishStack", classOf[CourseGradePublishStack])
      .property("listeners", list(ref("recalcGpPublishListener"), ref("examTakerGeneratePublishListener")))
    bind(classOf[DefaultGradeTypePolicy])
    bind(classOf[MoreHalfReserveMethod], classOf[More01ReserveMethod])
      .shortName()
    bind(classOf[TranscriptPlanCourseProvider], classOf[TranscriptGpaProvider], classOf[TranscriptPublishedGradeProvider], 
      classOf[TranscriptStdGraduationProvider], classOf[SpringTranscriptDataProviderRegistry], classOf[TranscriptPublishedExternExamGradeProvider])
      .shortName()
  }
}
