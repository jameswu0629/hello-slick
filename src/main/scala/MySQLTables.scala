import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import play.api.libs.json._

import voldemort.client._
import voldemort.versioning.Versioned

import java.sql.Date

object PurgeBirthQueue10 extends Table[(Int, String, Date, Int)]("purge_birth_queue_10") {
  def id          = column[Int]("id", O.PrimaryKey) // This is the primary key column
  def email       = column[String]("email")
  def update_time = column[Date]("update_time")
  def status      = column[Int]("status")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = id ~ email ~ update_time ~ status
}