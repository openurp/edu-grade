package org.openurp.edu.grade.course.service

import org.apache.commons.collections.Predicate
import org.openurp.edu.base.model.Project
import org.openurp.edu.course.model.Clazz
//remove if not needed
import scala.collection.JavaConversions._

class ClazzProjectPredicate(private var project: Project) extends Predicate {

  def evaluate(`object`: AnyRef): Boolean = {
    `object`.asInstanceOf[Clazz].getProject == project
  }
}
