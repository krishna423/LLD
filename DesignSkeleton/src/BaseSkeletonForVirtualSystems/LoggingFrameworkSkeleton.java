package BaseSkeletonForVirtualSystems;

import java.time.Instant;

public class LoggingFrameworkSkeleton {


    // Flow --> client ---> log.debug(message)---->[ LoggingFramework (Use Formatter) ] --> Append Log in store


    // LogEvent - client object to log
    // LogFormatter - formatting - PlainString, JSON
    // LogAppender - Console, File
    // LogLevel - Debug,info
    // Logger - loggingFramework
    // DriverClass

   static class LogEvent{
       String message;
       Instant timestamp;
       LogLevel logLevel;
       String loggerName;
       String threadName;
       Throwable exception;

   }

   enum LogLevel{
       Trace(0), Debug(1) ,Info(2), error(3), Warn(4);

       private int priority;
       LogLevel(int priority) {
           this.priority = priority;
       }

       public boolean isEnabled(LogLevel configuredLogLevel ) {
           return this.priority <= configuredLogLevel.priority;
       }
   }

   interface ILogFormatterStrategy{
       String format(LogEvent logEvent);
   }

   interface ILogAppender{
       void append(LogEvent logEvent);
       //Formatting strategy
   }

   interface ILogger{
       void debug(String message);
       void trace(String message);
       void info(String message);
       void error(String message, Throwable exception);
       //ILogAppender
   }


    static class StringLogFormatterStrategy implements ILogFormatterStrategy{

        @Override
        public String format(LogEvent logEvent) {
            return logEvent.message + "|" + logEvent.logLevel + "|" + logEvent.timestamp;
        }
    }


   static class ConsoleLogAppender implements ILogAppender{
       private ILogFormatterStrategy logFormatterStrategy;

       @Override
       public void append(LogEvent logEvent) {
           String logEventString = logFormatterStrategy.format(logEvent);
           System.out.println(logEventString);
       }
   }




   static class Logger  implements ILogger{

       private ILogFormatterStrategy logFormatterStrategy;
       private ILogAppender logAppender;
       private LogLevel minimumConfiguredLogLevel;

       @Override
       public void debug(String message) {
        log(LogLevel.Debug,message,null);
       }

       @Override
       public void trace(String message) {

       }

       @Override
       public void info(String message) {

       }

       @Override
       public void error(String message, Throwable exception) {

       }


       private void log(LogLevel requestLogLevel, String message, Throwable exception){
           //first check priority --- Enabled or not
           //append
           if(!requestLogLevel.isEnabled(minimumConfiguredLogLevel)){
               return;
           }
           LogEvent logEvent = new LogEvent() ; // log.setMessage=message, level = level
           logAppender.append(logEvent);
       }


       class Driver{
           public static void main(String[] args) {
               ILogFormatterStrategy logFormatterStrategy;
               ILogAppender logAppender;
               LogLevel minimumConfiguredLogLevel;

               Logger logger = new Logger();
               logger.debug("12312312312");
               logger.info("info");
           }
       }


   }

}
