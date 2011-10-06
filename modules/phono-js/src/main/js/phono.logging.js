;var PhonoLogger = function() {
    var logger = this;
    logger.eventQueue = [];
    logger.initialized = false;
    $(document).ready(function() {
        console.log("Phono Logger Initialized")
        logger.initialized = true;
        logger.flushEventQueue();
    });    
};

(function() {

    var newLine = "\r\n";

    // Logging events
    // ====================================================================================

    var PhonoLogEvent = function(timeStamp, level, messages, exception) {
        this.timeStamp = timeStamp;
        this.level = level;
        this.messages = messages;
        this.exception = exception;
    };

    PhonoLogEvent.prototype = {
        getThrowableStrRep: function() {
            return this.exception ? getExceptionStringRep(this.exception) : "";
        },
        getCombinedMessages: function() {
            return (this.messages.length === 1) ? this.messages[0] : this.messages.join(newLine);
        }
    };

    // Log Levels
    // ====================================================================================

    var PhonoLogLevel = function(level, name) {
        this.level = level;
        this.name = name;
    };

    PhonoLogLevel.prototype = {
        toString: function() {
            return this.name;
        },
        equals: function(level) {
            return this.level == level.level;
        },
        isGreaterOrEqual: function(level) {
            return this.level >= level.level;
        }
    };

    PhonoLogLevel.ALL = new PhonoLogLevel(Number.MIN_VALUE, "ALL");
    PhonoLogLevel.TRACE = new PhonoLogLevel(10000, "TRACE");
    PhonoLogLevel.DEBUG = new PhonoLogLevel(20000, "DEBUG");
    PhonoLogLevel.INFO = new PhonoLogLevel(30000, "INFO");
    PhonoLogLevel.WARN = new PhonoLogLevel(40000, "WARN");
    PhonoLogLevel.ERROR = new PhonoLogLevel(50000, "ERROR");
    PhonoLogLevel.FATAL = new PhonoLogLevel(60000, "FATAL");
    PhonoLogLevel.OFF = new PhonoLogLevel(Number.MAX_VALUE, "OFF");

    // Logger
    // ====================================================================================

    PhonoLogger.prototype.log = function(level, params) {

        var exception;
        var finalParamIndex = params.length - 1;
        var lastParam = params[params.length - 1];
        if (params.length > 1 && isError(lastParam)) {
            exception = lastParam;
            finalParamIndex--;
        }

        var messages = [];
        for (var i = 0; i <= finalParamIndex; i++) {
            messages[i] = params[i];
        }

        var loggingEvent = new PhonoLogEvent(new Date(), level , messages, exception);
        this.eventQueue.push(loggingEvent);

        this.flushEventQueue();
        
    };
    
    PhonoLogger.prototype.flushEventQueue = function() {
        if(this.initialized) {
            var logger = this;
            Phono.util.each(this.eventQueue, function(idx, event) {
                Phono.events.trigger(logger, "log", event);
            });
            this.eventQueue = [];
        }
    };

    PhonoLogger.prototype.debug = function() {
        this.log(PhonoLogLevel.DEBUG, arguments);
    };

    PhonoLogger.prototype.info = function() {
        this.log(PhonoLogLevel.INFO, arguments);
    };

    PhonoLogger.prototype.warn = function() {
        this.log(PhonoLogLevel.WARN, arguments);
    };

    PhonoLogger.prototype.error = function() {
        this.log(PhonoLogLevel.ERROR, arguments);
    };

    // Util
    // ====================================================================================

    function getExceptionMessage(ex) {
        if (ex.message) {
            return ex.message;
        } else if (ex.description) {
            return ex.description;
        } else {
            return toStr(ex);
        }
    };

    // Gets the portion of the URL after the last slash
    function getUrlFileName(url) {
        var lastSlashIndex = Math.max(url.lastIndexOf("/"), url.lastIndexOf("\\"));
        return url.substr(lastSlashIndex + 1);
    };

    // Returns a nicely formatted representation of an error
    function getExceptionStringRep(ex) {
        if (ex) {
            var exStr = "Exception: " + getExceptionMessage(ex);
            try {
                if (ex.lineNumber) {
                    exStr += " on line number " + ex.lineNumber;
                }
                if (ex.fileName) {
                    exStr += " in file " + getUrlFileName(ex.fileName);
                }
            } catch (localEx) {
            }
            if (showStackTraces && ex.stack) {
                exStr += newLine + "Stack trace:" + newLine + ex.stack;
            }
            return exStr;
        }
        return null;
    };

    function isError(err) {
        return (err instanceof Error);
    };

    function bool(obj) {
        return Boolean(obj);
    };
    
})();

