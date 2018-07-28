package Utils

import org.json.simple.JSONObject
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.*
import java.util.Properties

internal class EmailAuthenticator(private val login: String, private val password: String) : javax.mail.Authenticator() {
    public override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(login, password)
    }
}

object SendMail {
    private var message: Message? = null


    fun init(config:HashMap<String,String>) {
        val properties = Properties()
        properties["mail.smtp.host"] = config["host"].toString()
        properties["mail.smtp.port"] = config["port"].toString()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.ssl.enable"] = "false"
        properties["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        try {
            val auth = EmailAuthenticator(config["login"].toString(),config["password"].toString())
            val session = Session.getDefaultInstance(properties, auth)
            session.debug = false

            val email_from = InternetAddress(config["from"].toString())
            val email_to = InternetAddress(config["address"].toString())
            val reply_to = if (config["replyTo"] != null)
                InternetAddress(config["replyTo"].toString())
            else
                null
            message = MimeMessage(session)
            message!!.setFrom(email_from)
            message!!.setRecipient(Message.RecipientType.TO, email_to)
            message!!.subject = config["subject"].toString()
            if (reply_to != null)
                message!!.replyTo = arrayOf<Address>(reply_to)
        } catch (e: AddressException) {
            System.err.println(e.message)
        } catch (e: MessagingException) {
            System.err.println(e.message)
        }

    }

    @Throws(MessagingException::class)
    private fun createFileAttachment(filepath: String): MimeBodyPart {
        val mbp = MimeBodyPart()

        val fds = FileDataSource(filepath)
        mbp.dataHandler = DataHandler(fds)
        mbp.fileName = fds.name
        return mbp
    }

    @Throws(Exception::class)
    fun sendMessage(text: String,attachments:ArrayList<String>?=null): Boolean {
        var result = false
        try {
            val mmp = MimeMultipart()
            val bodyPart = MimeBodyPart()
            bodyPart.setContent(text, "text/plain; charset=utf-8")
            mmp.addBodyPart(bodyPart)
            if (attachments != null) {
                for (filename in attachments) {
                    val mbr = createFileAttachment(filename)
                    mmp.addBodyPart(mbr)
                }
            }
            message!!.setContent(mmp)
            Transport.send(message!!)
            result = true
        } catch (e: Exception) {
            System.err.println(e.message)
        }

        return result
    }

}


