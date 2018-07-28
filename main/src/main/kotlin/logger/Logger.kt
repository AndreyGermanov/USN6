package logger

import org.slf4j.LoggerFactory

/**
 * Class, which used to implement logging features
 */
object Logger {
    private val logger = LoggerFactory.getLogger("WAB3_Logger")!!

    /**
     * Method used to log message to preconfigured logger instance
     *
     * @param level: Log level
     * @param message: Message to log
     */
    fun log(level:LogLevels,message:String,className:String?=null,methodName:String?=null) {
        var output = "$message("
        if (className != null) {
            output += className
        }
        if (methodName != null) {
            output += ",$methodName"
        }
        when (level) {
            LogLevels.DEBUG -> logger.debug(message)
            LogLevels.ERROR -> logger.error(message)
            LogLevels.WARNING -> logger.warn(message)
            LogLevels.INFO -> logger.info(message)
            LogLevels.TRACE -> logger.trace(message)
        }
    }
}

/**
 * Enumeration of Log levels
 */
enum class LogLevels {
    DEBUG,ERROR,WARNING,INFO,TRACE
}
