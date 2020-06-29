function log {
    echo "[$(date "+%Y-%m-%d %H:%M:%S")]" $*
}

function wait_for {
    url=$1

    until curl $url >> /dev/null 2>&1; do
        log "waiting for $url"
        sleep 5
    done
}

case $1 in
    producer)
        wait_for 'http://kafdrop:9000'
        log "starting producer..."
        java -jar producer.jar
        log "done"
        ;;
    consumer)
        wait_for 'http://jobmanager:8081'
        wait_for 'http://kafdrop:9000'

        log "submitting jar to flink... "
        filename=$(curl -s -X POST \
            -H "Expect:" -F "jarfile=@consumer.jar" \
            jobmanager:8081/jars/upload | jq -r '.filename')
        jarid=$(basename "$filename")

        log "starting ${jarid}..."
        curl -s -X POST "jobmanager:8081/jars/${jarid}/run"

        log "done"
        ;;
esac
