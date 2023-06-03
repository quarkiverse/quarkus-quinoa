import { QwcServerLog} from 'qwc-server-log';

/**
 * This component filter the log to only show Quinoa related entries.
 */
export class QwcQuinoaLog extends QwcServerLog {

    doLogEntry(entry){
        if (entry.loggerName && entry.loggerName.includes("quinoa")) {
            return true;
        }
        return false;
    }
}

customElements.define('qwc-quinoa-log', QwcQuinoaLog);
