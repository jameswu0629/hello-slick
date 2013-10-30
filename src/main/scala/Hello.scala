import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import play.api.libs.json._

import voldemort.client._
import voldemort.versioning.Versioned
import com.codahale.jerkson.{ Json => Json }
import scala.util.parsing.json._

import java.sql.Date

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text._
import scala.util.matching.Regex

class Purge(vdmHost:String) 
{
    private val logger = LoggerFactory.getLogger(this.getClass)
    val formatter      = new SimpleDateFormat("yyyy")
    val pattern        = new Regex("(\\d+)")

    def run(l:List[(Int, String, java.sql.Date, Int)]) 
    {   
        var factory:Option[StoreClientFactory] = None
        try
        {
            // connect VDM
            val bootstrapUrl:String                  = "tcp://%s:6666".format(vdmHost)
            factory                                  = Some(new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls(bootstrapUrl)))
            val gaClient:StoreClient[String, String] = factory.get.getStoreClient("GlobalAccount_Store")
            val arClient:StoreClient[String, String] = factory.get.getStoreClient("Account_Store")


            l.foreach 
            {
                case (id, email, update_time, status) =>
                {
                    try
                    {
                        logger.info("EmailAddress = %s".format(email))
                        val key         = email.replace("\\u0001", "")
                        val profileData = arClient.get(key)
                        val globalData  = gaClient.get(key)
                    
                        if (profileData == null) 
                        {
                            logger.info("profile not found: %s".format(key))
                        } 
                        else if (globalData == null) 
                        {
                            logger.info("global not found: %s".format(key))              
                        } 
                        else
                        {
                            println ("Proccessing %s".format(key))
                            logger.info("Proccessing %s".format(profileData.getValue))
                            var profileJson = Json.parse[Map[String, Any]](profileData.getValue)
                            var globalJson  = Json.parse[Map[String, Any]](globalData.getValue)
            
                            // update Account_Store
                            profileJson.get("DateOfBirth") match 
                            {
                                case None =>
                                case _ =>                            
                                val dateOfBirth:String        = profileJson.get("DateOfBirth").get.toString
                                val bTimestemp:Option[String] = (pattern findFirstIn dateOfBirth)
                                val yearOfBirth:String        = formatter.format(bTimestemp.get.toLong)

                                if (profileJson.get("YearOfBirth") == None)
                                {
                                    profileJson += ("YearOfBirth" -> yearOfBirth)
                                }
                                logger.info("DateOfBirth = %s".format(dateOfBirth))
                                logger.info("bTimestemp = %s".format(bTimestemp.get))
                                logger.info("yearOfBirth = %s".format(yearOfBirth))
                            }
                            
                            // remove DateOfBirth
                            profileJson -= ("DateOfBirth")
                            logger.info("""put "%s" '%s'""".format(key, Json.generate(profileJson)))
                            arClient.put(key, Json.generate(profileJson))
            
                            // update GlobalAccount_Store
                            val accountId = profileJson.get("Id").get
                            globalJson += ("Id" -> accountId)
                            logger.info("""put "%s" '%s'""".format(key, Json.generate(globalJson)))
                            logger.info("""put "$Id$_%s" '%s'""".format(accountId, key))
                            //gaClient.put(key, Json.generate(globalJson))
                            //gaClient.put("$Id$_%s".format(accountId), key)
                        
                            val q = for { c <- PurgeBirthQueue10 if c.email === email } yield (c.status)
                            q.update(1)
                        }
                    }
                    catch
                    {
                        case ex:Exception =>
                        logger.info(ex.toString)
                    }
                }                            
            } // l.foreach                           
        }
        catch
        {
            case ex:Exception =>
            logger.info(ex.toString)
        }
        finally
        {
            factory.get.close
        }
        
    } // def run
}

object Purge10  extends App {  
    
    private val logger = LoggerFactory.getLogger(this.getClass)
    val host = "10.23.37.85"
    
    try
    {
        logger.info("Connection mysql...")
        Database.forURL("jdbc:mysql://10.116.221.94/identity?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci", user = "", password = "", driver = "com.mysql.jdbc.Driver") withSession 
        {
            val purge = new Purge(host)

            var keepRunning = true
            while(keepRunning) 
            {   
                val l = Query(PurgeBirthQueue10).filter(_.status === 0).take(500).list
                if (l.length == 0) keepRunning = false
                purge.run(l)
            } // while(keepRunning) 
        } // Database.forURL
    }
    catch
    {
        case ex:Exception =>
        logger.info(ex.toString)
    }

}
